package com.aiface.aging.features.home

data class ProPanelHomeObject(
    var item_id: String,
    var prompt: String,
    var url: String,
    var category_name: String,
    var title: String,
    var category_id: String = "",
    var image_count: Int = 1,
)
