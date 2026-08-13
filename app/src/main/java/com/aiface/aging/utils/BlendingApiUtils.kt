package com.aiface.aging.utils

import okhttp3.MultipartBody
import okhttp3.RequestBody

const val BLENDING_XILLI_APP_ID = "25"
const val BLENDING_XILLI_AUTH_TOKEN = "Bearer 22|PWayQjoz9XtR3WCH6jImnKWOlOdvJNXA1eX42mYk"
const val BLENDING_FRAMES_OPTION = "blending"
const val BLENDING_FRAMES_PARENT = "Blending"
const val XILLI_BASE_URL = "https://api.xilliapps.com/"
const val FRAME_CATEGORY_API_HEADER = "api/getFrameHeader"
const val FRAME_PACK_API_HEADER = "api/getFrameBody"

fun getBlendingApiRequestBodyHeader(option: String, limit: String): RequestBody {
    return MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("trending", "1")
        .addFormDataPart("common", "1")
        .addFormDataPart("app", BLENDING_XILLI_APP_ID)
        .addFormDataPart("index", "0")
        .addFormDataPart("limit", limit)
        .addFormDataPart("option", option)
        .build()
}

fun getBlendingApiRequestBodyPack(catId: Int): RequestBody {
    return MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("cat_id", catId.toString())
        .addFormDataPart("access", "*")
        .addFormDataPart("index", "0")
        .addFormDataPart("limit", "100")
        .build()
}
