package com.aiface.aging.utils

import android.content.ContentValues
import android.content.Context
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.graphics.Canvas
import android.graphics.drawable.Drawable
import android.media.MediaScannerConnection
import android.net.Uri
import android.os.Build
import android.os.Environment
import android.provider.MediaStore
import android.util.Base64
import android.util.Log
import android.view.View
import android.view.View.MeasureSpec.EXACTLY
import android.view.View.MeasureSpec.makeMeasureSpec
import com.bumptech.glide.Glide
import com.bumptech.glide.load.DataSource
import com.bumptech.glide.load.engine.GlideException
import com.bumptech.glide.request.RequestListener
import com.bumptech.glide.request.RequestOptions
import com.bumptech.glide.request.target.Target
import com.aiface.aging.R
import com.aiface.aging.shared.editorui.ModelImage
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.async
import kotlinx.coroutines.awaitAll
import kotlinx.coroutines.coroutineScope
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileOutputStream
import java.io.InputStream
import java.io.OutputStream
import kotlin.random.Random


object ImageUtils {



    fun isImageCached(context: Context, url: String?): Boolean {
        var isImageCached: Boolean? = null
        Glide.with(context)
            .load(url).override(800)
            .apply(RequestOptions().onlyRetrieveFromCache(true))
            .addListener(object : RequestListener<Drawable?> {
                override fun onLoadFailed(
                    e: GlideException?,
                    model: Any?,
                    target: Target<Drawable?>,
                    isFirstResource: Boolean
                ): Boolean {
                    isImageCached = false
                    return false
                }

                override fun onResourceReady(
                    resource: Drawable,
                    model: Any,
                    target: Target<Drawable?>?,
                    dataSource: DataSource,
                    isFirstResource: Boolean
                ): Boolean {
                    isImageCached = true
                    return false
                }
            }).preload()
        return isImageCached ?: false
    }

