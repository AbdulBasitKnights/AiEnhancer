package com.aiface.aging.di


import com.aiface.aging.data.repository.DailyCheckInRepositoryImpl
import com.aiface.aging.data.repository.FaceSwapRepositoryImpl
import com.aiface.aging.data.repository.MainRepositoryImpl
import com.aiface.aging.domain.repository.DailyCheckInRepository
import com.aiface.aging.domain.repository.FaceSwapRepository
import com.aiface.aging.domain.repository.MainRepository
import dagger.Binds
import dagger.Module
import dagger.hilt.InstallIn
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
abstract class RepositoryModule {

    @Binds
    @Singleton
    abstract fun bindMainRepository(
        mainRepositoryImpl: MainRepositoryImpl
    ): MainRepository

    @Binds
    @Singleton
    abstract fun bindFaceSwapRepository(
        impl: FaceSwapRepositoryImpl
    ): FaceSwapRepository

    @Binds
    @Singleton
    abstract fun bindDailyCheckInRepository(
        impl: DailyCheckInRepositoryImpl,
    ): DailyCheckInRepository
}
