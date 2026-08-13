package com.aiface.aging.features.result

import android.app.Dialog
import android.os.Bundle
import android.util.Log
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.widget.FrameLayout
import android.widget.Toast
import com.bumptech.glide.Glide
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.aiface.aging.R
import com.aiface.aging.databinding.BottomsheetReportBinding
import com.aiface.aging.utils.FirebaseLogUtils

class ReportBottomsheetFragment : BottomSheetDialogFragment() {

    private var binding: BottomsheetReportBinding? = null
    private var selectedOption = 1

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = BottomsheetReportBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val b = binding ?: return

        selectOption(1)
        b.ll1.setOnClickListener { selectOption(1) }
        b.ll2.setOnClickListener { selectOption(2) }
        b.ll3.setOnClickListener { selectOption(3) }
        b.selector1.setOnClickListener { selectOption(1) }
        b.selector2.setOnClickListener { selectOption(2) }
        b.selector3.setOnClickListener { selectOption(3) }

        b.btnSubmit.setOnClickListener {
            val event = when (selectedOption) {
                2 -> "report_not_as_expected"
                3 -> "report_inappropriate_content"
                else -> "report_like_it"
            }
            val jobId = arguments?.getString(ARG_JOB_ID).orEmpty()
            val imageUrl = arguments?.getString(ARG_IMAGE_URL).orEmpty()
            FirebaseLogUtils.firebaseUserAction(
                action = event,
                activityName = "ResultFragment",
                extraParams = buildMap {
                    if (jobId.isNotBlank()) put("job_id", jobId.take(100))
                    if (imageUrl.isNotBlank()) put("image_url", imageUrl.take(100))
                    put("option", selectedOption.toString())
                },
            )
            FirebaseLogUtils.logEvent(event, "User gave feedback")
            Log.d(TAG, "reported event=$event jobId=$jobId")
            Toast.makeText(requireContext(), R.string.thanks_for_feedback, Toast.LENGTH_SHORT).show()
            dismissAllowingStateLoss()
        }

        dialog?.let { dg -> makeBottomSheetRounded(b.root, dg) }
    }

    private fun selectOption(option: Int) {
        selectedOption = option
        val b = binding ?: return
        val ctx = context ?: return
        Glide.with(ctx).load(if (option == 1) R.drawable.ic_radio_selected else R.drawable.ic_radio_unselected)
            .into(b.selector1)
        Glide.with(ctx).load(if (option == 2) R.drawable.ic_radio_selected else R.drawable.ic_radio_unselected)
            .into(b.selector2)
        Glide.with(ctx).load(if (option == 3) R.drawable.ic_radio_selected else R.drawable.ic_radio_unselected)
            .into(b.selector3)
    }

    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val bottomSheet =
                    (dialog as? BottomSheetDialog)
                        ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                        as? FrameLayout
                bottomSheet?.setBackgroundResource(R.drawable.report_bottomsheet_rounded)
            }
        })
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        private const val TAG = "ReportBottomSheet"
        private const val ARG_JOB_ID = "job_id"
        private const val ARG_IMAGE_URL = "image_url"

        fun newInstance(jobId: String? = null, imageUrl: String? = null): ReportBottomsheetFragment {
            return ReportBottomsheetFragment().apply {
                arguments = Bundle().apply {
                    putString(ARG_JOB_ID, jobId)
                    putString(ARG_IMAGE_URL, imageUrl)
                }
            }
        }
    }
}
