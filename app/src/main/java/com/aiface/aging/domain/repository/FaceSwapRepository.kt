package com.aiface.aging.domain.repository

import com.aiface.aging.data.model.faceswap.FaceSwapCategoryDto
import com.aiface.aging.data.model.faceswap.FaceSwapGenerateData
import com.aiface.aging.data.model.faceswap.FaceSwapStatusData
import com.aiface.aging.data.model.faceswap.FaceSwapTemplateDto
import com.aiface.aging.domain.model.Resource
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody

data class FaceSwapCatalog(
    val categories: List<FaceSwapCategoryDto>,
    val templates: List<FaceSwapTemplateDto>,
)

interface FaceSwapRepository {
    /** Fire-and-forget Splash/Home preload on a process-scoped IO coroutine. */
    fun preloadCatalog()

    fun peekCachedCatalog(): FaceSwapCatalog?

    fun isCatalogLoadInFlight(): Boolean

    /**
     * Returns cached catalog when available (unless [forceRefresh]), otherwise runs
     * register → categories → templates with single-flight dedupe.
     */
    fun getCatalog(forceRefresh: Boolean = false): Flow<Resource<FaceSwapCatalog>>

    fun registerDevice(deviceId: String): Flow<Resource<String>>
    fun getCategories(): Flow<Resource<List<FaceSwapCategoryDto>>>
    fun getTemplates(): Flow<Resource<List<FaceSwapTemplateDto>>>
    fun swapFace(
        sourceImage: MultipartBody.Part,
        templateId: String,
    ): Flow<Resource<FaceSwapGenerateData>>
    fun getSwapStatus(generationId: String): Flow<Resource<FaceSwapStatusData>>
}
