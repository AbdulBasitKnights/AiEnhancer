package com.aiface.aging.data.initializer

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.aiface.aging.data.params.RemoteKeys
import com.aiface.aging.data.params.RemoteValue


class RemoteUiConfiguration private constructor() : BaseRemoteConfiguration() {

    companion object {
        private const val PREFS_NAME = "remote_ui_config"

        @Volatile
        private var instance: RemoteUiConfiguration? = null

        fun getInstance(): RemoteUiConfiguration {
            return instance ?: synchronized(this) {
                instance ?: RemoteUiConfiguration().also { instance = it }
            }
        }
    }

    override fun getPrefsName(): String = PREFS_NAME

    override fun sync(remoteConfig: FirebaseRemoteConfig) {
        with(remoteConfig) {
            saveToLocal(LayoutNativeLfo)
            saveToLocal(LayoutNativeOb)
            saveToLocal(LayoutNativeFullOb)
            saveToLocal(DailyCredits)
            saveToLocal(WeeklyProCredits)
            saveToLocal(FreeTrialCredits)
        }
    }

    private object LayoutNativeLfo: RemoteKeys.StringEnumKey<RemoteValue.UiResistMeta>(
        "native_lfo_resist_meta",
        RemoteValue.UiResistMeta.FULL_LAYOUT_ADMOB
    )

    private object LayoutNativeOb: RemoteKeys.StringEnumKey<RemoteValue.UiResistMeta>(
        "native_onb_resist_meta",
        RemoteValue.UiResistMeta.FULL_LAYOUT_ADMOB
    )

    private object LayoutNativeFullOb: RemoteKeys.StringEnumKey<RemoteValue.UiResistMeta>(
        "native_fullscr_resist_meta",
        RemoteValue.UiResistMeta.FULL_LAYOUT_ADMOB
    )

    private object FreeTrialCredits : RemoteKeys.DoubleKey("trial_credits", 50.0)
    private object WeeklyProCredits : RemoteKeys.DoubleKey("weekly_pro_credits", 500.0)
    private object DailyCredits : RemoteKeys.DoubleKey("daily_credits", 10.0)


    val layoutNativeLfo: RemoteValue.UiResistMeta
        get() = LayoutNativeLfo.get()

    val layoutNativeOb: RemoteValue.UiResistMeta
        get() = LayoutNativeOb.get()

    val layoutNativeFullOb: RemoteValue.UiResistMeta
        get() = LayoutNativeFullOb.get()

    val freeTrialCredits: Double get() = FreeTrialCredits.get()
    val weeklyProCredits: Double get() = WeeklyProCredits.get()
    val dailyCredits: Double get() = DailyCredits.get()

}