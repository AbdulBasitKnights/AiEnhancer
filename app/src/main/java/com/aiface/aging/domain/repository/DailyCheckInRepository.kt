package com.aiface.aging.domain.repository

import com.aiface.aging.domain.model.DailyCheckInUiState
import kotlinx.coroutines.flow.Flow

interface DailyCheckInRepository {
    val checkInState: Flow<DailyCheckInUiState>

    suspend fun refresh()

    /** Pull server state by device id and merge with local (survives clear-data re-register). */
    suspend fun syncWithServer()

    suspend fun claimToday(): Result<Int>

    suspend fun markDialogShownToday()

    suspend fun shouldAutoShowDialog(): Boolean
}
