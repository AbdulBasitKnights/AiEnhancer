package com.aiface.aging.data.repository

import com.aiface.aging.data.model.GenerateResponse
import com.aiface.aging.data.model.GenerationsResponse
import com.aiface.aging.data.model.NewGenerateResponse
import com.aiface.aging.data.model.PlanDto
import com.aiface.aging.data.model.PurchaseSubscriptionRequest
import com.aiface.aging.data.model.RegisterRequest
import com.aiface.aging.data.model.SubscriptionDataDto
import com.aiface.aging.data.remote.ApiService
import com.aiface.aging.domain.model.Category
import com.aiface.aging.domain.model.Plan
import com.aiface.aging.domain.model.Post
import com.aiface.aging.domain.model.RegisterResult
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.model.Subscription
import com.aiface.aging.domain.model.Template
import com.aiface.aging.domain.repository.MainRepository
import com.aiface.aging.utils.ApiEnvelope
import com.aiface.aging.utils.SafeApiCall
import com.aiface.aging.utils.TokenExpiryParser
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject
import com.aiface.aging.data.model.Post as PostDto

class MainRepositoryImpl @Inject constructor(
    private val newApiService: ApiService
) : MainRepository, SafeApiCall() {

    override suspend fun registerUser(deviceId: String): Flow<Resource<RegisterResult>> = flow {
        emit(Resource.Loading())
        val response = safeApiCall { newApiService.registerUser(RegisterRequest(deviceId)) }
        emit(mapAuthResponse(response, fallbackError = "Registration failed"))
    }

    override suspend fun getAccessToken(deviceId: String): Flow<Resource<RegisterResult>> = flow {
        emit(Resource.Loading())
        val response = safeApiCall { newApiService.getAccessToken(RegisterRequest(deviceId)) }
        emit(mapAuthResponse(response, fallbackError = "Failed to get access token"))
    }

    private fun mapAuthResponse(
        response: Resource<com.aiface.aging.data.model.RegisterResponse>,
        fallbackError: String,
    ): Resource<RegisterResult> {
        return when (response) {
            is Resource.Success -> {
                val body = response.data
                if (body == null || !ApiEnvelope.isSuccess(body.status)) {
                    Resource.Error(body?.message ?: fallbackError)
                } else {
                    val dto = body.data
                    if (dto == null || dto.token.isBlank()) {
                        Resource.Error(body.message ?: "Empty auth response")
                    } else {
                        val expiresAtMillis = TokenExpiryParser.resolveExpiresAtMillis(
                            expiresAt = dto.expiresAt,
                            expiresIn = dto.expiresIn,
                        )
                        Resource.Success(
                            RegisterResult(
                                token = dto.token,
                                deviceId = dto.deviceId,
                                appName = dto.appName,
                                userId = dto.userId,
                                expiresAtMillis = expiresAtMillis,
                            ),
                        )
                    }
                }
            }
            is Resource.Error -> Resource.Error(response.message ?: fallbackError)
            is Resource.Loading -> Resource.Loading()
        }
    }

    // ── Public categories (new backend) ───────────────────────────────────────

    override suspend fun getCategories(): Flow<Resource<List<Category>>> = flow {
        emit(Resource.Loading())
        val response = safeApiCall { newApiService.getPublicCategories() }
        when (response) {
            is Resource.Success -> {
                val body = response.data
                if (body == null || !ApiEnvelope.isSuccess(body.status)) {
                    emit(Resource.Error(body?.message ?: "Unknown Error"))
                } else {
                    val categories = body.data?.data.orEmpty().map { dto ->
                        Category(
                            id = dto.id,
                            appName = dto.appName,
                            name = dto.name,
                            description = dto.description,
                            position = dto.position,
                            isActive = dto.isActive,
                            templateCount = dto.templateCount,
                            templates = dto.templates.orEmpty().map { t ->
                                Template(
                                    id = t.id,
                                    categoryId = t.categoryId,
                                    title = t.title,
                                    generationType = t.generationType,
                                    isActive = t.isActive,
                                    priority = t.priority,
                                    isPro = t.isPro || t.isPremium,
                                    imageCount = t.imageCount,
                                    vendorTemplateId = t.vendorTemplateId,
                                    mediaUrl = t.mediaUrl,
                                    gifUrl = t.gifUrl,
                                    thumbnailUrl = t.thumbnailUrl,
                                    prompt = t.prompt,
                                    negativePrompt = t.negativePrompt,
                                )
                            },
                        )
                    }
                    emit(Resource.Success(categories))
                }
            }
            is Resource.Error -> emit(Resource.Error(response.message ?: "Unknown Error"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    // ── New image generation (new backend) ───────────────────────────────────

    override suspend fun generateImage(
        parts: List<MultipartBody.Part>,
    ): Flow<Resource<NewGenerateResponse>> = flow {
        emit(Resource.Loading())
        val response = safeApiCall { newApiService.generateImage(parts) }
        when (response) {
            is Resource.Success -> {
                val body = response.data
                if (body == null || !ApiEnvelope.isSuccess(body.status)) {
                    emit(Resource.Error(body?.message ?: "Generation failed"))
                } else {
                    emit(Resource.Success(body))
                }
            }
            is Resource.Error -> emit(Resource.Error(response.message ?: "Generation failed"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    // ── Subscription APIs (new backend) ──────────────────────────────────────

    override suspend fun getCurrentSubscription(): Flow<Resource<Subscription>> = flow {
        emit(Resource.Loading())
        val response = safeApiCall { newApiService.getCurrentSubscription() }
        when (response) {
            is Resource.Success -> {
                val dto = response.data?.data
                if (dto != null) {
                    emit(Resource.Success(dto.toDomain()))
                } else {
                    // "No subscription on file" → return an empty free-tier object
                    emit(Resource.Success(Subscription(null, null, null, 0, 0, null, null, null)))
                }
            }
            is Resource.Error -> emit(Resource.Error(response.message ?: "Failed to load subscription"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override suspend fun getPlans(): Flow<Resource<List<Plan>>> = flow {
        emit(Resource.Loading())
        val response = safeApiCall { newApiService.getPlans() }
        when (response) {
            is Resource.Success -> {
                val plans = response.data?.data
                    ?.filter { it.isActive }
                    ?.map { it.toDomain() }
                    ?: emptyList()
                emit(Resource.Success(plans))
            }
            is Resource.Error -> emit(Resource.Error(response.message ?: "Failed to load plans"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    override suspend fun purchasePlan(planUuid: String): Flow<Resource<Subscription>> = flow {
        emit(Resource.Loading())
        val response = safeApiCall {
            newApiService.purchasePlan(PurchaseSubscriptionRequest(planUuid))
        }
        when (response) {
            is Resource.Success -> {
                val dto = response.data?.data
                if (dto != null) {
                    emit(Resource.Success(dto.toDomain()))
                } else {
                    emit(Resource.Error("Empty purchase response"))
                }
            }
            is Resource.Error -> emit(Resource.Error(response.message ?: "Purchase failed"))
            is Resource.Loading -> emit(Resource.Loading())
        }
    }

    // ── Private mappers ───────────────────────────────────────────────────────

    private fun SubscriptionDataDto.toDomain() = Subscription(
        planId = planId,
        planName = planName,
        durationDays = durationDays,
        videoCreditsRemaining = videoCreditsRemaining,
        imageCreditsRemaining = imageCreditsRemaining,
        startedAt = startedAt,
        expiresAt = expiresAt,
        status = status,
    )

    private fun PlanDto.toDomain() = Plan(
        id = id,
        name = name,
        durationDays = durationDays,
        videoCredits = videoGenerationCredits,
        imageCredits = imageGenerationCredits,
        isActive = isActive,
    )

    // ── Legacy endpoints (old backend) ────────────────────────────────────────

    override suspend fun getPosts(): Flow<Resource<List<Post>>> = flow {
//        emit(Resource.Loading())
//        val response: Resource<List<PostDto>> = safeApiCall { apiService.getPosts() }
//        when (response) {
//            is Resource.Success -> {
//                val posts = response.data?.map { dto ->
//                    Post(dto.userId, dto.id, dto.title, dto.body)
//                } ?: emptyList()
//                emit(Resource.Success(posts))
//            }
//            is Resource.Error -> emit(Resource.Error(response.message ?: "Unknown Error"))
//            is Resource.Loading -> emit(Resource.Loading())
//        }
    }

    override suspend fun generateImageWithImage(
        file: List<MultipartBody.Part>,
        prompt: RequestBody?,
        modelId: RequestBody?,
        deviceId: RequestBody?,
    ): Flow<Resource<GenerateResponse>> = flow {
//        emit(Resource.Loading())
//        val response = safeApiCall {
//            apiService.generateImageWithImage(
//                file = file,
//                prompt = prompt,
//                media_id = modelId,
//                device_id = deviceId,
//            )
//        }
//        emit(response)
    }

    override suspend fun generateImageWithId(
        prompt: String,
        mediaId: String,
        deviceId: String,
    ): Flow<Resource<GenerateResponse>> = flow {
//        emit(Resource.Loading())
//        val response = safeApiCall {
//            apiService.generateImageWithId(
//                prompt = prompt,
//                media_id = mediaId,
//                device_id = deviceId,
//            )
//        }
//        emit(response)
    }

    override suspend fun getGenerations(deviceId: String): Flow<Resource<GenerationsResponse>> = flow {
//        emit(Resource.Loading())
//        val response = safeApiCall {
//            apiService.getGenerations(deviceId = deviceId)
//        }
//        emit(response)
//    }
}
}
