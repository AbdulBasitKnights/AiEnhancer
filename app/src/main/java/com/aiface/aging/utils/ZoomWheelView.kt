package com.aiface.aging.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.util.AttributeSet
import android.view.MotionEvent
import android.view.View

class ZoomWheelView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null
) : View(context, attrs) {

    private val minZoom = 1f
    private val maxZoom = 5f

    private val totalSteps = 40
    private val tickSpacing = 30f

    private var scrollOffset = 0f
    private var currentStep = 0

    private var lastX = 0f

    private val normalPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.GRAY
        strokeWidth = 4f
    }

    private val activePaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.parseColor("#7C4DFF")
        strokeWidth = 6f
    }

    private val centerPaint = Paint(Paint.ANTI_ALIAS_FLAG).apply {
        color = Color.WHITE
        strokeWidth = 6f
    }

    var onZoomChanged: ((Float) -> Unit)? = null

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        val centerX = width / 2f
        val centerY = height / 2f

        for (i in -totalSteps..totalSteps) {

            val x = centerX + (i * tickSpacing) + scrollOffset

            val stepIndex = -i
            val paint = if (stepIndex in 0 until currentStep)
                activePaint
            else
                normalPaint

            val heightLine = if (i % 5 == 0) 60f else 35f

            canvas.drawLine(
                x,
                centerY - heightLine / 2,
                x,
                centerY + heightLine / 2,
                paint
            )
        }

        // center indicator
        canvas.drawLine(
            centerX,
            0f,
            centerX,
            height.toFloat(),
            centerPaint
        )
    }

    override fun onTouchEvent(event: MotionEvent): Boolean {
        when (event.action) {

            MotionEvent.ACTION_DOWN -> {
                lastX = event.x
                return true
            }

            MotionEvent.ACTION_MOVE -> {
                val dx = event.x - lastX
                lastX = event.x

                scrollOffset += dx

                val newStep = (-scrollOffset / tickSpacing).toInt()
                    .coerceIn(0, totalSteps)

                if (newStep != currentStep) {
                    currentStep = newStep

                    val zoom = calculateZoom()
                    onZoomChanged?.invoke(zoom)
                }

                // limit scroll range
                scrollOffset = scrollOffset.coerceIn(
                    -totalSteps * tickSpacing,
                    0f
                )

                invalidate()
            }
        }
        return super.onTouchEvent(event)
    }

    private fun calculateZoom(): Float {
        val percent = currentStep.toFloat() / totalSteps
        return minZoom + percent * (maxZoom - minZoom)
    }
}