package com.aiface.aging.utils

import android.animation.ValueAnimator
import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.view.animation.DecelerateInterpolator
import android.widget.FrameLayout
import com.aiface.aging.R

class IosToggleView @JvmOverloads constructor(
    context: Context,
    attrs: AttributeSet? = null,
    defStyleAttr: Int = 0,
) : FrameLayout(context, attrs, defStyleAttr) {

    private val trackView: View
    private val thumbView: View

    private var checked = false
    private var thumbTravelPx = 0
    private var checkedChangeListener: ((IosToggleView, Boolean) -> Unit)? = null
    private var suppressListener = false

    init {
        LayoutInflater.from(context).inflate(R.layout.view_ios_toggle, this, true)
        trackView = findViewById(R.id.toggleTrack)
        thumbView = findViewById(R.id.toggleThumb)
        trackView.isClickable = false
        thumbView.isClickable = false
        isClickable = true
        isFocusable = true
        setOnClickListener { toggle(animate = true) }
        post { applyCheckedState(animate = false, notifyListener = false) }
    }

    override fun onInterceptTouchEvent(ev: android.view.MotionEvent): Boolean = true

    override fun onTouchEvent(event: android.view.MotionEvent): Boolean {
        if (event.action == android.view.MotionEvent.ACTION_UP) {
            performClick()
        }
        return true
    }

    override fun onSizeChanged(w: Int, h: Int, oldw: Int, oldh: Int) {
        super.onSizeChanged(w, h, oldw, oldh)
        val thumbWidth = thumbView.layoutParams.width.takeIf { it > 0 } ?: thumbView.width
        val startMargin = (thumbView.layoutParams as? MarginLayoutParams)?.marginStart ?: 0
        thumbTravelPx = (width - thumbWidth - (startMargin * 2)).coerceAtLeast(0)
        applyCheckedState(animate = false, notifyListener = false)
    }

    var isChecked: Boolean
        get() = checked
        set(value) {
            setChecked(value, animate = false)
        }

    fun setChecked(value: Boolean, animate: Boolean) {
        if (checked == value) {
            applyCheckedState(animate = false, notifyListener = false)
            return
        }
        checked = value
        applyCheckedState(animate = animate, notifyListener = false)
    }

    fun toggle(animate: Boolean = true) {
        checked = !checked
        applyCheckedState(animate = animate, notifyListener = true)
    }

    fun setOnCheckedChangeListener(listener: ((IosToggleView, Boolean) -> Unit)?) {
        checkedChangeListener = listener
    }

    private fun applyCheckedState(animate: Boolean, notifyListener: Boolean) {
        trackView.setBackgroundResource(
            if (checked) R.drawable.bg_ios_toggle_track_on else R.drawable.bg_ios_toggle_track_off,
        )

        val targetX = if (checked) thumbTravelPx.toFloat() else 0f
        thumbView.animate().cancel()

        if (animate) {
            ValueAnimator.ofFloat(thumbView.translationX, targetX).apply {
                duration = 220L
                interpolator = DecelerateInterpolator()
                addUpdateListener { animator ->
                    thumbView.translationX = animator.animatedValue as Float
                }
                start()
            }
        } else {
            thumbView.translationX = targetX
        }

        if (notifyListener && !suppressListener) {
            checkedChangeListener?.invoke(this, checked)
        }
    }

    fun setCheckedSilently(value: Boolean, animate: Boolean = false) {
        suppressListener = true
        setChecked(value, animate)
        suppressListener = false
    }
}
