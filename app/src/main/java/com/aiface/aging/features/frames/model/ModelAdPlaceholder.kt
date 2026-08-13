package com.aiface.aging.features.frames.model

import com.aiface.aging.features.editor.model.ModelFramePack
import kotlin.random.Random

class ModelAdPlaceholder :
    ModelFrame(Random(99999999).nextInt(), ModelFramePack(), FrameDataType.AD)