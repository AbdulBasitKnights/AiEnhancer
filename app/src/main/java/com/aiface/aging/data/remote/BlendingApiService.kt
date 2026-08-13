package com.aiface.aging.data.remote

import com.aiface.aging.data.remote.dto.frames.ModelFramePackDto
import com.aiface.aging.data.remote.dto.frames.ModelFramesHeaderDto
import com.aiface.aging.utils.BLENDING_XILLI_AUTH_TOKEN
import com.aiface.aging.utils.FRAME_CATEGORY_API_HEADER
import com.aiface.aging.utils.FRAME_PACK_API_HEADER
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface BlendingApiService {

    @Headers(
        "Accept: application/json",
        "Authorization: $BLENDING_XILLI_AUTH_TOKEN",
    )
    @POST(FRAME_CATEGORY_API_HEADER)
    suspend fun getBlendingFrameHeadersData(@Body requestBody: RequestBody): List<ModelFramesHeaderDto>

    @Headers(
        "Accept: application/json",
        "Authorization: $BLENDING_XILLI_AUTH_TOKEN",
    )
    @POST(FRAME_PACK_API_HEADER)
    suspend fun getBlendingFramePacksData(@Body requestBody: RequestBody): List<ModelFramePackDto>
}
