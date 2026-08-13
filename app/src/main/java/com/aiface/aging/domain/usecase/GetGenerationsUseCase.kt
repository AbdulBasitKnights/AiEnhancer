package com.aiface.aging.domain.usecase

import com.aiface.aging.data.model.GenerationsResponse
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetGenerationsUseCase @Inject constructor(
    private val repository: MainRepository,
) {
    suspend operator fun invoke(
        deviceId: String,
    ): Flow<Resource<GenerationsResponse>> {
        return repository.getGenerations(deviceId = deviceId)
    }
}

