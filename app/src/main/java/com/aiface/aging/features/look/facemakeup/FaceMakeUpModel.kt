package com.aiface.aging.features.look.facemakeup

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.graphics.Canvas
import androidx.lifecycle.ViewModel
import com.google.mediapipe.framework.image.BitmapImageBuilder
import com.google.mediapipe.tasks.core.BaseOptions
import com.google.mediapipe.tasks.vision.core.ImageProcessingOptions
import com.google.mediapipe.tasks.vision.core.RunningMode
import com.google.mediapipe.tasks.vision.facelandmarker.FaceLandmarker
import android.graphics.Path
import android.graphics.Paint
import android.graphics.RectF
import android.graphics.BlurMaskFilter
import android.graphics.Color
import android.graphics.PorterDuff
import android.graphics.PorterDuffXfermode
import android.graphics.RadialGradient
import android.graphics.Shader
import androidx.core.graphics.ColorUtils
import kotlin.math.hypot
import androidx.core.graphics.toColorInt


class FaceMakeUpModel  : ViewModel() {

    var faceLandmarker: FaceLandmarker? = null
    var originalBitmap: Bitmap? = null
    var makeupState: MutableMap<OverlayView.MakeupType, Int> = mutableMapOf()
    var slimFattyLevel: Float = 0f
    var smileLevel: Int = 0
    var landmarks: List<Pair<Float, Float>> = emptyList()



    fun loadModel(context: Context) {
        val baseOptions = BaseOptions.builder()
            .setModelAssetPath("models/face_landmarker.task")
            .build()
        val options = FaceLandmarker.FaceLandmarkerOptions.builder()
            .setBaseOptions(baseOptions)
            .setRunningMode(RunningMode.IMAGE)
            .setNumFaces(1)
            .build()
        faceLandmarker = FaceLandmarker.createFromOptions(context, options)
    }

    fun runFaceLandmark(bitmap: Bitmap,binding: com.aiface.aging.databinding.FragmentFaceMakeUpBinding) {
        originalBitmap = bitmap
        val mpImage = BitmapImageBuilder(bitmap).build()
        val result = faceLandmarker?.detect(mpImage, ImageProcessingOptions.builder().build())
        landmarks = result?.faceLandmarks()?.firstOrNull()?.map { point ->
            Pair(point.x() * bitmap.width, point.y() * bitmap.height)
        } ?: emptyList()
        applyMakeupAndWarpIfNeeded(binding)
    }

    fun applyMakeupAndWarpIfNeeded(binding: com.aiface.aging.databinding.FragmentFaceMakeUpBinding) {
        val src = originalBitmap ?: return
        if (landmarks.isEmpty()) {
            binding.imageEditorView.setImageBitmap(src)
            return
        }
        // Step 1: Apply all makeup to a temp bitmap
        val makeupBitmap = src.copy(Bitmap.Config.ARGB_8888, true)
        val canvas = Canvas(makeupBitmap)
        makeupState.forEach { (type, color) ->
            when (type) {
                OverlayView.MakeupType.LIPSTICK -> applyLipstick(canvas, color)
                OverlayView.MakeupType.BLUSH -> applyBlush(canvas, color)
                OverlayView.MakeupType.EYESHADOW -> applyEyeshadow(canvas, color)
                OverlayView.MakeupType.EYEBROW -> {
                    applyEyebrow(canvas, color)
                }
            }
        }
        binding.imageEditorView.setImageBitmap(makeupBitmap)
    }

