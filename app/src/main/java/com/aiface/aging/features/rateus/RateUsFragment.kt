package com.aiface.aging.features.rateus

import android.app.Dialog
import android.content.Context
import android.os.Bundle
import android.text.Editable
import android.text.TextWatcher
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.ViewTreeObserver
import android.view.animation.AlphaAnimation
import android.view.animation.Animation
import android.view.animation.AnimationSet
import android.view.animation.OvershootInterpolator
import android.view.animation.ScaleAnimation
import android.widget.FrameLayout
import android.widget.RatingBar
import android.widget.Toast
import androidx.appcompat.widget.AppCompatButton
import androidx.core.content.ContextCompat
import androidx.core.text.HtmlCompat
import androidx.fragment.app.FragmentActivity
import androidx.fragment.app.FragmentManager
import com.google.android.material.bottomsheet.BottomSheetDialog
import com.google.android.material.bottomsheet.BottomSheetDialogFragment
import com.google.android.material.textfield.TextInputEditText
import com.aiface.aging.R
import com.aiface.aging.databinding.FragmentRateusBinding
import com.aiface.aging.shared.ads.AppOpenManager
import com.aiface.aging.shared.rateUs
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.ToastUtils

class RateUsFragment : BottomSheetDialogFragment() {

    private var binding: FragmentRateusBinding? = null
    private var mActivity: FragmentActivity? = null
    private var ratingBarValue = 0.0f

    private var headingList: List<String> = emptyList()
    private var descriptionList: List<String> = emptyList()

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View? {
        binding = FragmentRateusBinding.inflate(inflater, container, false)
        dialog?.let { dialog -> binding?.root?.let { root -> makeBottomSheetRounded(root, dialog) } }

        mActivity?.let { context ->
            headingList = listOf(
                context.getString(R.string.ur_ideas),
                context.getString(R.string.unhappy_expe),
                context.getString(R.string.seeking_insights),
                context.getString(R.string.valueble_feedback),
                context.getString(R.string.elevate_performance),
                context.getString(R.string.thanks_for_stars),
            )
            descriptionList = listOf(
                context.getString(R.string.appreciate),
                context.getString(R.string.detailed_feedback),
                context.getString(R.string.help_enhance),
                context.getString(R.string.feedback_shapes),
                context.getString(R.string.help_maintain),
                context.getString(R.string.ongoing_support),
            )
        }
        return binding?.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        mActivity?.let {
            FirebaseLogUtils.firebaseUserAction("onViewCreated_RateUsFragment", "RateUsFragment")
            binding?.title?.text = headingList.getOrElse(0) { getString(R.string.ur_ideas) }
            binding?.textlayout?.text = HtmlCompat.fromHtml(
                getString(R.string.rate_us_prompt_html),
                HtmlCompat.FROM_HTML_MODE_LEGACY,
            )
            binding?.ratingBar?.let { animateStar() }
            binding?.ratingBar?.onRatingBarChangeListener =
                RatingBar.OnRatingBarChangeListener { _, rating, _ ->
                    ratingBarValue = rating
                    emojiAnimation()
                    checkEnableSubmitButton()
                    val drawableRes = when (ratingBarValue) {
                        in 0f..1f -> {
                            binding?.title?.text = headingList.getOrElse(1) { "" }
                            binding?.textlayout?.text = descriptionList.getOrElse(1) { "" }
                            R.drawable.sad
                        }
                        in 1f..2f -> {
                            binding?.title?.text = headingList.getOrElse(2) { "" }
                            binding?.textlayout?.text = descriptionList.getOrElse(2) { "" }
                            R.drawable.littlesad
                        }
                        in 2f..3f -> {
                            binding?.title?.text = headingList.getOrElse(3) { "" }
                            binding?.textlayout?.text = descriptionList.getOrElse(3) { "" }
                            R.drawable.normal
                        }
                        in 3f..4f -> {
                            binding?.title?.text = headingList.getOrElse(4) { "" }
                            binding?.textlayout?.text = descriptionList.getOrElse(4) { "" }
                            R.drawable.happy
                        }
                        else -> {
                            binding?.title?.text = headingList.getOrElse(5) { "" }
                            binding?.textlayout?.text = descriptionList.getOrElse(5) { "" }
                            R.drawable.excited
                        }
                    }
                    binding?.emoji?.setImageResource(drawableRes)
                }
        }
    }

