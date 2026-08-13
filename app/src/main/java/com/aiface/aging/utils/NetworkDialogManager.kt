package com.aiface.aging.utils

import android.app.Activity
import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.os.Handler
import android.os.Looper
import android.provider.Settings
import android.util.Log
import android.view.LayoutInflater
import android.view.Window
import android.widget.TextView
import androidx.core.graphics.drawable.toDrawable
import com.aiface.aging.R

object NetworkDialogManager {

    private var dialog: Dialog? = null
    private val mainHandler = Handler(Looper.getMainLooper())

    fun showNoInternetDialog(activity: Activity) {
        mainHandler.post {
            Log.d("networkChecker", "showNoInternetDialog() called")

            if (activity.isFinishing || activity.isDestroyed) {
                Log.d("networkChecker", "Dialog not shown: activity invalid")
                return@post
            }

            if (dialog?.isShowing == true) {
                Log.d("networkChecker", "Dialog already showing")
                return@post
            }

            try {
                dismissDialogInternal()

                val view = LayoutInflater.from(activity)
                    .inflate(R.layout.dialog_no_internet, null, false)

                val btnRetry = view.findViewById<TextView>(R.id.btnTryAgain)

                val customDialog = Dialog(activity)
                customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
                customDialog.setContentView(view)
                customDialog.setCancelable(false)
                customDialog.window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
                customDialog.window?.setLayout(
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT,
                    android.view.ViewGroup.LayoutParams.MATCH_PARENT
                )

                btnRetry.setOnClickListener {
                  //  restartApp(activity)

                    val intent = Intent(Settings.ACTION_WIFI_SETTINGS)
                    activity.startActivity(intent)

                    dismissDialog()
                }

                dialog = customDialog
                dialog?.show()

                Log.d("networkChecker", "Custom dialog show() executed")
            } catch (e: Exception) {
                Log.e("networkChecker", "Dialog show failed: ${e.message}", e)
            }
        }
    }

    fun dismissDialog() {
        mainHandler.post {
            dismissDialogInternal()
        }
    }

    private fun dismissDialogInternal() {
        try {
            dialog?.dismiss()
        } catch (_: Exception) {
        }
        dialog = null
    }

    fun restartApp(activity: Activity) {
        val intent = activity.packageManager
            .getLaunchIntentForPackage(activity.packageName)

        intent?.addFlags(
            Intent.FLAG_ACTIVITY_CLEAR_TOP or
                    Intent.FLAG_ACTIVITY_CLEAR_TASK or
                    Intent.FLAG_ACTIVITY_NEW_TASK
        )

        activity.startActivity(intent)
        activity.finish()
        Runtime.getRuntime().exit(0)
    }
}