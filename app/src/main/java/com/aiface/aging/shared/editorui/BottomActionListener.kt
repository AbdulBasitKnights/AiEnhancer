package com.aiface.aging.shared.editorui

interface BottomActionListener {

    fun onActionTickClick(type: String, action: ((String) -> Unit)?)

    fun onActionCancelClick(type: String, action: ((String) -> Unit)?)
}
