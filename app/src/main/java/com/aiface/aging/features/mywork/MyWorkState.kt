package com.aiface.aging.features.mywork


data class MyWorkState(
    val imgList : List<MediaStoreImage> ? = emptyList(),
    val errorMsg : String? = null,
    val isLoading : Boolean = false
)