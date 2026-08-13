package com.aiface.aging.data.initializer

import com.google.firebase.remoteconfig.FirebaseRemoteConfig
import com.aiface.aging.data.params.RemoteKeys
import com.aiface.aging.data.params.RemoteValue


class RemoteLogicConfiguration private constructor() : BaseRemoteConfiguration() {

    companion object {
        private const val PREFS_NAME = "remote_logic_config"

        @Volatile
        private var instance: RemoteLogicConfiguration? = null

        fun getInstance(): RemoteLogicConfiguration {
            return instance ?: synchronized(this) {
                instance ?: RemoteLogicConfiguration().also { instance = it }
            }
        }
    }

    override fun getPrefsName(): String = PREFS_NAME

    override fun sync(remoteConfig: FirebaseRemoteConfig) {
        with(remoteConfig) {
            saveToLocal(SelectedLFOConfig)
            saveToLocal(EnableLogTesterConfig)
        }
    }

    // Rating config keys
    private object SelectedLFOConfig: RemoteKeys.StringEnumKey<RemoteValue.Selected>("logic_select_lfo1", RemoteValue.Selected.UNSELECTED)
    private object EnableLogTesterConfig: RemoteKeys.BooleanKey("allow_log_for_tester", false)

    val selectedLfo: RemoteValue.Selected
        get() = SelectedLFOConfig.get()

    val enableLogTester: Boolean
        get() = EnableLogTesterConfig.get()
}
