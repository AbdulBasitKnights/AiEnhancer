package com.aiface.aging.utils

import android.content.Context
import android.graphics.Bitmap
import android.net.Uri
import android.view.View
import android.view.ViewGroup
import androidx.appcompat.app.AppCompatActivity
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import androidx.fragment.app.FragmentTransaction
import com.google.gson.Gson
import com.aiface.aging.MainActivity
import com.aiface.aging.features.filters.model.ModelFiltersPackDto
import com.aiface.aging.features.filters.model.ModelFiltersDto
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

object AppUtils {

    suspend fun fetchImagePathsSorted(context: Context, assetFolder: String): List<String> =
        withContext(Dispatchers.IO) {
            val imagePaths = mutableListOf<String>()
            try {
                val fileList = context.assets.list(assetFolder)?.filter { it.endsWith(".png") }
                fileList?.sortedBy { it.removeSuffix(".png").toIntOrNull() ?: Int.MAX_VALUE }
                    ?.forEach { fileName ->
                        val path = "file:///android_asset/$assetFolder/$fileName"
                        imagePaths.add(path)
                    }
            } catch (e: IOException) {
                e.printStackTrace() // Handle the error as needed
            }
            imagePaths
        }

    fun loadFragment(id: Int, fragment: Fragment, activity: AppCompatActivity) {
        val fragmentManager: FragmentManager = activity.supportFragmentManager
        val fragmentTransaction = fragmentManager.beginTransaction()
        fragmentTransaction.replace(id, fragment)
        fragmentTransaction.addToBackStack(null)
        fragmentTransaction.setTransition(FragmentTransaction.TRANSIT_FRAGMENT_OPEN)
        fragmentTransaction.commit()
    }

    fun getFilePathFromContentUri(contentUri: Uri, context: Context): String? {
        return UriUtils.getPathFromUri(context, contentUri)
    }

    fun deserializeFilterHeaderFromJson(jsonString: String?): ModelFiltersDto {
        val gson = Gson()
        return gson.fromJson(jsonString, ModelFiltersDto::class.java)
    }

    fun deserializeFilterPackFromJson(jsonString: String?): ModelFiltersPackDto {
        val gson = Gson()
        return gson.fromJson(jsonString, ModelFiltersPackDto::class.java)
    }
    fun View.setCustomMargins(left: Int, top: Int, right: Int, bottom: Int) {
        if (layoutParams is ViewGroup.MarginLayoutParams) {
            val p = layoutParams as ViewGroup.MarginLayoutParams
            p.setMargins(left, top, right, bottom)
            requestLayout()
        }
    }

    fun isNightMode(context: Context): Boolean {
        val nightModeFlags =
            context.resources.configuration.uiMode and android.content.res.Configuration.UI_MODE_NIGHT_MASK
        return nightModeFlags == android.content.res.Configuration.UI_MODE_NIGHT_YES
    }

    fun getMain(activity: FragmentActivity?): MainActivity? = activity as? MainActivity

    fun hideHomeBannerAd(activity: FragmentActivity?) {
        getMain(activity)?.hideHomeBannerAd()
    }

    fun showHomeBannerAd(activity: FragmentActivity?) {
        getMain(activity)?.showHomeBannerAd()
    }

    fun convertBitmapToImagePath(context: Context, bitmap: Bitmap): String? {
        var imagePath: String? = null

        try {
            // Create a temporary file in your app's private directory
            val tempDir = context.cacheDir
            val tempFile = File(tempDir, "${System.currentTimeMillis()}.jpg")

            // Create a FileOutputStream to save the Bitmap to the temporary file
            val fos = FileOutputStream(tempFile)

            // Compress the Bitmap to JPEG format (you can change the format if needed)
            bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos)

            // Close the FileOutputStream
            fos.flush()
            fos.close()

            // Get the file path of the temporary file
            imagePath = tempFile.absolutePath
        } catch (e: IOException) {
            e.printStackTrace()
        }

        return imagePath
    }
}