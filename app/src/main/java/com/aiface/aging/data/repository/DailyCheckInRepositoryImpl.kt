package com.aiface.aging.data.repository

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiface.aging.AiFaceApp
import com.aiface.aging.data.model.CheckInClaimRequest
import com.aiface.aging.data.remote.ApiService
import com.aiface.aging.domain.model.CheckInDayStatus
import com.aiface.aging.domain.model.CheckInDayUi
import com.aiface.aging.domain.model.DailyCheckInUiState
import com.aiface.aging.domain.repository.DailyCheckInRepository
import com.aiface.aging.shared.CreditManager
import com.aiface.aging.utils.ApiEnvelope
import com.aiface.aging.utils.DeviceIdManager
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import java.time.LocalDate
import java.time.ZoneOffset
import java.time.temporal.ChronoUnit
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class DailyCheckInRepositoryImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val apiService: ApiService,
    private val creditManager: CreditManager,
    private val deviceIdManager: DeviceIdManager,
) : DailyCheckInRepository {

    private val _checkInState = MutableStateFlow(DailyCheckInUiState(
        streakDays = 1,
        days = emptyList(),
        canClaim = false,
        alreadyClaimedToday = false,
        todayReward = rewardForDay(1),
    ))
    override val checkInState: Flow<DailyCheckInUiState> = _checkInState

    override suspend fun refresh() {
        val snapshot = readLocalSnapshot()
        val ui = buildUiState(snapshot)
        _checkInState.update { ui }
    }

    override suspend fun syncWithServer() {
        val deviceId = deviceIdManager.getDeviceId()
        bindDeviceIfNeeded(deviceId)
        try {
            val response = apiService.getCheckInState(deviceId)
            if (!response.isSuccessful) return
            val body = response.body() ?: return
            if (!ApiEnvelope.isSuccess(body.status)) return
            val remote = body.data ?: return
            mergeRemoteState(remote, deviceId)
            flushPendingClaim(deviceId)
            refresh()
        } catch (e: Exception) {
            Log.d(TAG, "Check-in sync skipped (offline or endpoint missing): ${e.message}")
            flushPendingClaim(deviceId)
        }
    }

    override suspend fun claimToday(): Result<Int> {
        val deviceId = deviceIdManager.getDeviceId()
        bindDeviceIfNeeded(deviceId)
        val snapshot = normalizeSnapshot(readLocalSnapshot())
        if (!snapshot.canClaimToday) {
            return Result.failure(IllegalStateException("Already claimed today"))
        }

        val reward = rewardForDay(snapshot.cycleDay)
        creditManager.addCredits(reward)

        val todayUtc = utcToday()
        val nextCycleDay = if (snapshot.cycleDay >= CYCLE_LENGTH) 1 else snapshot.cycleDay + 1

        context.checkInStore.edit { prefs ->
            prefs[KEY_CYCLE_DAY] = nextCycleDay
            prefs[KEY_LAST_CLAIM_UTC] = todayUtc
            prefs[KEY_BOUND_DEVICE_ID] = deviceId
            prefs[KEY_PENDING_CLAIM] = buildPendingPayload(
                deviceId = deviceId,
                claimUtc = todayUtc,
                cycleDay = snapshot.cycleDay,
                reward = reward,
            )
        }

        refresh()
        syncClaimToServer(deviceId, todayUtc, snapshot.cycleDay, reward)
        return Result.success(reward)
    }

    override suspend fun markDialogShownToday() {
        context.checkInStore.edit { prefs ->
            prefs[KEY_DIALOG_SHOWN_UTC] = utcToday()
        }
    }

    override suspend fun shouldAutoShowDialog(): Boolean {
        val snapshot = normalizeSnapshot(readLocalSnapshot())
        if (!snapshot.canClaimToday) return false
        val prefs = context.checkInStore.data.first()
        return prefs[KEY_DIALOG_SHOWN_UTC] != utcToday()
    }

    private suspend fun flushPendingClaim(deviceId: String) {
        val prefs = context.checkInStore.data.first()
        val pending = prefs[KEY_PENDING_CLAIM] ?: return
        val parts = pending.split("|")
        if (parts.size != 4) return
        syncClaimToServer(
            deviceId = parts[0].ifBlank { deviceId },
            claimUtc = parts[1],
            cycleDay = parts[2].toIntOrNull() ?: return,
            reward = parts[3].toIntOrNull() ?: return,
        )
    }

    private suspend fun syncClaimToServer(
        deviceId: String,
        claimUtc: String,
        cycleDay: Int,
        reward: Int,
    ) {
        try {
            val response = apiService.claimCheckIn(
                CheckInClaimRequest(
                    deviceId = deviceId,
                    claimUtc = claimUtc,
                    cycleDay = cycleDay,
                    reward = reward,
                ),
            )
            if (response.isSuccessful && ApiEnvelope.isSuccess(response.body()?.status)) {
                context.checkInStore.edit { prefs ->
                    prefs.remove(KEY_PENDING_CLAIM)
                    response.body()?.data?.totalCredits?.let { remoteCredits ->
                        creditManager.setCreditsIfHigher(remoteCredits)
                    }
                }
            }
        } catch (e: Exception) {
            Log.d(TAG, "Claim sync queued offline: ${e.message}")
        }
    }

    private suspend fun mergeRemoteState(
        remote: com.aiface.aging.data.model.CheckInStateDto,
        localDeviceId: String,
    ) {
        if (!remote.deviceId.isNullOrBlank() && remote.deviceId != localDeviceId) return

        val local = readLocalSnapshot()
        val remoteCycle = remote.cycleDay?.coerceIn(1, CYCLE_LENGTH) ?: local.cycleDay
        val remoteLast = remote.lastClaimUtc

        val mergedCycle: Int
        val mergedLast: String?

        when {
            remoteLast.isNullOrBlank() -> {
                mergedCycle = local.cycleDay
                mergedLast = local.lastClaimUtc
            }
            local.lastClaimUtc.isNullOrBlank() -> {
                mergedCycle = remoteCycle
                mergedLast = remoteLast
            }
            remoteLast >= local.lastClaimUtc -> {
                mergedCycle = remoteCycle
                mergedLast = remoteLast
            }
            else -> {
                mergedCycle = local.cycleDay
                mergedLast = local.lastClaimUtc
            }
        }

        context.checkInStore.edit { prefs ->
            prefs[KEY_CYCLE_DAY] = mergedCycle
            mergedLast?.let { prefs[KEY_LAST_CLAIM_UTC] = it }
            prefs[KEY_BOUND_DEVICE_ID] = localDeviceId
        }

        remote.totalCredits?.let { creditManager.setCreditsIfHigher(it) }
    }

    private suspend fun bindDeviceIfNeeded(deviceId: String) {
        val prefs = context.checkInStore.data.first()
        if (prefs[KEY_BOUND_DEVICE_ID] == null) {
            context.checkInStore.edit { it[KEY_BOUND_DEVICE_ID] = deviceId }
        }
    }

    private suspend fun readLocalSnapshot(): LocalSnapshot {
        val prefs = context.checkInStore.data.first()
        return LocalSnapshot(
            cycleDay = (prefs[KEY_CYCLE_DAY] ?: 1).coerceIn(1, CYCLE_LENGTH),
            lastClaimUtc = prefs[KEY_LAST_CLAIM_UTC],
        )
    }

    private fun normalizeSnapshot(raw: LocalSnapshot): LocalSnapshot {
        val today = utcToday()
        val last = raw.lastClaimUtc
        if (last.isNullOrBlank()) {
            return raw.copy(canClaimToday = true)
        }
        val gap = daysBetweenUtc(last, today)
        return when {
            gap == 0L -> raw.copy(canClaimToday = false, alreadyClaimedToday = true)
            gap == 1L -> raw.copy(canClaimToday = true)
            else -> raw.copy(
                cycleDay = 1,
                canClaimToday = true,
                streakBroken = true,
            )
        }
    }

    private fun buildUiState(raw: LocalSnapshot): DailyCheckInUiState {
        val snapshot = normalizeSnapshot(raw)
        val focusDay = if (snapshot.alreadyClaimedToday) {
            val previous = snapshot.cycleDay - 1
            if (previous < 1) CYCLE_LENGTH else previous
        } else {
            snapshot.cycleDay
        }

        val days = (1..CYCLE_LENGTH).map { day ->
            val status = when {
                snapshot.alreadyClaimedToday && day <= focusDay -> {
                    if (day == focusDay) CheckInDayStatus.CURRENT else CheckInDayStatus.CLAIMED
                }
                !snapshot.alreadyClaimedToday && day < snapshot.cycleDay -> CheckInDayStatus.CLAIMED
                !snapshot.alreadyClaimedToday && day == snapshot.cycleDay -> CheckInDayStatus.CURRENT
                else -> CheckInDayStatus.LOCKED
            }
            CheckInDayUi(
                dayNumber = day,
                reward = rewardForDay(day),
                status = status,
                showCheckmark = status == CheckInDayStatus.CLAIMED ||
                    (status == CheckInDayStatus.CURRENT && snapshot.alreadyClaimedToday),
            )
        }

        return DailyCheckInUiState(
            streakDays = focusDay.coerceIn(1, CYCLE_LENGTH),
            days = days,
            canClaim = snapshot.canClaimToday,
            alreadyClaimedToday = snapshot.alreadyClaimedToday,
            todayReward = rewardForDay(focusDay),
        )
    }

    private data class LocalSnapshot(
        val cycleDay: Int,
        val lastClaimUtc: String?,
        val canClaimToday: Boolean = true,
        val alreadyClaimedToday: Boolean = false,
        val streakBroken: Boolean = false,
    )

    companion object {
        private const val TAG = "DailyCheckIn"
        private const val CYCLE_LENGTH = 7

        private val Context.checkInStore by preferencesDataStore(name = "checkin_store")

        private val KEY_CYCLE_DAY = intPreferencesKey("cycle_day")
        private val KEY_LAST_CLAIM_UTC = stringPreferencesKey("last_claim_utc")
        private val KEY_BOUND_DEVICE_ID = stringPreferencesKey("bound_device_id")
        private val KEY_PENDING_CLAIM = stringPreferencesKey("pending_claim")
        private val KEY_DIALOG_SHOWN_UTC = stringPreferencesKey("dialog_shown_utc")

        fun utcToday(): String =
            LocalDate.now(ZoneOffset.UTC).toString()

        fun daysBetweenUtc(from: String, to: String): Long {
            val start = LocalDate.parse(from)
            val end = LocalDate.parse(to)
            return ChronoUnit.DAYS.between(start, end)
        }

        fun rewardForDay(day: Int): Int {
            val daily = AiFaceApp.dailyCredits.toInt().coerceAtLeast(CreditManager.FREE_STARTER_CREDITS)
            return if (day == CYCLE_LENGTH) daily * 4 else daily
        }

        private fun buildPendingPayload(
            deviceId: String,
            claimUtc: String,
            cycleDay: Int,
            reward: Int,
        ): String = "$deviceId|$claimUtc|$cycleDay|$reward"
    }
}
