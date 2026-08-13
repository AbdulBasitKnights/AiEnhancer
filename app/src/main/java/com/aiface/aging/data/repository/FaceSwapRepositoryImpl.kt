package com.aiface.aging.data.repository

import android.util.Log
import com.aiface.aging.data.local.PreferenceManager
import com.aiface.aging.data.model.faceswap.FaceSwapCategoryDto
import com.aiface.aging.data.model.faceswap.FaceSwapGenerateData
import com.aiface.aging.data.model.faceswap.FaceSwapRegisterRequest
import com.aiface.aging.data.model.faceswap.FaceSwapStatusData
import com.aiface.aging.data.model.faceswap.FaceSwapTemplateDto
import com.aiface.aging.data.remote.FaceSwapApiService
import com.aiface.aging.di.IoDispatcher
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.FaceSwapCatalog
import com.aiface.aging.domain.repository.FaceSwapRepository
import com.aiface.aging.utils.DeviceIdManager
import com.aiface.aging.utils.FaceSwapSafeApiCall
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.CoroutineStart
import kotlinx.coroutines.Deferred
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.async
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.launch
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.util.concurrent.atomic.AtomicBoolean
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FaceSwapRepositoryImpl @Inject constructor(
    private val apiService: FaceSwapApiService,
    private val preferenceManager: PreferenceManager,
    private val deviceIdManager: DeviceIdManager,
    @IoDispatcher private val ioDispatcher: CoroutineDispatcher,
) : FaceSwapRepository, FaceSwapSafeApiCall() {

    companion object {
        private const val TAG = "FaceSwapPreload"
    }

    private val repositoryScope = CoroutineScope(SupervisorJob() + ioDispatcher)
    private val catalogMutex = Mutex()
    private val catalogLoadInFlight = AtomicBoolean(false)
    private val _cachedCatalog = MutableStateFlow<FaceSwapCatalog?>(null)

    @Volatile
    private var catalogPreloadDeferred: Deferred<Unit>? = null

    override fun peekCachedCatalog(): FaceSwapCatalog? = _cachedCatalog.value

    override fun isCatalogLoadInFlight(): Boolean =
        _cachedCatalog.value == null && (
            catalogLoadInFlight.get() ||
                catalogMutex.isLocked ||
                catalogPreloadDeferred?.isActive == true
            )

    override fun preloadCatalog() {
        if (_cachedCatalog.value != null) {
            Log.d(TAG, "Repository: preload skipped — cache already populated")
            return
        }
        if (catalogPreloadDeferred?.isActive == true) {
            Log.d(TAG, "Repository: preload skipped — catalog request already in flight")
            return
        }
        repositoryScope.launch {
            awaitCatalogPreload()
        }
    }

    private suspend fun awaitCatalogPreload() {
        if (_cachedCatalog.value != null) return

        catalogPreloadDeferred?.let { inFlight ->
            Log.d(TAG, "Repository: joining in-flight preload")
            inFlight.await()
            return
        }

        lateinit var deferred: Deferred<Unit>
        deferred = repositoryScope.async(start = CoroutineStart.LAZY) {
            try {
                Log.d(TAG, "Repository: API started from Splash preload")
                val result = fetchCatalogSingleFlight(forceRefresh = false)
                when (result) {
                    is Resource.Success ->
                        Log.d(
                            TAG,
                            "Repository: Splash preload cached " +
                                "(${result.data?.categories?.size} categories, " +
                                "${result.data?.templates?.size} templates)",
                        )
                    is Resource.Error ->
                        Log.d(TAG, "Repository: Splash preload failed — ${result.message}")
                    is Resource.Loading -> Unit
                }
            } finally {
                synchronized(this@FaceSwapRepositoryImpl) {
                    if (catalogPreloadDeferred === deferred) {
                        catalogPreloadDeferred = null
                    }
                }
            }
        }

        val toAwait = synchronized(this) {
            val winner = catalogPreloadDeferred
            if (winner != null) {
                deferred.cancel()
                winner
            } else {
                catalogPreloadDeferred = deferred
                deferred.start()
                deferred
            }
        }
        toAwait.await()
    }

    override fun getCatalog(forceRefresh: Boolean): Flow<Resource<FaceSwapCatalog>> = flow {
        if (!forceRefresh) {
            _cachedCatalog.value?.takeIf {
                it.categories.isNotEmpty() && it.templates.isNotEmpty()
            }?.let { cached ->
                Log.d(TAG, "Repository: cache hit — no API call")
                emit(Resource.Success(cached))
                return@flow
            }
        }

        emit(Resource.Loading())
        emit(fetchCatalogSingleFlight(forceRefresh))
    }

    /**
     * Single-flight network fetch. Mutex is held only around API work and cache write —
     * never across Flow.emit. Empty successful responses are NOT cached.
     */
    private suspend fun fetchCatalogSingleFlight(forceRefresh: Boolean): Resource<FaceSwapCatalog> {
        catalogMutex.withLock {
            if (!forceRefresh) {
                _cachedCatalog.value?.takeIf {
                    it.categories.isNotEmpty() && it.templates.isNotEmpty()
                }?.let { return Resource.Success(it) }
            }

            catalogLoadInFlight.set(true)
            try {
                preferenceManager.warmFaceSwapCache()

                when (val registerResult = ensureRegistered()) {
                    is Resource.Error -> return Resource.Error(
                        registerResult.message ?: "Failed to register device",
                    )
                    is Resource.Success, is Resource.Loading -> Unit
                }

                val categoriesResult = fetchCategoriesFromApi()
                if (categoriesResult is Resource.Error) {
                    return Resource.Error(
                        categoriesResult.message ?: "Failed to load categories",
                    )
                }
                val categories = (categoriesResult as Resource.Success).data.orEmpty()

                val templatesResult = fetchTemplatesFromApi()
                if (templatesResult is Resource.Error) {
                    return Resource.Error(
                        templatesResult.message ?: "Failed to load templates",
                    )
                }
                val templates = (templatesResult as Resource.Success).data.orEmpty()

                val catalog = FaceSwapCatalog(categories, templates)
                // Only cache usable catalogs so a later open can retry.
                if (categories.isNotEmpty() && templates.isNotEmpty()) {
                    _cachedCatalog.value = catalog
                }
                return Resource.Success(catalog)
            } finally {
                catalogLoadInFlight.set(false)
            }
        }
    }

    /** Reuses saved token when present; otherwise Register Device API then save token. */
    private suspend fun ensureRegistered(): Resource<String> {
        val existing = preferenceManager.peekFaceSwapToken()
            ?: preferenceManager.readFaceSwapToken()
        if (!existing.isNullOrBlank()) {
            Log.d(TAG, "Repository: register skipped — token already available")
            return Resource.Success(existing)
        }

        val deviceId = deviceIdManager.getDeviceId()
        return when (val result = safeApiCall {
            apiService.registerDevice(FaceSwapRegisterRequest(deviceId))
        }) {
            is Resource.Success -> {
                val token = result.data?.data?.token
                if (token.isNullOrBlank()) {
                    Resource.Error(result.data?.message ?: "Invalid register response")
                } else {
                    preferenceManager.saveFaceSwapToken(token)
                    Resource.Success(token)
                }
            }
            is Resource.Error -> Resource.Error(mapError(result.message))
            is Resource.Loading -> Resource.Loading()
        }
    }

    private suspend fun fetchCategoriesFromApi(): Resource<List<FaceSwapCategoryDto>> {
        return when (val result = safeApiCall { apiService.getCategories() }) {
            is Resource.Success -> {
                val items = result.data?.data?.items.orEmpty()
                    .filter { it.isActive != false && !it.id.isNullOrBlank() }
                    .sortedBy { it.sortOrder ?: 0 }
                if (items.isEmpty()) {
                    Resource.Error(result.data?.message ?: "No categories found")
                } else {
                    Resource.Success(items)
                }
            }
            is Resource.Error -> Resource.Error(mapError(result.message))
            is Resource.Loading -> Resource.Loading()
        }
    }

    private suspend fun fetchTemplatesFromApi(): Resource<List<FaceSwapTemplateDto>> {
        return when (val result = safeApiCall { apiService.getTemplates() }) {
            is Resource.Success -> {
                val items = result.data?.data?.items.orEmpty()
                    .filter {
                        it.isActive != false &&
                            !it.id.isNullOrBlank() &&
                            (it.mediaType.isNullOrBlank() || it.mediaType.equals("image", true))
                    }
                    .sortedBy { it.sortOrder ?: 0 }
                Resource.Success(items)
            }
            is Resource.Error -> Resource.Error(mapError(result.message))
            is Resource.Loading -> Resource.Loading()
        }
    }

    override fun registerDevice(deviceId: String): Flow<Resource<String>> = flow {
        emit(Resource.Loading())
        preferenceManager.warmFaceSwapCache()
        val existing = preferenceManager.peekFaceSwapToken()
            ?: preferenceManager.readFaceSwapToken()
        if (!existing.isNullOrBlank()) {
            emit(Resource.Success(existing))
            return@flow
        }
        when (val result = safeApiCall {
            apiService.registerDevice(FaceSwapRegisterRequest(deviceId))
        }) {
            is Resource.Success -> {
                val token = result.data?.data?.token
                if (token.isNullOrBlank()) {
                    emit(Resource.Error(result.data?.message ?: "Invalid register response"))
                } else {
                    preferenceManager.saveFaceSwapToken(token)
                    emit(Resource.Success(token))
                }
            }
            is Resource.Error -> emit(Resource.Error(mapError(result.message)))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun getCategories(): Flow<Resource<List<FaceSwapCategoryDto>>> = flow {
        _cachedCatalog.value?.categories?.takeIf { it.isNotEmpty() }?.let { cached ->
            emit(Resource.Success(cached))
            return@flow
        }
        emit(Resource.Loading())
        emit(fetchCategoriesFromApi())
    }

    override fun getTemplates(): Flow<Resource<List<FaceSwapTemplateDto>>> = flow {
        _cachedCatalog.value?.templates?.takeIf { it.isNotEmpty() }?.let { cached ->
            emit(Resource.Success(cached))
            return@flow
        }
        emit(Resource.Loading())
        emit(fetchTemplatesFromApi())
    }

    override fun swapFace(
        sourceImage: MultipartBody.Part,
        templateId: String,
    ): Flow<Resource<FaceSwapGenerateData>> = flow {
        emit(Resource.Loading())
        val templateBody = templateId.toRequestBody("text/plain".toMediaTypeOrNull())
        when (val result = safeApiCall {
            apiService.swapFace(sourceImage, templateBody)
        }) {
            is Resource.Success -> {
                val data = result.data?.data
                if (data == null || data.generationId.isNullOrBlank()) {
                    emit(Resource.Error(result.data?.message ?: "Invalid swap response"))
                } else {
                    emit(Resource.Success(data))
                }
            }
            is Resource.Error -> emit(Resource.Error(mapError(result.message)))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override fun getSwapStatus(generationId: String): Flow<Resource<FaceSwapStatusData>> = flow {
        when (val result = safeApiCall { apiService.getSwapStatus(generationId) }) {
            is Resource.Success -> {
                val data = result.data?.data
                if (data == null) {
                    emit(Resource.Error(result.data?.message ?: "Invalid status response"))
                } else {
                    emit(Resource.Success(data))
                }
            }
            is Resource.Error -> emit(Resource.Error(mapError(result.message)))
            is Resource.Loading -> Unit
        }
    }

    private fun mapError(raw: String?): String {
        val message = raw.orEmpty().trim()
        val normalized = message
            .removePrefix("Network call failed:")
            .trim()

        return when {
            normalized.contains("Unable to resolve host", true) ||
                normalized.contains("Failed to connect", true) ||
                normalized.contains("timeout", true) ->
                "No internet connection. Please try again."
            normalized.contains("Invalid or missing X-App-Label", true) ->
                "App configuration error. Please update the app."
            normalized.contains("Invalid or missing X-API-KEY", true) ||
                normalized.contains("Invalid API key", true) ->
                "App configuration error. Please update the app."
            normalized.startsWith("401") || normalized.contains("unauthorized", true) ->
                "Session expired. Please try again."
            normalized.startsWith("403") || normalized.contains("forbidden", true) ->
                "Access denied. Please try again."
            normalized.startsWith("404") ->
                "Requested resource was not found."
            normalized.startsWith("422") || normalized.contains("JSON decode", true) ->
                "Invalid request. Please try again."
            normalized.startsWith("429") ->
                "Too many requests. Please wait and try again."
            normalized.startsWith("500") ||
                normalized.startsWith("502") ||
                normalized.startsWith("503") ->
                "Server is busy. Please try again later."
            // Prefer backend message when present (e.g. "Invalid or missing X-App-Label header")
            normalized.isNotBlank() &&
                !normalized.matches(Regex("""^\d{3}\s+\w.*""")) ->
                normalized
            normalized.isBlank() ->
                "Something went wrong. Please try again."
            else ->
                normalized
        }
    }
}
