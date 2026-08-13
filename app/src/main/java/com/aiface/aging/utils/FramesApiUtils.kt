package com.aiface.aging.utils

import okhttp3.MultipartBody
import okhttp3.RequestBody

const val ALL_FRAME_CATEGORY_API_HEADER = "api/getCategoriesAndFrames"
const val TOP_FRAMES_OPTION = "Top"
const val TOP_FRAMES_PARENT = "Top"
const val TOP_XILLI_AUTH_TOKEN = "Bearer 21|nDfmMuBLFbfvbFnlGwG15YyBnm2znrI7IEbIEHBR"

fun getApiRequestBodyHeader(option: String, limit: String): RequestBody {
    return MultipartBody.Builder()
        .setType(MultipartBody.FORM)
        .addFormDataPart("trending", "1")
        .addFormDataPart("common", "1")
        .addFormDataPart("app", "24")
        .addFormDataPart("index", "0")
        .addFormDataPart("limit", limit)
        .addFormDataPart("option", option)
        .build()
}
