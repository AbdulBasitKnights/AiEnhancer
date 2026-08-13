package com.aiface.aging.features.editor

import android.view.View
import android.view.animation.AccelerateInterpolator
import androidx.appcompat.app.AppCompatActivity
import androidx.core.view.isVisible
import androidx.fragment.app.Fragment
import androidx.fragment.app.FragmentManager
import com.aiface.aging.R

object EditorBottomPanelHelper {

    private const val PANEL_TAG = "editor_bottom_panel"
    private const val BACK_STACK = "editor_bottom_panel"

    fun show(
        activity: AppCompatActivity,
        containerId: Int,
        container: View,
        fragment: Fragment,
        toolsView: View? = null,
    ) {
        if (activity.isFinishing || activity.supportFragmentManager.isDestroyed) return

        toolsView?.visibility = View.GONE
        container.visibility = View.VISIBLE

        val fragmentManager = activity.supportFragmentManager
        val existingPanel = fragmentManager.findFragmentByTag(PANEL_TAG)
        val transaction = fragmentManager.beginTransaction()

        if (existingPanel == null) {
            transaction.setCustomAnimations(
                R.anim.slide_in_bottom,
                R.anim.fade_out_panel,
                R.anim.slide_in_bottom,
                R.anim.slide_out_bottom,
            )
                .replace(containerId, fragment, PANEL_TAG)
                .addToBackStack(BACK_STACK)
        } else {
            transaction.setCustomAnimations(
                R.anim.fade_in_panel,
                R.anim.fade_out_panel,
            )
                .replace(containerId, fragment, PANEL_TAG)
        }
        transaction.commit()
    }

    fun hide(
        activity: AppCompatActivity,
        container: View,
        toolsView: View? = null,
        onHidden: (() -> Unit)? = null,
    ) {
        if (activity.isFinishing || activity.supportFragmentManager.isDestroyed) {
            onHidden?.invoke()
            return
        }

        if (!container.isVisible) {
            toolsView?.visibility = View.VISIBLE
            onHidden?.invoke()
            return
        }

        container.post {
            val slideDistance = container.height.takeIf { it > 0 }?.toFloat() ?: 320f
            container.animate()
                .translationY(slideDistance)
                .setDuration(220)
                .setInterpolator(AccelerateInterpolator())
                .withEndAction {
                    dismissPanel(activity, container, toolsView, onHidden)
                }
                .start()
        }
    }

    fun dismissImmediately(
        activity: AppCompatActivity,
        container: View,
        toolsView: View? = null,
        onHidden: (() -> Unit)? = null,
    ) {
        if (activity.isFinishing || activity.supportFragmentManager.isDestroyed) {
            onHidden?.invoke()
            return
        }
        container.animate().cancel()
        dismissPanel(activity, container, toolsView, onHidden)
    }

    private fun dismissPanel(
        activity: AppCompatActivity,
        container: View,
        toolsView: View?,
        onHidden: (() -> Unit)?,
    ) {
        try {
            val fragmentManager = activity.supportFragmentManager
            if (!fragmentManager.isDestroyed &&
                !fragmentManager.isStateSaved &&
                fragmentManager.backStackEntryCount > 0
            ) {
                fragmentManager.popBackStack(BACK_STACK, FragmentManager.POP_BACK_STACK_INCLUSIVE)
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }
        try {
            container.visibility = View.GONE
            container.translationY = 0f
            toolsView?.visibility = View.VISIBLE
            onHidden?.invoke()
        } catch (e: Exception) {
            e.printStackTrace()
        }
    }
}