    private fun applyLipstick(canvas: Canvas, colorInt: Int) {
        val lipsIndices = listOf(
            61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 308, 324, 318, 402, 317,
            14, 87, 178, 88, 95, 185, 40, 39, 37, 0, 267, 269, 270, 409, 415, 310, 311,
            312, 13, 82, 81, 42, 183, 78
        )
        val lipPoints = lipsIndices.mapNotNull { landmarks.getOrNull(it) }
        if (lipPoints.size < 10) return

        // Lip path
        val path = Path().apply {
            moveTo(lipPoints[0].first, lipPoints[0].second)
            for (pt in lipPoints.drop(1)) lineTo(pt.first, pt.second)
            close()
        }

        val centerX = lipPoints.map { it.first }.average().toFloat()
        val centerY = lipPoints.map { it.second }.average().toFloat()
        val radius = lipPoints.maxOfOrNull {
            hypot((it.first - centerX).toDouble(), (it.second - centerY).toDouble())
        }?.toFloat() ?: 50f

        // Get base alpha from slider
        val baseAlpha = Color.alpha(colorInt)

        // Gradient that respects user opacity
        val gradient = RadialGradient(
            centerX, centerY, radius,
            intArrayOf(
                (colorInt and 0x00FFFFFF) or (baseAlpha shl 24),                // center
                (colorInt and 0x00FFFFFF) or ((baseAlpha * 0.6f).toInt() shl 24), // mid
                (colorInt and 0x00FFFFFF) or ((baseAlpha * 0.3f).toInt() shl 24)  // edge
            ),
            floatArrayOf(0.0f, 0.6f, 1.0f),
            Shader.TileMode.CLAMP
        )

        val basePaint = Paint().apply {
            shader = gradient
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        canvas.drawPath(path, basePaint)

        // ✅ Optional: even highlights should fade with opacity
        val shinePaint = Paint().apply {
            color = Color.WHITE
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = (baseAlpha * 0.1f).toInt()  // scale with slider opacity
        }
        canvas.drawCircle(centerX, centerY - radius * 0.16f, radius * 0.2f, shinePaint)
    }

    private fun colorWithAlpha(color: Int, alpha: Int): Int {
        return (color and 0x00FFFFFF) or ((alpha.coerceIn(0,255)) shl 24)
    }

    private fun lightenColor(color: Int, factor: Float = 0.6f): Int {
        val r = ((1 - factor) * ((color shr 16) and 0xFF) + factor * 255).toInt()
        val g = ((1 - factor) * ((color shr 8) and 0xFF) + factor * 255).toInt()
        val b = ((1 - factor) * (color and 0xFF) + factor * 255).toInt()
        val a = (color shr 24) and 0xFF
        return (a shl 24) or (r shl 16) or (g shl 8) or b
    }


    private fun applyBlush(canvas: Canvas, colorNew: Int) {
        val leftCheekIdx = 205
        val rightCheekIdx = 425
        val cheekRadiusX = 40f
        val cheekRadiusY = 24f

        // Extract user alpha (from slider)
        val baseAlpha = Color.alpha(colorNew)

        // Lighten base color but preserve RGB
        val rgb = colorNew and 0x00FFFFFF
        val blushColor = lightenColor(rgb or (0xFF shl 24)) // lighten full-opacity version

        val paint = Paint().apply {
            isAntiAlias = true
            style = Paint.Style.FILL
            color = (blushColor and 0x00FFFFFF) or (baseAlpha shl 24) // ✅ keep user opacity
            maskFilter = BlurMaskFilter(cheekRadiusY * 0.8f, BlurMaskFilter.Blur.NORMAL)
        }

        // Draw blush directly with correct alpha
        listOf(leftCheekIdx, rightCheekIdx).forEach { idx ->
            landmarks.getOrNull(idx)?.let { (x, y) ->
                val rectF = RectF(
                    x - cheekRadiusX, y - cheekRadiusY,
                    x + cheekRadiusX, y + cheekRadiusY
                )
                canvas.drawOval(rectF, paint)
            }
        }
    }

    private fun applyEyeshadow(canvas: Canvas, color: Int) {
        // Left eye: upper eyelid/crease and lower eyelid (MediaPipe FaceMesh indices)
        val leftUpperLid = listOf(246, 161, 160, 159, 158, 157, 173)
        val leftLowerLid = listOf(133, 155, 154, 153, 145, 144, 163).reversed()
        val leftEyePoints = (leftUpperLid + leftLowerLid).mapNotNull { landmarks.getOrNull(it) }
        drawEyeshadowShape(canvas, leftEyePoints, color)

        // Right eye: upper eyelid/crease and lower eyelid
        val rightUpperLid = listOf(398, 384, 385, 386, 387, 388, 466)
        val rightLowerLid = listOf(362, 382, 381, 380, 374, 373, 390).reversed()
        val rightEyePoints = (rightUpperLid + rightLowerLid).mapNotNull { landmarks.getOrNull(it) }
        drawEyeshadowShape(canvas, rightEyePoints, color)
    }

    private fun drawEyeshadowShape(canvas: Canvas, points: List<Pair<Float, Float>>, color: Int) {
        if (points.size < 6) return
        val path = Path().apply {
            moveTo(points[0].first, points[0].second)
            for (pt in points.drop(1)) {
                lineTo(pt.first, pt.second)
            }
            close()
        }
        val paint = Paint().apply {
            this.color = color
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = 70
        }
        canvas.drawPath(path, paint)
    }

    fun applyEyebrow(canvas: Canvas, colorInt: Int?, opacity: Float = 0.4f) {
        val leftIndices = listOf(46, 53, 52, 65, 55, 107, 66, 105, 63, 70)
        val rightIndices = listOf(285, 295, 282, 283, 276, 300, 293, 334, 296, 336)

        // Lighten a color but keep alpha intact
        fun makeColorLighter(color: Int): Int {
            val alpha = Color.alpha(color)
            val red = (color shr 16) and 0xFF
            val green = (color shr 8) and 0xFF
            val blue = color and 0xFF

            val lighterRed = minOf(255, red + 50)
            val lighterGreen = minOf(255, green + 50)
            val lighterBlue = minOf(255, blue + 50)

            return (alpha shl 24) or (lighterRed shl 16) or (lighterGreen shl 8) or lighterBlue
        }

        fun drawBrow(indices: List<Int>) {
            val points = indices.mapNotNull { landmarks.getOrNull(it) }
            if (points.size < 5 || colorInt == null) return

            val lighterColor = makeColorLighter(colorInt)

            val path = Path().apply {
                moveTo(points[0].first, points[0].second)
                for (pt in points.drop(1)) lineTo(pt.first, pt.second)
                close()
            }

            val centerX = points.map { it.first }.average().toFloat()
            val centerY = points.map { it.second }.average().toFloat()
            val maxRadius = points.maxOfOrNull {
                hypot((it.first - centerX).toDouble(), (it.second - centerY).toDouble())
            }?.toFloat() ?: 30f

            val baseAlpha = (Color.alpha(colorInt) * opacity).toInt().coerceIn(0, 255)

            val radialGradient = RadialGradient(
                centerX, centerY, maxRadius,
                intArrayOf(
                    (lighterColor and 0x00FFFFFF) or (baseAlpha shl 24),                 // center
                    (lighterColor and 0x00FFFFFF) or ((baseAlpha * 0.6f).toInt() shl 24), // mid
                    (lighterColor and 0x00FFFFFF) or ((baseAlpha * 0.3f).toInt() shl 24), // border
                    (lighterColor and 0x00FFFFFF) or ((baseAlpha * 0.1f).toInt() shl 24)  // edge
                ),
                floatArrayOf(0.0f, 0.4f, 0.8f, 1.0f),
                Shader.TileMode.CLAMP
            )

            val paint = Paint().apply {
                shader = radialGradient
                style = Paint.Style.FILL
                isAntiAlias = true
            }

            canvas.drawPath(path, paint)
        }

        drawBrow(leftIndices)
        drawBrow(rightIndices)
    }

    private fun applyClearSkin(canvas: Canvas) {
        val faceIndices = listOf(10, 338, 297, 332, 284, 251, 389, 356, 454, 323, 361, 288, 397, 365, 379, 378, 400, 377, 152, 148, 176, 149, 150, 136, 172, 58, 132, 93, 234, 127, 162, 21, 54, 103, 67, 109)
        val facePoints = faceIndices.mapNotNull { landmarks.getOrNull(it) }
        if (facePoints.size < 10) return
        val path = Path().apply {
            moveTo(facePoints[0].first, facePoints[0].second)
            for (pt in facePoints.drop(1)) {
                lineTo(pt.first, pt.second)
            }
            close()
        }
        val blurred = originalBitmap?.let { fastBoxBlur(it, 12) } ?: return
        val save = canvas.save()
        canvas.clipPath(path)
        canvas.drawBitmap(blurred, 0f, 0f, null)
        canvas.restoreToCount(save)
    }

    private fun fastBoxBlur(src: Bitmap, radius: Int): Bitmap {
        if (radius < 1) return src
        val w = src.width
        val h = src.height
        val pix = IntArray(w * h)
        src.getPixels(pix, 0, w, 0, 0, w, h)
        var rsum: Int
        var gsum: Int
        var bsum: Int
        val div = radius + radius + 1
        val r = IntArray(w * h)
        val g = IntArray(w * h)
        val b = IntArray(w * h)
        var yi = 0
        for (y in 0 until h) {
            rsum = 0; gsum = 0; bsum = 0
            for (i in -radius..radius) {
                val p = pix[yi + Math.min(w - 1, Math.max(i, 0))]
                rsum += (p shr 16) and 0xFF
                gsum += (p shr 8) and 0xFF
                bsum += p and 0xFF
            }
            for (x in 0 until w) {
                r[yi] = rsum / div
                g[yi] = gsum / div
                b[yi] = bsum / div
                if (x == 0) continue
                val p1 = pix[yi + Math.max(x - radius - 1, 0)]
                val p2 = pix[yi + Math.min(x + radius, w - 1)]
                rsum += ((p2 shr 16) and 0xFF) - ((p1 shr 16) and 0xFF)
                gsum += ((p2 shr 8) and 0xFF) - ((p1 shr 8) and 0xFF)
                bsum += (p2 and 0xFF) - (p1 and 0xFF)
            }
            yi += w
        }
        yi = 0
        for (x in 0 until w) {
            rsum = 0; gsum = 0; bsum = 0
            for (i in -radius..radius) {
                val yp = Math.max(0, yi + i * w)
                rsum += r[yp]
                gsum += g[yp]
                bsum += b[yp]
            }
            var yp = yi
            for (y in 0 until h) {
                val idx = yp + x
                val a = (pix[idx] shr 24) and 0xFF
                pix[idx] = (a shl 24) or ((rsum / div) shl 16) or ((gsum / div) shl 8) or (bsum / div)
                if (y == 0) continue
                val p1 = Math.max(0, yp + (y - radius - 1) * w + x)
                val p2 = Math.min(h - 1, y + radius) * w + x
                rsum += r[p2] - r[p1]
                gsum += g[p2] - g[p1]
                bsum += b[p2] - b[p1]
            }
            yi++
        }
        val out = src.config?.let { Bitmap.createBitmap(w, h, it) }
        out?.setPixels(pix, 0, w, 0, 0, w, h)
        return out!!
    }


    @SuppressLint("UseKtx")
    private fun warpSmile(src: Bitmap, smileLevel: Int): Bitmap {
        if (landmarks.isEmpty()) return src
        val width = src.width
        val height = src.height
        val meshWidth = 20
        val meshHeight = 20
        val verts = FloatArray((meshWidth + 1) * (meshHeight + 1) * 2)
        var index = 0
        for (y in 0..meshHeight) {
            val fy = y * height / meshHeight.toFloat()
            for (x in 0..meshWidth) {
                val fx = x * width / meshWidth.toFloat()
                verts[index * 2] = fx
                verts[index * 2 + 1] = fy
                index++
            }
        }
        val leftCornerIdx = 61
        val rightCornerIdx = 291
        val mouthIndices = listOf(61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95, 185, 40, 39, 37, 0, 267, 269, 270, 409, 415, 310, 311, 312, 13, 82, 81, 42, 183, 78)
        val smileAmountY = -10f * smileLevel
        val smileAmountX = 8f * smileLevel
        val leftCorner = if (leftCornerIdx < landmarks.size) landmarks[leftCornerIdx] else null
        val rightCorner = if (rightCornerIdx < landmarks.size) landmarks[rightCornerIdx] else null
        for (i in mouthIndices) {
            if (i >= landmarks.size || leftCorner == null || rightCorner == null) continue
            val (mx, my) = landmarks[i]
            val t = ((mx - leftCorner.first) / (rightCorner.first - leftCorner.first)).coerceIn(0f, 1f)
            val moveX = lerp(-smileAmountX, smileAmountX, t)
            val moveY = smileAmountY * (1 - Math.abs(0.5f - t) * 2)
            for (v in verts.indices step 2) {
                val dx = verts[v] - mx
                val dy = verts[v + 1] - my
                val dist = Math.hypot(dx.toDouble(), dy.toDouble())
                if (dist < 30) {
                    val influence = (30 - dist) / 30
                    verts[v] = (verts[v] + moveX * influence).toFloat()
                    verts[v + 1] = (verts[v + 1] + moveY * influence).toFloat()
                }
            }
        }
        val resultBitmap = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888)
        val canvas = Canvas(resultBitmap)
        canvas.drawBitmapMesh(src, meshWidth, meshHeight, verts, 0, null, 0, null)
        return resultBitmap
    }

    private fun lerp(a: Float, b: Float, t: Float): Float {
        return a + (b - a) * t
    }



    // Debug function to visualize the eye contour path
    private fun debugDrawEyeContour(canvas: Canvas, eyeContour: List<Pair<Float, Float>>) {
        val debugPaint = Paint().apply {
            color = android.graphics.Color.GREEN
            style = Paint.Style.STROKE
            strokeWidth = 2f
            isAntiAlias = true
        }
        if (eyeContour.isNotEmpty()) {
            val path = Path().apply {
                moveTo(eyeContour[0].first, eyeContour[0].second)
                for (pt in eyeContour.drop(1)) {
                    lineTo(pt.first, pt.second)
                }
                close()
            }
            canvas.drawPath(path, debugPaint)
        }
    }



}