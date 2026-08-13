package com.aiface.aging.features.blender

import com.aiface.aging.features.editor.model.ModelFramePack

interface BlendingBottomSheetPassData {
    fun onSelectedBlendingItem(position: Int, frameSelectedList: ArrayList<ModelFramePack>)
}
