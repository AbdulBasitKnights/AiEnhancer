package com.aiface.aging.data

import android.content.Context

import com.aiface.aging.features.filters.model.ModelFiltersPackDto
import com.aiface.aging.shared.ASSETS_PATH_FILTER_HEADER
import com.aiface.aging.shared.ASSETS_PATH_FILTER_PACK
import com.aiface.aging.features.filters.model.ModelFiltersDto
import com.aiface.aging.utils.AppUtils.deserializeFilterHeaderFromJson
import com.aiface.aging.utils.AppUtils.deserializeFilterPackFromJson
import org.json.JSONException
import org.json.JSONObject
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LocalFiltersDataSource @Inject constructor(
    private val context: Context
) {
    private fun provideJson(context: Context?, JsonPath: String): String? {
        val json: String? = try {
            val inputStream: InputStream = context?.assets!!.open(JsonPath)
            val size = inputStream.available()
            val buffer = ByteArray(size)
            inputStream.read(buffer)
            inputStream.close()
            String(buffer, Charsets.UTF_8)
        } catch (ex: IOException) {
            ex.printStackTrace()
            return null
        }
        return json
    }


    suspend fun getLocalHeadersDataNew(option: String): List<ModelFiltersDto> {
        val headerDataList: ArrayList<ModelFiltersDto> = ArrayList()
        try {
            val jsonObject = JSONObject(
                provideJson(
                    context,
                    ASSETS_PATH_FILTER_HEADER,
                )!!,
            )
            val jsonArray = jsonObject.getJSONArray(option)
            for (i in 0 until jsonArray.length()) {
                val headerObject = jsonArray.getJSONObject(i)
                headerDataList.add(
                    deserializeFilterHeaderFromJson(
                        headerObject.toString()
                    )
                )
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return headerDataList
    }




    suspend fun getLocalPacksDataNew(cat_id: Int): List<ModelFiltersPackDto> {
        val packDataList: ArrayList<ModelFiltersPackDto> = ArrayList()
        try {
            val jsonObject = JSONObject(
                provideJson(
                    context,
                    ASSETS_PATH_FILTER_PACK
                )!!,
            )
            val jsonArray = jsonObject.getJSONArray(cat_id.toString())
            for (i in 0 until jsonArray.length()) {
                val packObject = jsonArray.getJSONObject(i)
                packDataList.add(
                    deserializeFilterPackFromJson(
                        packObject.toString()
                    )
                )
            }
        } catch (e: JSONException) {
            e.printStackTrace()
        }
        return packDataList
    }
}