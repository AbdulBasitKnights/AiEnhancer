package com.aiface.aging.features.settings

import android.graphics.drawable.Drawable

data class ModelSettings(
    val id : Int,
    val title : String,
    val isHeader : Boolean=false,
    val icon : Drawable?=null,
    val background : Drawable?=null,
    val showView:Boolean=true
)
