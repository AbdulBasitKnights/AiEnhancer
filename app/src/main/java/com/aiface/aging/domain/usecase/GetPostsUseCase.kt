package com.aiface.aging.domain.usecase

import com.aiface.aging.domain.model.Post
import com.aiface.aging.domain.model.Resource
import com.aiface.aging.domain.repository.MainRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetPostsUseCase @Inject constructor(
    private val repository: MainRepository
) {
    suspend operator fun invoke(): Flow<Resource<List<Post>>> {
        return repository.getPosts()
    }
}
