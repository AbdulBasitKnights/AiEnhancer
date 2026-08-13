package com.aiface.aging.domain.usecase


import com.aiface.aging.data.model.NewGenerateResponse
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import okhttp3.MultipartBody
import javax.inject.Inject

class GenerateImageUseCase @Inject constructor(
    private val repository: MainRepository,
) {
    suspend operator fun invoke(
        parts: List<MultipartBody.Part>,
    ): Flow<Resource<NewGenerateResponse>> = repository.generateImage(parts)
}
