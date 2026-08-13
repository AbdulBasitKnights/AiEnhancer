package com.aiface.aging.utils

import android.content.Context
import android.graphics.Canvas
import android.graphics.Color
import android.graphics.Paint
import android.graphics.Path
import android.graphics.RectF
import android.util.AttributeSet
import android.view.View

class GridOverlay(context: Context, attrs: AttributeSet? = null)
    : View(context, attrs) {

    enum class GridType {
        NONE, RULE_OF_3, RULE_OF_4, PHI_GRID, GOLDEN_SPIRAL
    }

    var gridType = GridType.NONE
        set(value) {
            field = value
            invalidate()
        }

    private val paint = Paint().apply {
        color = Color.WHITE
        strokeWidth = 2f
        isAntiAlias = true
    }

    override fun onDraw(canvas: Canvas) {
        super.onDraw(canvas)

        when (gridType) {
            GridType.NONE -> return
            GridType.RULE_OF_3 -> drawRuleOfThirds(canvas)
            GridType.RULE_OF_4 -> draw4Grid(canvas)
            GridType.PHI_GRID -> drawPhi(canvas)
            GridType.GOLDEN_SPIRAL -> drawSpiral(canvas)
        }
    }

    private fun drawRuleOfThirds(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawLine(w / 3, 0f, w / 3, h, paint)
        canvas.drawLine(2 * w / 3, 0f, 2 * w / 3, h, paint)
        canvas.drawLine(0f, h / 3, w, h / 3, paint)
        canvas.drawLine(0f, 2 * h / 3, w, 2 * h / 3, paint)
    }

    private fun draw4Grid(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()

        canvas.drawLine(w / 4, 0f, w / 4, h, paint)
        canvas.drawLine(w / 2, 0f, w / 2, h, paint)
        canvas.drawLine(3 * w / 4, 0f, 3 * w / 4, h, paint)

        canvas.drawLine(0f, h / 4, w, h / 4, paint)
        canvas.drawLine(0f, h / 2, w, h / 2, paint)
        canvas.drawLine(0f, 3 * h / 4, w, 3 * h / 4, paint)
    }

    private fun drawPhi(canvas: Canvas) {
        val w = width.toFloat()
        val h = height.toFloat()
        val phi = 1.618f

        canvas.drawLine(w / phi, 0f, w / phi, h, paint)
        canvas.drawLine(0f, h / phi, w, h / phi, paint)
    }

    private fun drawSpiral(canvas: Canvas) {
        val rect = RectF(0f, 0f, width.toFloat(), height.toFloat())
        drawGoldenSpiral(rect, canvas)
    }

    private fun drawGoldenSpiral(rect: RectF, canvas: Canvas) {
        val path = Path()
        path.addArc(rect, 0f, 90f)
        canvas.drawPath(path, paint)
    }
}
