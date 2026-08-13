package com.aiface.aging.features.blender.catalog

import com.aiface.aging.data.remote.BlendingApiService
import com.aiface.aging.data.remote.dto.frames.toBlenderCategory
import com.aiface.aging.data.remote.dto.frames.toModelFramesPack
import com.aiface.aging.utils.BLENDING_FRAMES_OPTION
import com.aiface.aging.utils.getBlendingApiRequestBodyHeader
import com.aiface.aging.utils.getBlendingApiRequestBodyPack
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class BlendingCatalogRepository @Inject constructor(
    private val api: BlendingApiService,
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
                    val headers = api.getBlendingFrameHeadersData(
                        getBlendingApiRequestBodyHeader(BLENDING_FRAMES_OPTION, "50"),
                    )
                    val categories = headers.map { header ->
                        val packs = runCatching {
                            api.getBlendingFramePacksData(getBlendingApiRequestBodyPack(header.id))
                                .map { it.toModelFramesPack() }
                        }.getOrDefault(emptyList())
                        toBlenderCategory(header, packs)
                    }.filter { it.packs.isNotEmpty() }
                    cached = categories
                    categories
                }
            }
        }
}
