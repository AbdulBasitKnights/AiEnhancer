package com.aiface.aging.domain.model

import androidx.annotation.Keep

@Keep
data class Post(
    val userId: Int,
    val id: Int,
    val title: String,
    val body: String
)
