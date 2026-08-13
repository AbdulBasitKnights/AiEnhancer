package com.aiface.aging.domain.usecase

import com.aiface.aging.data.model.GenerateResponse
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import okhttp3.RequestBody
import javax.inject.Inject

class GenerateImageWithImageUseCase @Inject constructor(
    private val repository: MainRepository,
) {
    suspend operator fun invoke(
        file: List<MultipartBody.Part>,
        prompt: RequestBody?,
        modelId: RequestBody?,
        deviceId: RequestBody?,
    ): Flow<Resource<GenerateResponse>> {
        return repository.generateImageWithImage(
            file = file,
            prompt = prompt,
            modelId = modelId,
            deviceId = deviceId,
        )
    }
}

