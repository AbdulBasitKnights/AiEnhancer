package com.aiface.aging.utils

import android.content.Context
import android.content.res.Resources
import android.graphics.Bitmap
import android.graphics.BitmapFactory
import android.net.Uri
import android.os.ParcelFileDescriptor
import android.util.Log
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaTypeOrNull
import okhttp3.MultipartBody
import okhttp3.RequestBody.Companion.toRequestBody
import java.io.ByteArrayOutputStream
import java.io.File
import java.io.FileInputStream
import java.io.IOException
import java.io.InputStream
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.also
import kotlin.apply
import kotlin.io.use
import kotlin.jvm.javaClass
import kotlin.text.toIntOrNull

@Singleton
class ImageCompressor @Inject constructor(
    @ApplicationContext private val context: Context,
) {
    companion object {
        private const val TAG = "ImageCompressor"
        private const val MAX_DIMENSION = 1280     // px
        private const val COMPRESS_QUALITY = 85    // JPEG quality
        private const val MAX_SIZE_BYTES = 5 * 1024 * 1024  // 5 MB hard cap
    }

    /**
     * Compresses the image at [uri] on the IO dispatcher.
     *
     * Supported URI schemes — handled in order of reliability:
     *   `file://`            → direct [FileInputStream] (no IPC, no permission check)
     *   `android.resource://` → [BitmapFactory.decodeResource] (drawable/raw assets)
     *   `content://`         → [ContentResolver.openInputStream] with a PFD fallback
     *
     * After [SelectImageBottomSheet] was updated to always deliver stable `file://`
     * URIs for gallery and camera, the `content://` path is now a safety net only.
     */
    suspend fun compress(uri: Uri): ByteArray = withContext(Dispatchers.IO) {
        when (uri.scheme) {
            "file" -> compressFileUri(uri)
            "android.resource" -> compressResourceUri(uri)
            else -> compressContentUri(uri)   // content:// and any other scheme
        }
    }

    /**
     * Compresses the image and wraps it as a named multipart part ready for Retrofit.
     */
    suspend fun toMultipartPart(uri: Uri, fieldName: String): MultipartBody.Part =
        withContext(Dispatchers.IO) {
            val bytes = compress(uri)
            MultipartBody.Part.createFormData(
                name = fieldName,
                filename = "img_${System.currentTimeMillis()}.jpg",
                body = bytes.toRequestBody("image/jpeg".toMediaTypeOrNull()),
            )
        }

    // ── Per-scheme compress paths ──────────────────────────────────────────────

    /** `file://` path — direct FileInputStream, zero ContentResolver overhead. */
    private fun compressFileUri(uri: Uri): ByteArray {
        val path = uri.path ?: throw IOException("Null path in file URI: $uri")
        val file = File(path)
        if (!file.exists()) throw IOException("File does not exist: $path")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        FileInputStream(file).use { BitmapFactory.decodeStream(it, null, bounds) }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = FileInputStream(file).use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            ?: throw IOException("BitmapFactory returned null for file: $path")

        return encodeToJpeg(bitmap)
    }

    /** `android.resource://` path — drawable / raw resources bundled in the APK. */
    private fun compressResourceUri(uri: Uri): ByteArray {
        val resId = uri.lastPathSegment?.toIntOrNull()
            ?: throw IOException("Malformed android.resource URI — no resource ID: $uri")
        val bitmap = try {
            BitmapFactory.decodeResource(context.resources, resId)
        } catch (e: Resources.NotFoundException) {
            throw IOException("Resource not found for URI: $uri", e)
        } ?: throw IOException("BitmapFactory returned null for resource URI: $uri")
        return encodeToJpeg(bitmap)
    }

    /**
     * `content://` path — used as a safety net for URIs not yet converted to `file://`.
     * Tries [ContentResolver.openInputStream] first, falls back to a
     * [ParcelFileDescriptor]-backed stream if the first attempt throws.
     */
    private fun compressContentUri(uri: Uri): ByteArray {
        val stream = openContentStream(uri)
            ?: throw IOException("Cannot open stream for URI: $uri")

        val bounds = BitmapFactory.Options().apply { inJustDecodeBounds = true }
        stream.use { BitmapFactory.decodeStream(it, null, bounds) }

        val decodeOpts = BitmapFactory.Options().apply {
            inSampleSize = calculateSampleSize(bounds.outWidth, bounds.outHeight)
            inPreferredConfig = Bitmap.Config.RGB_565
        }
        val bitmap = openContentStream(uri)?.use { BitmapFactory.decodeStream(it, null, decodeOpts) }
            ?: throw IOException("Failed to decode bitmap from URI: $uri")

        return encodeToJpeg(bitmap)
    }

    // ── Content stream opening ─────────────────────────────────────────────────

    private fun openContentStream(uri: Uri): InputStream? {
        // Attempt 1 — direct input stream
        try {
            val s = context.contentResolver.openInputStream(uri)
            if (s != null) return s
            Log.w(TAG, "openInputStream returned null for $uri")
        } catch (e: SecurityException) {
            Log.w(TAG, "SecurityException on openInputStream for $uri — trying PFD fallback", e)
        } catch (e: Exception) {
            Log.w(TAG, "${e.javaClass.simpleName} on openInputStream for $uri — trying PFD fallback", e)
        }

        // Attempt 2 — ParcelFileDescriptor (handles some URIs that return null above)
        return try {
            val pfd = context.contentResolver.openFileDescriptor(uri, "r")
                ?: return null.also { Log.w(TAG, "openFileDescriptor returned null for $uri") }
            ParcelFileDescriptor.AutoCloseInputStream(pfd)
        } catch (e: Exception) {
            Log.e(TAG, "All stream-open attempts failed for $uri: ${e.javaClass.simpleName}: ${e.message}")
            null
        }
    }

    // ── Encode ────────────────────────────────────────────────────────────────

    private fun encodeToJpeg(source: Bitmap): ByteArray {
        val scaled = scaleBitmap(source)
        if (scaled !== source) source.recycle()

        val out = ByteArrayOutputStream()
        scaled.compress(Bitmap.CompressFormat.JPEG, COMPRESS_QUALITY, out)
        scaled.recycle()

        val bytes = out.toByteArray()
        check(bytes.size <= MAX_SIZE_BYTES) {
            "Compressed image (${bytes.size} bytes) exceeds $MAX_SIZE_BYTES byte limit"
        }
        return bytes
    }

    // ── Dimension helpers ──────────────────────────────────────────────────────

    private fun calculateSampleSize(w: Int, h: Int): Int {
        var size = 1
        if (h > MAX_DIMENSION || w > MAX_DIMENSION) {
            val halfH = h / 2
            val halfW = w / 2
            while (halfH / size >= MAX_DIMENSION && halfW / size >= MAX_DIMENSION) size *= 2
        }
        return size
    }

    private fun scaleBitmap(src: Bitmap): Bitmap {
        val w = src.width
        val h = src.height
        if (w <= MAX_DIMENSION && h <= MAX_DIMENSION) return src
        val scale = MAX_DIMENSION.toFloat() / kotlin.comparisons.maxOf(w, h)
        return Bitmap.createScaledBitmap(src, (w * scale).toInt(), (h * scale).toInt(), true)
    }
}
