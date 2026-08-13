package com.aiface.aging.domain.usecase

import com.aiface.aging.data.model.GenerateResponse
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GenerateImageWithIdUseCase @Inject constructor(
    private val repository: MainRepository,
) {
    suspend operator fun invoke(
        prompt: String,
        mediaId: String,
        deviceId: String,
    ): Flow<Resource<GenerateResponse>> {
        return repository.generateImageWithId(
            prompt = prompt,
            mediaId = mediaId,
            deviceId = deviceId,
        )
    }
}
