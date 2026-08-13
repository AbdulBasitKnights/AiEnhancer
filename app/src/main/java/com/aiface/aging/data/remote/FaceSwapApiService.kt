package com.aiface.aging.data.remote

import com.aiface.aging.data.model.faceswap.FaceSwapBaseResponse
import com.aiface.aging.data.model.faceswap.FaceSwapCategoryDto
import com.aiface.aging.data.model.faceswap.FaceSwapGenerateData
import com.aiface.aging.data.model.faceswap.FaceSwapItemsData
import com.aiface.aging.data.model.faceswap.FaceSwapRegisterData
import com.aiface.aging.data.model.faceswap.FaceSwapRegisterRequest
import com.aiface.aging.data.model.faceswap.FaceSwapStatusData
import com.aiface.aging.data.model.faceswap.FaceSwapTemplateDto
import okhttp3.MultipartBody
import okhttp3.RequestBody
import retrofit2.Response
import retrofit2.http.Body
import retrofit2.http.GET
import retrofit2.http.Multipart
import retrofit2.http.POST
import retrofit2.http.Part
import retrofit2.http.Path

interface FaceSwapApiService {

    /**
     * Headers via [FaceSwapAuthInterceptor]:
     * Content-Type: application/json, X-API-KEY, X-App-Label
     */
    @POST("auth/device")
    suspend fun registerDevice(
        @Body request: FaceSwapRegisterRequest,
    ): Response<FaceSwapBaseResponse<FaceSwapRegisterData>>

    /**
     * Headers via [FaceSwapAuthInterceptor]:
     * X-API-KEY, X-App-Label
     */
    @GET("categories")
    suspend fun getCategories(): Response<FaceSwapBaseResponse<FaceSwapItemsData<FaceSwapCategoryDto>>>

    /**
     * Headers via [FaceSwapAuthInterceptor]:
     * X-API-KEY, X-App-Label
     */
    @GET("templates")
    suspend fun getTemplates(): Response<FaceSwapBaseResponse<FaceSwapItemsData<FaceSwapTemplateDto>>>

    /**
     * Headers via [FaceSwapAuthInterceptor]:
     * X-API-KEY, X-App-Label, Authorization: Bearer &lt;token&gt;
     * Content-Type is set by OkHttp for multipart (with boundary).
     */
    @Multipart
    @POST("swap-face")
    suspend fun swapFace(
        @Part sourceImage: MultipartBody.Part,
        @Part("template_id") templateId: RequestBody,
    ): Response<FaceSwapBaseResponse<FaceSwapGenerateData>>

    /**
     * Headers via [FaceSwapAuthInterceptor]:
     * X-API-KEY, X-App-Label, Authorization: Bearer &lt;token&gt;
     */
    @GET("generations/face-swap/{generationId}")
    suspend fun getSwapStatus(
        @Path("generationId") generationId: String,
    ): Response<FaceSwapBaseResponse<FaceSwapStatusData>>
}
