package com.aiface.aging.utils

import com.aiface.aging.features.editor.model.ModelFrameHeader
import com.aiface.aging.features.filters.model.ModelFilters


object FrameUtils {
    fun getTopFramesHeader(): ModelFrameHeader {
        val args = ModelFrameHeader()
        args.apply {
          //  parent = "Top"
            parent = "Template"
        }
        return args
    }

    fun getSimpleFramesHeader(): ModelFrameHeader {
        val args = ModelFrameHeader()
        args.apply {
         //   parent = "Simple"
            parent = "Background"
        }
        return args
    }

    fun getStickersHeader(): ModelFrameHeader {
        val args = ModelFrameHeader()
        args.apply {
            parent = "Stickers"
        }
        return args
    }

    fun getFilterHeader(): ModelFilters {
        val args = ModelFilters()
        args.apply {
            parent = "Filters"
        }
        return args
    }
}