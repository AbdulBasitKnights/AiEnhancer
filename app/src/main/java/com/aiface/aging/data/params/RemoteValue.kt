package com.aiface.aging.data.params

interface RemoteEnumString {
    val remoteValue: String
}

object RemoteValue {
    enum class UiResistMeta(override val remoteValue: String) :
        RemoteEnumString {
        META_ONLY("meta_only"),
        FULL_LAYOUT_META("full_layout_meta"),
        FULL_LAYOUT_ADMOB("full_layout_admob"),
    }

    enum class Selected(override val remoteValue: String) :
        RemoteEnumString {
        SELECTED("select_language"),
        UNSELECTED("unselect_language")
    }
}