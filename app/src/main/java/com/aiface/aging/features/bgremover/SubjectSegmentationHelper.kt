package com.aiface.aging.features.bgremover

import android.graphics.Bitmap
import android.graphics.Rect
import android.util.Log
import com.google.mlkit.vision.common.InputImage
import com.google.mlkit.vision.segmentation.subject.SubjectSegmentation
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenter
import com.google.mlkit.vision.segmentation.subject.SubjectSegmenterOptions
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException
import kotlin.coroutines.suspendCoroutine

/**
 * ML Kit subject segmentation wrapper.
 * Handles GMS optional-module native load failures (ABI mismatch, missing .so).
 */
object SubjectSegmentationHelper {

    private const val TAG = "SubjectSegmentation"

    private val segmenterOptions: SubjectSegmenterOptions by lazy {
        SubjectSegmenterOptions.Builder()
            .enableForegroundConfidenceMask()
            .enableForegroundBitmap()
            .build()
    }

    fun createSegmenter(): SubjectSegmenter = SubjectSegmentation.getClient(segmenterOptions)

    /** True when Play Services delivered a broken / wrong-ABI native module. */
    fun isNativeModuleError(error: Throwable?): Boolean {
        var cause: Throwable? = error
        while (cause != null) {
            when (cause) {
                is UnsatisfiedLinkError,
                is NoClassDefFoundError,
                is LinkageError -> return true
            }
            val message = cause.message.orEmpty()
            if (message.contains("libmediapipe_tasks_jni", ignoreCase = true)) return true
            if (message.contains("EM_X86_64", ignoreCase = true)) return true
            if (message.contains("EM_AARCH64", ignoreCase = true)) return true
            if (message.contains("dlopen failed", ignoreCase = true)) return true
            cause = cause.cause
        }
        return false
    }

    /** User-facing copy when GMS ships a broken native module. */
    fun displayMessage(error: Throwable, fallback: String): String =
        if (isNativeModuleError(error)) {
            "Background removal is unavailable on this device. Update Google Play Services, then try again."
        } else {
            error.message ?: fallback
        }

    suspend fun segmentForegroundCropped(source: Bitmap): Bitmap = suspendCoroutine { continuation ->
        val inputImage = InputImage.fromBitmap(source, 0)
        try {
            createSegmenter()
                .process(inputImage)
                .addOnSuccessListener { result ->
                    try {
                        val foreground = result.foregroundBitmap
                        if (foreground == null) {
                            continuation.resumeWithException(
                                IllegalStateException("No subject detected in photo"),
                            )
                            return@addOnSuccessListener
                        }
                        val box = calculateBoundingBox(foreground)
                        val output = if (box != null) {
                            Bitmap.createBitmap(
                                foreground,
                                box.left,
                                box.top,
                                box.width(),
                                box.height(),
                            )
                        } else {
                            foreground
                        }
                        continuation.resume(output)
                    } catch (t: Throwable) {
                        continuation.resumeWithException(t)
                    }
                }
                .addOnFailureListener { error ->
                    continuation.resumeWithException(error)
                }
        } catch (linkError: LinkageError) {
            Log.e(TAG, "Native ML Kit module failed to load", linkError)
            continuation.resumeWithException(linkError)
        } catch (t: Throwable) {
            Log.e(TAG, "Segmentation could not start", t)
            continuation.resumeWithException(t)
        }
    }

    private fun calculateBoundingBox(maskBitmap: Bitmap): Rect? {
        val width = maskBitmap.width
        val height = maskBitmap.height
        val pixels = IntArray(width * height)
        maskBitmap.getPixels(pixels, 0, width, 0, 0, width, height)

        var left = width
        var top = height
        var right = 0
        var bottom = 0

        for (y in 0 until height) {
            for (x in 0 until width) {
                if (pixels[y * width + x] != 0) {
                    if (x < left) left = x
                    if (x > right) right = x
                    if (y < top) top = y
                    if (y > bottom) bottom = y
                }
            }
        }

        return if (left <= right && top <= bottom) {
            Rect(left, top, right + 1, bottom + 1)
        } else {
            null
        }
    }
}
