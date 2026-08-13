package com.aiface.aging.domain.repository

import com.aiface.aging.data.model.GenerateResponse
import com.aiface.aging.data.model.GenerationsResponse
import com.aiface.aging.data.model.NewGenerateResponse
import com.aiface.aging.domain.model.Category
import com.aiface.aging.domain.model.Plan
import com.aiface.aging.domain.model.Post
import com.aiface.aging.domain.model.RegisterResult
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.model.Subscription
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import okhttp3.RequestBody

interface MainRepository {
    suspend fun registerUser(deviceId: String): Flow<Resource<RegisterResult>>

    /** Refresh access token for an already-registered device. */
    suspend fun getAccessToken(deviceId: String): Flow<Resource<RegisterResult>>

    suspend fun getPosts(): Flow<Resource<List<Post>>>
    suspend fun getCategories(): Flow<Resource<List<Category>>>

    /** New image generation on the generationlab backend. */
    suspend fun generateImage(parts: List<MultipartBody.Part>): Flow<Resource<NewGenerateResponse>>

    /** Subscription APIs. */
    suspend fun getCurrentSubscription(): Flow<Resource<Subscription>>
    suspend fun getPlans(): Flow<Resource<List<Plan>>>
    suspend fun purchasePlan(planUuid: String): Flow<Resource<Subscription>>
    suspend fun generateImageWithImage(
        file: List<okhttp3.MultipartBody.Part>,
        prompt: okhttp3.RequestBody?,
        modelId: okhttp3.RequestBody?,
        deviceId: okhttp3.RequestBody?,
    ): Flow<Resource<GenerateResponse>>
    suspend fun generateImageWithId(
        prompt: String,
        mediaId: String,
        deviceId: String,
    ): Flow<Resource<GenerateResponse>>
    suspend fun getGenerations(deviceId: String): Flow<Resource<GenerationsResponse>>
}
