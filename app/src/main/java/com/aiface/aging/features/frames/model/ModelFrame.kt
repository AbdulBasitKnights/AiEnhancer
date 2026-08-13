package com.aiface.aging.features.frames.model

import com.aiface.aging.features.editor.model.ModelFramePack

open class ModelFrame(val id: Int, val modelFramePack: ModelFramePack = ModelFramePack(), val type: FrameDataType = FrameDataType.FRAME)