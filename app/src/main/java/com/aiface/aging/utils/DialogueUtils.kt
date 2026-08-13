package com.aiface.aging.utils

import android.app.Dialog
import android.content.Context
import android.graphics.Color
import android.graphics.drawable.ColorDrawable
import android.view.View
import android.view.Window
import androidx.constraintlayout.widget.ConstraintLayout
import com.kaopiz.kprogresshud.KProgressHUD


object DialogueUtils {
    fun getDialogue(context: Context, layout: Int): Dialog {
        val dialog = Dialog(context)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.setCancelable(false)
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(layout)
        return dialog
    }

    fun getCancelableDialogue(context: Context, layout: Int): Dialog {
        val dialog = Dialog(context)
        dialog.window!!.setBackgroundDrawable(ColorDrawable(Color.TRANSPARENT))
        dialog.requestWindowFeature(Window.FEATURE_NO_TITLE)
        dialog.setContentView(layout)
        return dialog
    }

    fun getWaitDialogue(label: String, context: Context): KProgressHUD {
        return KProgressHUD.create(context)
            .setStyle(KProgressHUD.Style.SPIN_INDETERMINATE)
            .setLabel(label)
            .setCancellable(false)
            .setAnimationSpeed(2)
            .setDimAmount(0.5f)
    }

    fun setRewardButtonVisibility(goPremiumButton : ConstraintLayout?, watchButton : ConstraintLayout?){
        try {
            goPremiumButton?.visibility = View.VISIBLE
            watchButton?.visibility = View.VISIBLE
//            when(configValue){
//                0->{
//                    goPremiumButton?.visibility = View.GONE
//                    watchButton?.visibility = View.VISIBLE
//                }
//                1->{
//                    goPremiumButton?.visibility = View.VISIBLE
//                    watchButton?.visibility = View.GONE
//                }
//                2->{
//                    goPremiumButton?.visibility = View.VISIBLE
//                    watchButton?.visibility = View.VISIBLE
//                }
//            }
        }catch (e : Exception){
            e.printStackTrace()
        }
    }
}