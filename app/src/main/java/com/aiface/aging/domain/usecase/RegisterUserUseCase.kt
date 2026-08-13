package com.aiface.aging.domain.usecase


import com.aiface.aging.domain.model.RegisterResult
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class RegisterUserUseCase @Inject constructor(
    private val repository: MainRepository,
) {
    suspend operator fun invoke(deviceId: String): Flow<Resource<RegisterResult>> {
        return repository.registerUser(deviceId)
    }
}
