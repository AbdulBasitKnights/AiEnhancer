package com.aiface.aging.features.look.facemakeup

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.util.AttributeSet
import android.view.View

class OverlayView @JvmOverloads constructor(
    context: Context, attrs: AttributeSet? = null, defStyleAttr: Int = 0
) : View(context, attrs, defStyleAttr) {
    private var landmarks: List<Pair<Float, Float>>? = null
    private var makeupType: MakeupType = MakeupType.LIPSTICK
    private var imageWidth: Int = 0
    private var imageHeight: Int = 0
    private var lipstickColor: Int = Color.MAGENTA
    private var blushColor: Int = Color.rgb(255, 105, 180)
    private var eyeshadowColor: Int = Color.BLUE
    private var eyebrowColor: Int = Color.DKGRAY

    private val paint = Paint().apply {
        color = Color.RED
        style = Paint.Style.FILL
        isAntiAlias = true
        alpha = 120
    }

    fun setLandmarks(landmarks: List<Pair<Float, Float>>) {
        this.landmarks = landmarks
        invalidate()
    }

    fun setMakeupType(type: MakeupType) {
        this.makeupType = type
        invalidate()
    }

    fun setImageInfo(width: Int, height: Int) {
        imageWidth = width
        imageHeight = height
        invalidate()
    }

    fun setMakeupColor(color: Int) {
        when (makeupType) {
            MakeupType.LIPSTICK -> lipstickColor = color
            MakeupType.BLUSH -> blushColor = color
            MakeupType.EYESHADOW -> eyeshadowColor = color
            MakeupType.EYEBROW -> eyebrowColor = color
        }
        invalidate()
    }

    private fun mapPointToView(x: Float, y: Float): Pair<Float, Float> {
        if (imageWidth == 0 || imageHeight == 0 || width == 0 || height == 0) return Pair(x, y)
        val viewAspect = width.toFloat() / height
        val imageAspect = imageWidth.toFloat() / imageHeight
        var drawWidth: Float
        var drawHeight: Float
        var offsetX = 0f
        var offsetY = 0f
        if (imageAspect > viewAspect) {
            // Image is wider than view
            drawWidth = width.toFloat()
            drawHeight = width.toFloat() / imageAspect
            offsetY = (height - drawHeight) / 2f
        } else {
            // Image is taller than view
            drawHeight = height.toFloat()
            drawWidth = height.toFloat() * imageAspect
            offsetX = (width - drawWidth) / 2f
        }
        val mappedX = x * (drawWidth / imageWidth) + offsetX
        val mappedY = y * (drawHeight / imageHeight) + offsetY
        return Pair(mappedX, mappedY)
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)
        landmarks?.let { points ->
            when (makeupType) {
                MakeupType.LIPSTICK -> drawLipstick(canvas, points)
                MakeupType.BLUSH -> drawBlush(canvas, points)
                MakeupType.EYESHADOW -> drawEyeshadow(canvas, points)
                MakeupType.EYEBROW -> drawEyebrow(canvas, points)
            }
        }
    }

    private fun drawLipstick(canvas: Canvas, points: List<Pair<Float, Float>>) {
        // MediaPipe FaceMesh lips landmark indices (outer and inner lips)
        val lipsIndices = listOf(
            61, 146, 91, 181, 84, 17, 314, 405, 321, 375, 291, 308, 324, 318, 402, 317, 14, 87, 178, 88, 95, 185, 40, 39, 37, 0, 267, 269, 270, 409, 415, 310, 311, 312, 13, 82, 81, 42, 183, 78
        )
        val lipPoints = lipsIndices.mapNotNull { idx -> points.getOrNull(idx) }
        val mappedLipPoints = lipPoints.map { (x, y) -> mapPointToView(x, y) }
        // Debug: Draw circles at each lip landmark
        val debugPaint = Paint().apply {
            color = Color.GREEN
            style = Paint.Style.FILL
            isAntiAlias = true
        }
        for ((x, y) in mappedLipPoints) {
            canvas.drawCircle(x, y, 6f, debugPaint)
        }
        // Draw the lips path (fill)
        val lipPath = Path()
        if (mappedLipPoints.isNotEmpty()) {
            lipPath.moveTo(mappedLipPoints[0].first, mappedLipPoints[0].second)
            for (pt in mappedLipPoints.drop(1)) {
                lipPath.lineTo(pt.first, pt.second)
            }
            lipPath.close()
            val fillPaint = Paint().apply {
                color = lipstickColor
                style = Paint.Style.FILL
                isAntiAlias = true
                alpha = 180
            }
            canvas.drawPath(lipPath, fillPaint)
        }
    }

    private fun drawBlush(canvas: Canvas, points: List<Pair<Float, Float>>) {
        // Approximate cheek positions using landmarks
        val leftCheekIdx = 205 // left cheek
        val rightCheekIdx = 425 // right cheek
        val cheekRadius = 40f
        points.getOrNull(leftCheekIdx)?.let { (x, y) ->
            val (mx, my) = mapPointToView(x, y)
            val blushPaint = Paint(paint).apply { color = blushColor; alpha = 80 }
            canvas.drawCircle(mx, my, cheekRadius, blushPaint)
        }
        points.getOrNull(rightCheekIdx)?.let { (x, y) ->
            val (mx, my) = mapPointToView(x, y)
            val blushPaint = Paint(paint).apply { color = blushColor; alpha = 80 }
            canvas.drawCircle(mx, my, cheekRadius, blushPaint)
        }
    }

    private fun drawEyeshadow(canvas: Canvas, points: List<Pair<Float, Float>>) {
        // MediaPipe FaceMesh left and right eye indices (upper eyelid)
        val leftEyeIndices = listOf(33, 246, 161, 160, 159, 158, 157, 173)
        val rightEyeIndices = listOf(362, 398, 384, 385, 386, 387, 388, 466)
        val eyePaint = Paint(paint).apply { color = eyeshadowColor; alpha = 70 }
        val leftEyePath = Path()
        val leftEyePoints = leftEyeIndices.mapNotNull { idx -> points.getOrNull(idx) }.map { (x, y) -> mapPointToView(x, y) }
        if (leftEyePoints.isNotEmpty()) {
            leftEyePath.moveTo(leftEyePoints[0].first, leftEyePoints[0].second)
            for (pt in leftEyePoints.drop(1)) {
                leftEyePath.lineTo(pt.first, pt.second)
            }
            leftEyePath.close()
            canvas.drawPath(leftEyePath, eyePaint)
        }
        val rightEyePath = Path()
        val rightEyePoints = rightEyeIndices.mapNotNull { idx -> points.getOrNull(idx) }.map { (x, y) -> mapPointToView(x, y) }
        if (rightEyePoints.isNotEmpty()) {
            rightEyePath.moveTo(rightEyePoints[0].first, rightEyePoints[0].second)
            for (pt in rightEyePoints.drop(1)) {
                rightEyePath.lineTo(pt.first, pt.second)
            }
            rightEyePath.close()
            canvas.drawPath(rightEyePath, eyePaint)
        }
    }

    private fun drawEyebrow(canvas: Canvas, points: List<Pair<Float, Float>>) {
        // Left eyebrow: 70-105, Right eyebrow: 336-365
        val leftIndices = 70..105
        val rightIndices = 336..365
        val paint = Paint().apply {
            color = eyebrowColor
            style = Paint.Style.FILL
            isAntiAlias = true
            alpha = 120
        }
        fun drawBrow(indices: IntRange) {
            val browPoints = indices.mapNotNull { points.getOrNull(it) }.map { (x, y) -> mapPointToView(x, y) }
            if (browPoints.size < 5) return
            val path = Path().apply {
                moveTo(browPoints[0].first, browPoints[0].second)
                for (pt in browPoints.drop(1)) {
                    lineTo(pt.first, pt.second)
                }
                close()
            }
            canvas.drawPath(path, paint)
        }
        drawBrow(leftIndices)
        drawBrow(rightIndices)
    }

    enum class MakeupType {
        LIPSTICK, BLUSH, EYESHADOW, EYEBROW
    }

} 