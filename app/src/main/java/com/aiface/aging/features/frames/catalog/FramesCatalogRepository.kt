package com.aiface.aging.features.frames.catalog

import com.aiface.aging.data.remote.FramesApiService
import com.aiface.aging.data.remote.dto.frames.toBlenderCategory
import com.aiface.aging.features.blender.catalog.BlenderCategory
import com.aiface.aging.utils.TOP_FRAMES_OPTION
import com.aiface.aging.utils.getApiRequestBodyHeader
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class FramesCatalogRepository @Inject constructor(
    private val api: FramesApiService,
) {
    private val mutex = Mutex()
    @Volatile
    private var cached: List<BlenderCategory> = emptyList()

    suspend fun getCategories(forceRefresh: Boolean = false): Result<List<BlenderCategory>> =
        withContext(Dispatchers.IO) {
            mutex.withLock {
                if (!forceRefresh && cached.isNotEmpty()) {
                    return@withContext Result.success(cached)
                }
                runCatching {
                    val list = api.getAllFramesData(
                        getApiRequestBodyHeader(TOP_FRAMES_OPTION, "100"),
                    ).map { it.toBlenderCategory() }
                        .filter { it.packs.isNotEmpty() }
                    cached = list
                    list
                }
            }
        }
}
