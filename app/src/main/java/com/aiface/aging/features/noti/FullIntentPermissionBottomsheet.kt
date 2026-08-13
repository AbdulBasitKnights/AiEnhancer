package com.aiface.aging.features.noti

import android.app.Dialog
import android.content.DialogInterface
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.aiface.aging.R
import com.aiface.aging.databinding.BottomsheetPostNotiBinding


class FullIntentPermissionBottomsheet : BottomSheetDialogFragment() {

    private var binding : BottomsheetPostNotiBinding? = null
    var onDismissListener: (() -> Unit)? = null

    override fun onDismiss(dialog: DialogInterface) {
        super.onDismiss(dialog)
        onDismissListener?.invoke()  // Notify listener when dismissed
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?
    ): View? {
        binding = BottomsheetPostNotiBinding.inflate(inflater,container,false)

        binding?.btnAllow?.setOnClickListener {
            dismiss()
            listener?.onPermissionBtnClick("allow")
        }

        binding?.btnDontAllow?.setOnClickListener {
            dismiss()
            listener?.onPermissionBtnClick("not")
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        dialog?.let {dg->
            binding?.root?.let {rootLayout->
                makeBottomSheetRounded(rootLayout, dg)
            }
        }
    }

    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val dg = dialog as BottomSheetDialog?
                val bottomSheet = dg?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet) as FrameLayout?
                bottomSheet?.setBackgroundResource(R.drawable.bg_bottomsheet_rounded)
            }
        })
    }

    interface IntentPermissionListener{
        fun onPermissionBtnClick(prompt : String)
    }

    fun setPromptBtnListener(listener : IntentPermissionListener){
        this.listener = listener
    }
    private var listener : IntentPermissionListener? = null
}