package com.aiface.aging.features.credits

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.aiface.aging.domain.model.DailyCheckInUiState
import com.aiface.aging.domain.repository.DailyCheckInRepository
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import javax.inject.Inject

@HiltViewModel
class DailyCheckInViewModel @Inject constructor(
    private val dailyCheckInRepository: DailyCheckInRepository,
) : ViewModel() {

    val uiState = dailyCheckInRepository.checkInState
        .stateIn(
            viewModelScope,
            SharingStarted.WhileSubscribed(5_000),
            DailyCheckInUiState(
                streakDays = 1,
                days = emptyList(),
                canClaim = false,
                alreadyClaimedToday = false,
                todayReward = 5,
            ),
        )

    init {
        viewModelScope.launch {
            dailyCheckInRepository.refresh()
            dailyCheckInRepository.syncWithServer()
        }
    }

    fun refresh() {
        viewModelScope.launch { dailyCheckInRepository.refresh() }
    }

    fun syncWithServer() {
        viewModelScope.launch { dailyCheckInRepository.syncWithServer() }
    }

    fun claimToday(onResult: (Int) -> Unit = {}, onError: (Throwable) -> Unit = {}) {
        viewModelScope.launch {
            dailyCheckInRepository.claimToday()
                .onSuccess(onResult)
                .onFailure(onError)
        }
    }

    suspend fun shouldAutoShowDialog(): Boolean =
        dailyCheckInRepository.shouldAutoShowDialog()

    fun markDialogShown() {
        viewModelScope.launch { dailyCheckInRepository.markDialogShownToday() }
    }
}