    fun saveMediaToStorage(context: Context, bitmap: Bitmap?): String? {
        return bitmap?.let {
            val filename = "${System.currentTimeMillis()}.png"
            var imageUri: Uri?
            var imagePath: String? = null
            try {
                var fos: OutputStream? = null
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
                    val folderPath =
                        Environment.DIRECTORY_PICTURES + File.separator + context.resources.getString(
                            R.string.app_name
                        )
                    context.contentResolver?.also { resolver ->
                        val contentValues = ContentValues().apply {
                            put(MediaStore.MediaColumns.DISPLAY_NAME, filename)
                            put(MediaStore.MediaColumns.MIME_TYPE, "image/*")
                            put(MediaStore.MediaColumns.RELATIVE_PATH, folderPath)
                            put(
                                MediaStore.Images.Media.DATE_ADDED,
                                System.currentTimeMillis() / 1000
                            )
                        }
                        imageUri =
                            resolver.insert(
                                MediaStore.Images.Media.EXTERNAL_CONTENT_URI,
                                contentValues
                            )
                        fos = imageUri?.let {
                            resolver.openOutputStream(it)
                        }
                        val file = File(
                            Environment.getExternalStorageDirectory()
                                .absoluteFile,
                            folderPath + File.separator + filename
                        )
                        imagePath = file.absolutePath
                        //update gallery
                        imageUri?.let { resolver.update(it, contentValues, null, null) }
                    }
                } else {
                    val imagesDir = getFolder(
                        context.resources.getString(
                            R.string.app_name
                        ), context
                    )
                    val image = File(imagesDir, filename)
                    fos = FileOutputStream(image)
                    imageUri = Uri.fromFile(image)
                    imagePath = image.absolutePath
                    //update gallery
                    MediaScannerConnection.scanFile(
                        context, arrayOf(image.absolutePath),
                        null, null
                    )
                }
                fos?.use {
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, it)
                }
            } catch (e: Exception) {

            }
            return imagePath
        }
    }

    /** Cache-only export for editor Next → Preview. Gallery save happens on Preview. */
    fun saveBitmapToCache(context: Context, bitmap: Bitmap?): String? {
        if (bitmap == null || bitmap.isRecycled) return null
        return runCatching {
            val file = File(context.cacheDir, "preview_${System.currentTimeMillis()}.png")
            FileOutputStream(file).use { out ->
                bitmap.compress(Bitmap.CompressFormat.PNG, 100, out)
            }
            file.absolutePath
        }.getOrNull()
    }

    private fun getFolder(folderName: String, context: Context): File {
        val dir = File(
            Environment.getExternalStorageDirectory()
                .toString() + File.separator + context.resources.getString(
                R.string.app_name
            ) + File.separator + folderName
        )
        // Make sure the path directory exists.
        if (!dir.exists()) {
            dir.mkdirs()
        }
        return dir
    }

    fun createBitmapFromView(view: View, width: Int, height: Int): Bitmap {
        Log.d("main", "====width ${width} height ${height}")
        if (width > 0 && height > 0) {
            view.measure(
                makeMeasureSpec(
                    width, EXACTLY
                ),
                makeMeasureSpec(
                    height, EXACTLY
                )
            )
        }
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            view.measuredWidth,
            view.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val background = view.background

        background?.draw(canvas)
        view.draw(canvas)

        return bitmap
    }

    fun createDefaultBitmapFromView(view: View): Bitmap {
        Log.d("main", "====width ${view.width} height ${view.height}")
        if (view.width > 0 && view.height > 0) {
            view.measure(
                makeMeasureSpec(
                    view.width, EXACTLY
                ),
                makeMeasureSpec(
                    view.height, EXACTLY
                )
            )
        }
        view.layout(0, 0, view.measuredWidth, view.measuredHeight)

        val bitmap = Bitmap.createBitmap(
            view.measuredWidth,
            view.measuredHeight, Bitmap.Config.ARGB_8888
        )
        val canvas = Canvas(bitmap)
        val background = view.background

        background?.draw(canvas)
        view.draw(canvas)

        return bitmap
    }

    suspend fun convertImagesToBase64Old(context: Context, images: List<Uri>): List<ModelImage> =
        coroutineScope {
            images.map { uri ->
                async(Dispatchers.IO) {
                    try {
                        val inputStream: InputStream? =
                            uri.let { context.contentResolver.openInputStream(it) }
                        val bitmap = BitmapFactory.decodeStream(inputStream)
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bitmap.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                        val byteArray = byteArrayOutputStream.toByteArray()
                        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                        ModelImage(base64, "image_id_" + Random(100) + ".JPEG")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null // Return null if an error occurs
                    }
                }
            }.awaitAll().filterNotNull()
        }
    suspend fun convertImagesToBase64new(context: Context, images: List<Uri>): List<ModelImage> =
        coroutineScope {
            images.map { uri ->
                async(Dispatchers.IO) {
                    try {
                        val inputStream: InputStream? =
                            uri.let { context.contentResolver.openInputStream(it) }
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        inputStream?.copyTo(byteArrayOutputStream)
                        val byteArray = byteArrayOutputStream.toByteArray()
                        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                        ModelImage(base64, "image_id_" + Random(100) + ".JPEG")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null // Return null if an error occurs
                    }
                }
            }.awaitAll().filterNotNull()
        }


    suspend fun convertBitmapsToBase64(context: Context, bitmaps: List<Bitmap?>): List<ModelImage> =
        coroutineScope {
            bitmaps.map { bmp ->
                async(Dispatchers.IO) {
                    try {
                        val byteArrayOutputStream = ByteArrayOutputStream()
                        bmp?.compress(Bitmap.CompressFormat.JPEG, 80, byteArrayOutputStream)
                        val byteArray = byteArrayOutputStream.toByteArray()
                        val base64 = Base64.encodeToString(byteArray, Base64.NO_WRAP)
                        ModelImage(base64, "image_id_" + Random(100) + ".JPEG")
                    } catch (e: Exception) {
                        e.printStackTrace()
                        null // Return null if an error occurs
                    }
                }
            }.awaitAll().filterNotNull()
        }
}