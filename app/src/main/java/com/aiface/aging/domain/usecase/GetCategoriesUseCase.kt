package com.aiface.aging.domain.usecase

import com.aiface.aging.domain.model.Category
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetCategoriesUseCase @Inject constructor(
    private val repository: MainRepository,
) {
    suspend operator fun invoke(): Flow<Resource<List<Category>>> {
        return repository.getCategories()
    }
}

