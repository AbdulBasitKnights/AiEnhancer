package com.aiface.aging.features.frames

import com.aiface.aging.features.editor.model.ModelFramePack

interface FrameUpdateListener {
    fun onFrameUpdate(modelFramePack: ModelFramePack)
}
