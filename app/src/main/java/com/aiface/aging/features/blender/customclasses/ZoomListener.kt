package com.aiface.aging.features.blender.customclasses

import android.annotation.SuppressLint
import android.graphics.Matrix
import android.graphics.PointF
import android.util.Log
import android.view.MotionEvent
import android.view.View
import android.widget.ImageView
import kotlin.math.abs
import kotlin.math.atan2
import kotlin.math.sqrt


@SuppressLint("ClickableViewAccessibility")
class ZoomListener(private val imageView: ImageView) : View.OnTouchListener {

    private val matrix = Matrix()
    private val savedMatrix = Matrix()
    private val start = PointF()
    private val mid = PointF()
    private var oldDist = 1f
    private var mode = NONE
    private var rotation = 0f
    private var maxRotation = 45f // Maximum rotation in degrees

    companion object {
        const val NONE = 0
        const val DRAG = 1
        const val ZOOM = 2
        const val ROTATE = 3
        const val MAX_ZOOM = 5f
        const val MIN_ZOOM = 0.5f
    }

    init {
        imageView.setOnTouchListener(this)
    }

    @SuppressLint("ClickableViewAccessibility")
    override fun onTouch(v: View?, event: MotionEvent?): Boolean {
        event?.let {
            imageView.scaleType = ImageView.ScaleType.MATRIX
            val scale: Float

            dumpEvent(it)

            when (it.action and MotionEvent.ACTION_MASK) {
                MotionEvent.ACTION_DOWN -> {
                    matrix.set(imageView.imageMatrix)
                    savedMatrix.set(matrix)
                    start.set(it.x, it.y)
                    mode = DRAG
                }

                MotionEvent.ACTION_UP, MotionEvent.ACTION_POINTER_UP -> {
                    mode = NONE

                }

                MotionEvent.ACTION_POINTER_DOWN -> {
                    oldDist = spacing(it)

                    if (oldDist > 5f) {
                        savedMatrix.set(matrix)
                        midPoint(mid, it)
                        mode = ZOOM
                    } else if (it.pointerCount == 2) {
                        mode = ROTATE
                    }
                }

                MotionEvent.ACTION_MOVE -> {
                    if (mode == DRAG) {
                        matrix.set(savedMatrix)
                        matrix.postTranslate(it.x - start.x, it.y - start.y)
                    } else if (mode == ZOOM) {
                        val newDist = spacing(it)
                        if (newDist > 5f) {
                            matrix.set(savedMatrix)
                            scale = newDist / oldDist
                            val newScale = matrixValues(matrix)[Matrix.MSCALE_X] * scale
                            if (newScale > MIN_ZOOM && newScale < MAX_ZOOM) {
                                matrix.postScale(scale, scale, mid.x, mid.y)
                            }
                        }
                    } else if (mode == ROTATE) {
                        val angle = rotationBetweenFingers(it)
                        rotation += angle
                        if (abs(rotation) <= maxRotation) {
                            matrix.set(savedMatrix)
                            matrix.postRotate(angle, mid.x, mid.y)
                        } else {
                            rotation -= angle // Undo rotation if it exceeds the limit
                        }
                    }
                }
            }

            imageView.imageMatrix = matrix
        }
        return true
    }

    private fun spacing(event: MotionEvent): Float {
        val x = event.getX(0) - event.getX(1)
        val y = event.getY(0) - event.getY(1)
        return sqrt((x * x + y * y).toDouble()).toFloat()
    }

    private fun midPoint(point: PointF, event: MotionEvent) {
        val x = event.getX(0) + event.getX(1)
        val y = event.getY(0) + event.getY(1)
        point.set(x / 2, y / 2)
    }

    private fun rotationBetweenFingers(event: MotionEvent): Float {
        val deltaX = (event.getX(0) - event.getX(1)).toDouble()
        val deltaY = (event.getY(0) - event.getY(1)).toDouble()
        val radians = atan2(deltaY, deltaX)
        return Math.toDegrees(radians).toFloat()
    }

    private fun dumpEvent(event: MotionEvent) {
        val names = arrayOf(
            "DOWN",
            "UP",
            "MOVE",
            "CANCEL",
            "OUTSIDE",
            "POINTER_DOWN",
            "POINTER_UP",
            "7?",
            "8?",
            "9?"
        )
        val sb = StringBuilder()
        val action = event.action
        val actionCode = action and MotionEvent.ACTION_MASK
        sb.append("event ACTION_").append(names[actionCode])

        if (actionCode == MotionEvent.ACTION_POINTER_DOWN || actionCode == MotionEvent.ACTION_POINTER_UP) {
            sb.append("(pid ").append(action shr MotionEvent.ACTION_POINTER_ID_SHIFT)
            sb.append(")")
        }

        sb.append("[")
        for (i in 0 until event.pointerCount) {
            sb.append("#").append(i)
            sb.append("(pid ").append(event.getPointerId(i))
            sb.append(")=").append(event.getX(i).toInt())
            sb.append(",").append(event.getY(i).toInt())
            if (i + 1 < event.pointerCount) sb.append(";")
        }

        sb.append("]")
        Log.d("Touch Events ---------", sb.toString())
    }

    private fun matrixValues(matrix: Matrix): FloatArray {
        val values = FloatArray(9)
        matrix.getValues(values)
        return values
    }
}