    private fun checkEnableSubmitButton() {
        when {
            ratingBarValue > 0.0f && ratingBarValue < 5.0f -> {
                binding?.submitButton?.isEnabled = true
                binding?.submitButton?.text = getString(R.string.give_feedback)
                binding?.hintext?.visibility = View.INVISIBLE
                binding?.submitButton?.setOnClickListener { feedbackDialog() }
            }
            ratingBarValue > 4.0f -> {
                binding?.submitButton?.isEnabled = true
                binding?.hintext?.visibility = View.INVISIBLE
                binding?.submitButton?.text = getString(R.string.rate_on_google_play)
                binding?.submitButton?.setOnClickListener {
                    AppOpenManager.disableAppOpen = true
                    mActivity?.rateUs()
                    dismissAllowingStateLoss()
                }
            }
            else -> {
                ToastUtils.showToast(
                    requireContext(),
                    getString(R.string.please_select_at_least_one_star),
                )
                binding?.submitButton?.text = getString(R.string.rate_on_google_play)
                binding?.submitButton?.isEnabled = false
            }
        }
    }

    private fun emojiAnimation() {
        val animationSet = AnimationSet(true)
        val scaleAnimation = ScaleAnimation(
            0.5f, 1.0f, 0.5f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
        )
        scaleAnimation.duration = 200
        scaleAnimation.interpolator = OvershootInterpolator()
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 200
        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(alphaAnimation)
        binding?.emoji?.startAnimation(animationSet)
    }

    private fun feedbackDialog() {
        val dialog = Dialog(requireContext())
        dialog.setContentView(R.layout.feedbackdialog)
        val window = dialog.window
        window?.setBackgroundDrawableResource(android.R.color.transparent)
        window?.decorView?.setBackgroundResource(R.drawable.feedbackcorner)
        val editText = dialog.findViewById<TextInputEditText>(R.id.edit_query)
        val submit = dialog.findViewById<AppCompatButton>(R.id.submit)
        submit.isEnabled = false
        submit.background = ContextCompat.getDrawable(requireContext(), R.drawable.exit_btn_rate)
        submit.setTextColor(ContextCompat.getColor(requireContext(), R.color.gSelector_light))
        val exit = dialog.findViewById<AppCompatButton>(R.id.exit)
        exit.setOnClickListener { dialog.dismiss() }

        editText.addTextChangedListener(object : TextWatcher {
            override fun beforeTextChanged(s: CharSequence?, start: Int, count: Int, after: Int) = Unit

            override fun onTextChanged(s: CharSequence?, start: Int, before: Int, count: Int) {
                if (s.isNullOrEmpty()) {
                    submit.isEnabled = false
                    submit.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.exit_btn_rate)
                    submit.setTextColor(
                        ContextCompat.getColor(requireContext(), R.color.gSelector_light),
                    )
                } else {
                    submit.isEnabled = true
                    submit.background =
                        ContextCompat.getDrawable(requireContext(), R.drawable.cancle_btn_rate)
                    submit.setTextColor(ContextCompat.getColor(requireContext(), R.color.white))
                }
            }

            override fun afterTextChanged(s: Editable?) = Unit
        })

        submit.setOnClickListener {
            if (submit.isEnabled) {
                Toast.makeText(requireContext(), getString(R.string.submitted), Toast.LENGTH_SHORT)
                    .show()
                dialog.dismiss()
                dismissAllowingStateLoss()
            }
        }
        dialog.show()
    }

    private fun animateStar() {
        val animationSet = AnimationSet(true)
        val scaleAnimation = ScaleAnimation(
            0.0f, 1.0f, 0.0f, 1.0f,
            Animation.RELATIVE_TO_SELF, 0.5f,
            Animation.RELATIVE_TO_SELF, 0.5f,
        )
        scaleAnimation.duration = 500
        scaleAnimation.interpolator = OvershootInterpolator()
        val alphaAnimation = AlphaAnimation(0.0f, 1.0f)
        alphaAnimation.duration = 500
        animationSet.addAnimation(scaleAnimation)
        animationSet.addAnimation(alphaAnimation)
        binding?.ratingBar?.startAnimation(animationSet)
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        mActivity = requireActivity()
    }

    override fun onDetach() {
        super.onDetach()
        mActivity = null
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    private fun makeBottomSheetRounded(view: View, dialog: Dialog) {
        view.viewTreeObserver.addOnGlobalLayoutListener(object :
            ViewTreeObserver.OnGlobalLayoutListener {
            override fun onGlobalLayout() {
                view.viewTreeObserver.removeOnGlobalLayoutListener(this)
                val bottomSheet = (dialog as? BottomSheetDialog)
                    ?.findViewById<View>(com.google.android.material.R.id.design_bottom_sheet)
                        as? FrameLayout
                bottomSheet?.setBackgroundResource(R.drawable.bg_rounded_cardview_transparent)
            }
        })
    }

    companion object {
        const val TAG = "RateUsFragment"

        fun show(fragmentManager: FragmentManager) {
            if (fragmentManager.findFragmentByTag(TAG) != null) return
            RateUsFragment().show(fragmentManager, TAG)
        }
    }
}
