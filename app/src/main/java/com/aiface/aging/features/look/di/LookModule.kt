package com.aiface.aging.features.look.di

import android.content.Context
import com.aiface.aging.features.look.data.UserPreferencesRepositoryImpl
import com.aiface.aging.features.look.domain.UserPreferencesRepository
import com.aiface.aging.features.look.utils.LookPreferenceManager
import dagger.Module
import dagger.Provides
import dagger.hilt.InstallIn
import dagger.hilt.android.qualifiers.ApplicationContext
import dagger.hilt.components.SingletonComponent
import javax.inject.Singleton

@Module
@InstallIn(SingletonComponent::class)
object LookModule {

    @Provides
    @Singleton
    fun provideLookPreferenceManager(
        @ApplicationContext context: Context,
    ): LookPreferenceManager = LookPreferenceManager(context)

    @Provides
    @Singleton
    fun provideLookUserPreferencesRepository(
        preferenceManager: LookPreferenceManager,
    ): UserPreferencesRepository = UserPreferencesRepositoryImpl(preferenceManager)
}
