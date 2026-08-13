package com.aiface.aging.domain.model

enum class CheckInDayStatus {
    CLAIMED,
    CURRENT,
    LOCKED,
}

data class CheckInDayUi(
    val dayNumber: Int,
    val reward: Int,
    val status: CheckInDayStatus,
    val showCheckmark: Boolean = status == CheckInDayStatus.CLAIMED,
    val isBonusDay: Boolean = dayNumber == 7,
)

data class DailyCheckInUiState(
    val streakDays: Int,
    val days: List<CheckInDayUi>,
    val canClaim: Boolean,
    val alreadyClaimedToday: Boolean,
    val todayReward: Int,
    val isLoading: Boolean = false,
)

data class DailyCheckInSnapshot(
    val cycleDay: Int,
    val lastClaimUtc: String?,
    val boundDeviceId: String?,
)
