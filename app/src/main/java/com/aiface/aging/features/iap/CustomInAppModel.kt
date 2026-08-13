package com.aiface.aging.features.iap


data class CustomInAppModel(
    val id: Int,
    val durationPlan: String,
    val description: String,
    val totalPrice: String,
    val discountedPrice: String,
    val discountPercent: String,
    val showDiscount: Boolean
) {
    val thePrice : String
        get() = when(id){
            0-> "$discountedPrice/Week"
            1-> "$discountedPrice/Month"
            2-> "$discountedPrice/Year"
            3-> "$discountedPrice/Year"
            11-> "$discountedPrice/Week"
            22-> "$discountedPrice/Month"
            33-> "$discountedPrice/Year"
            else -> {
                "$discountedPrice/Week"
            }
        }
}
