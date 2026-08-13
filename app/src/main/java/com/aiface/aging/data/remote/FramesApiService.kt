package com.aiface.aging.data.remote

import com.aiface.aging.data.remote.dto.frames.ModelFrameHomeCategoriesDto
import com.aiface.aging.utils.ALL_FRAME_CATEGORY_API_HEADER
import com.aiface.aging.utils.TOP_XILLI_AUTH_TOKEN
import okhttp3.RequestBody
import retrofit2.http.Body
import retrofit2.http.Headers
import retrofit2.http.POST

interface FramesApiService {

    @Headers(
        "Accept: application/json",
        "Authorization: $TOP_XILLI_AUTH_TOKEN",
    )
    @POST(ALL_FRAME_CATEGORY_API_HEADER)
    suspend fun getAllFramesData(@Body requestBody: RequestBody): List<ModelFrameHomeCategoriesDto>
}
