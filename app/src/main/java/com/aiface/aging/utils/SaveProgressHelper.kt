package com.aiface.aging.utils

import android.app.Activity
import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.Window
import android.view.animation.AnimationUtils
import android.widget.ImageView
import android.widget.TextView
import androidx.annotation.StringRes
import com.aiface.aging.R

/**
 * Shared processing/saving loader (dim overlay + card + rotating ring).
 * Use overlay API when layout includes [R.layout.layout_save_progress_overlay],
 * or Activity dialog API for editors without an embedded overlay.
 */
object SaveProgressHelper {

    private val mainHandler = Handler(Looper.getMainLooper())
    private var dialog: Dialog? = null

    fun show(overlay: View, context: Context, @StringRes messageRes: Int = R.string.saving) {
        overlay.visibility = View.VISIBLE
        overlay.findViewById<TextView>(R.id.tvSaveProgress)?.setText(messageRes)
        overlay.findViewById<ImageView>(R.id.saveProgressImage)?.startAnimation(
            AnimationUtils.loadAnimation(context, R.anim.rotate),
        )
    }

    fun hide(overlay: View?) {
        overlay ?: return
        overlay.findViewById<ImageView>(R.id.saveProgressImage)?.clearAnimation()
        overlay.visibility = View.GONE
    }

    @JvmStatic
    @JvmOverloads
    fun show(activity: Activity, @StringRes messageRes: Int = R.string.saving) {
        runOnMain {
            if (activity.isFinishing || activity.isDestroyed) return@runOnMain
            dismissDialog()
            val content = LayoutInflater.from(activity)
                .inflate(R.layout.layout_save_progress_overlay, null, false)
            content.visibility = View.VISIBLE
            content.findViewById<TextView>(R.id.tvSaveProgress)?.setText(messageRes)
            content.findViewById<ImageView>(R.id.saveProgressImage)?.startAnimation(
                AnimationUtils.loadAnimation(activity, R.anim.rotate),
            )
            val d = Dialog(activity).apply {
                requestWindowFeature(Window.FEATURE_NO_TITLE)
                setCancelable(false)
                setContentView(content)
                window?.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
                window?.setLayout(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.MATCH_PARENT,
                )
            }
            dialog = d
            try {
                d.show()
            } catch (_: Exception) {
                dialog = null
            }
        }
    }

    /** Show "Processing…" while Next caches the image before interstitial / Preview. */
    @JvmStatic
    fun showProcessing(activity: Activity) {
        show(activity, R.string.processing)
    }

    @JvmStatic
    fun hide(activity: Activity?) {
        runOnMain {
            dismissDialog()
        }
    }

    private fun dismissDialog() {
        try {
            dialog?.findViewById<ImageView>(R.id.saveProgressImage)?.clearAnimation()
            if (dialog?.isShowing == true) {
                dialog?.dismiss()
            }
        } catch (_: Exception) {
        } finally {
            dialog = null
        }
    }

    private fun runOnMain(block: () -> Unit) {
        if (Looper.myLooper() == Looper.getMainLooper()) {
            block()
        } else {
            mainHandler.post(block)
        }
    }
}
