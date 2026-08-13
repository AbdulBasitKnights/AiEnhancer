package com.aiface.aging.shared

import android.content.Context
import android.util.Log
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.aiface.aging.AiFaceApp
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Local generation credits (DataStore prefs: credit_store / total_credits).
 * - New user: [ensureFreeCredits] grants 5 once when key missing
 * - Existing balance kept and updated after each successful generation
 */
@Singleton
class CreditManager @Inject constructor(
    @ApplicationContext private val context: Context,
) {

    companion object {
        private val Context.dataStore by preferencesDataStore(name = "credit_store")

        private val KEY_CREDITS = intPreferencesKey("total_credits")
        private val KEY_LAST_DAILY_CLAIM = stringPreferencesKey("last_daily_claim")
        private val KEY_WEEKLY_PREMIUM_CLAIMED =
            booleanPreferencesKey("weekly_premium_claimed")
        private val KEY_FREE_TRIAL_CLAIMED =
            booleanPreferencesKey("free_trial_claimed")

        private val formatter = SimpleDateFormat("yyyy-MM-dd", Locale.US)
        private const val IMAGE_COST = 1

        /** One-time free credits when prefs have no balance yet. */
        const val FREE_STARTER_CREDITS = 5

        private val DAILY_CLAIM_CREDITS: Int
            get() = AiFaceApp.dailyCredits.coerceAtLeast(FREE_STARTER_CREDITS)

        private val WEEKLY_PREMIUM_CREDITS: Int
            get() = AiFaceApp.weeklyCredits

        private val FREE_TRIAL_CREDITS: Int
            get() = FREE_STARTER_CREDITS
    }

    val creditsFlow: Flow<Int> =
        context.dataStore.data.map { prefs ->
            prefs[KEY_CREDITS] ?: 0
        }

    suspend fun getCredits(): Int {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_CREDITS] ?: 0
    }

    /**
     * If [KEY_CREDITS] already exists → keep (after prior generations).
     * If missing → grant [FREE_STARTER_CREDITS] (5).
     */
    suspend fun ensureFreeCredits(amount: Int = FREE_STARTER_CREDITS): Int {
        var balance = 0
        context.dataStore.edit { prefs ->
            if (prefs.contains(KEY_CREDITS)) {
                balance = prefs[KEY_CREDITS] ?: 0
            } else {
                balance = amount.coerceAtLeast(0)
                prefs[KEY_CREDITS] = balance
                Log.d("CreditSystem", "Granted free starter credits=$balance")
            }
        }
        return balance
    }

    suspend fun canGenerateImage(): Boolean {
        return getCredits() >= IMAGE_COST
    }

    suspend fun consumeImageCredit(): Boolean {
        if (!canGenerateImage()) return false
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CREDITS] ?: 0
            prefs[KEY_CREDITS] = (current - IMAGE_COST).coerceAtLeast(0)
        }
        return true
    }

    suspend fun addCredits(amount: Int) {
        if (amount <= 0) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CREDITS] ?: 0
            prefs[KEY_CREDITS] = current + amount
        }
    }

    /** Restore balance from server after re-install / clear-data when server has higher total. */
    suspend fun setCreditsIfHigher(remoteTotal: Int) {
        if (remoteTotal <= 0) return
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CREDITS] ?: 0
            if (remoteTotal > current) {
                prefs[KEY_CREDITS] = remoteTotal
            }
        }
    }

    private fun utcToday(): String =
        java.time.LocalDate.now(java.time.ZoneOffset.UTC).toString()

    suspend fun canClaimDaily(): Boolean {
        val prefs = context.dataStore.data.first()
        val lastClaim = prefs[KEY_LAST_DAILY_CLAIM]
        val today = utcToday()
        return lastClaim != today
    }

    /** @deprecated Use [DailyCheckInRepository.claimToday] for streak UI. Kept for legacy callers. */
    suspend fun claimDailyCredits(): Boolean {
        Log.d("CreditSystem", "daily=$DAILY_CLAIM_CREDITS")
        if (!canClaimDaily()) return false
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CREDITS] ?: 0
            prefs[KEY_CREDITS] = current + DAILY_CLAIM_CREDITS
            prefs[KEY_LAST_DAILY_CLAIM] = utcToday()
        }
        Log.d("CreditSystem", "Claimed daily=$DAILY_CLAIM_CREDITS")
        return true
    }

    suspend fun hasClaimedWeeklyPremium(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_WEEKLY_PREMIUM_CLAIMED] ?: false
    }

    suspend fun claimWeeklyPremiumCredits(): Boolean {
        if (hasClaimedWeeklyPremium()) return false
        context.dataStore.edit { prefs ->
            prefs[KEY_CREDITS] = WEEKLY_PREMIUM_CREDITS
            prefs[KEY_WEEKLY_PREMIUM_CLAIMED] = true
        }
        return true
    }

    suspend fun hasClaimedFreeTrial(): Boolean {
        val prefs = context.dataStore.data.first()
        return prefs[KEY_FREE_TRIAL_CLAIMED] ?: false
    }

    suspend fun claimFreeTrialCredits(): Boolean {
        if (hasClaimedFreeTrial()) return false
        context.dataStore.edit { prefs ->
            val current = prefs[KEY_CREDITS] ?: 0
            prefs[KEY_CREDITS] = current + FREE_TRIAL_CREDITS
            prefs[KEY_FREE_TRIAL_CLAIMED] = true
        }
        return true
    }

    suspend fun resetAllCredits() {
        context.dataStore.edit { prefs ->
            prefs[KEY_CREDITS] = 0
            prefs.remove(KEY_LAST_DAILY_CLAIM)
            prefs[KEY_WEEKLY_PREMIUM_CLAIMED] = false
            prefs[KEY_FREE_TRIAL_CLAIMED] = false
        }
    }

    suspend fun resetProCredits() {
        context.dataStore.edit { prefs ->
            val weeklyClaimed = prefs[KEY_WEEKLY_PREMIUM_CLAIMED] ?: false
            val trialClaimed = prefs[KEY_FREE_TRIAL_CLAIMED] ?: false
            if (weeklyClaimed) {
                val current = prefs[KEY_CREDITS] ?: 0
                prefs[KEY_CREDITS] = (current - WEEKLY_PREMIUM_CREDITS).coerceAtLeast(0)
                prefs[KEY_WEEKLY_PREMIUM_CLAIMED] = false
            }
            if (trialClaimed) {
                val current = prefs[KEY_CREDITS] ?: 0
                prefs[KEY_CREDITS] = (current - FREE_TRIAL_CREDITS).coerceAtLeast(0)
                prefs[KEY_FREE_TRIAL_CLAIMED] = false
            }
        }
    }

    suspend fun resetOnlyProCreditsKeepDaily() {
        context.dataStore.edit { prefs ->
            var updatedCredits = prefs[KEY_CREDITS] ?: 0
            val weeklyClaimed = prefs[KEY_WEEKLY_PREMIUM_CLAIMED] ?: false
            val trialClaimed = prefs[KEY_FREE_TRIAL_CLAIMED] ?: false
            if (weeklyClaimed) {
                updatedCredits -= WEEKLY_PREMIUM_CREDITS
                prefs[KEY_WEEKLY_PREMIUM_CLAIMED] = false
            }
            if (trialClaimed) {
                updatedCredits -= FREE_TRIAL_CREDITS
                prefs[KEY_FREE_TRIAL_CLAIMED] = false
            }
            prefs[KEY_CREDITS] = updatedCredits.coerceAtLeast(0)
        }
    }
}
