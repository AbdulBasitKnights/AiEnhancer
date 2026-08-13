package com.aiface.aging.features.credits

import android.app.Dialog
import android.content.Intent
import android.graphics.Color
import android.os.Bundle
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.view.WindowManager
import androidx.core.content.ContextCompat
import androidx.core.graphics.drawable.toDrawable
import androidx.core.view.isVisible
import androidx.fragment.app.DialogFragment
import androidx.fragment.app.activityViewModels
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import androidx.recyclerview.widget.GridLayoutManager
import com.aiface.aging.R
import com.aiface.aging.databinding.DialogDailyCheckinBinding
import com.aiface.aging.domain.model.DailyCheckInUiState
import com.aiface.aging.features.iap.IAPActivity
import com.aiface.aging.utils.FirebaseLogUtils
import com.aiface.aging.utils.ToastUtils
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch

@AndroidEntryPoint
class DailyCheckInDialogFragment : DialogFragment() {

    private var binding: DialogDailyCheckinBinding? = null
    private val viewModel: DailyCheckInViewModel by activityViewModels()
    private val daysAdapter = CheckInDaysAdapter()

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        return super.onCreateDialog(savedInstanceState).apply {
            window?.setBackgroundDrawable(Color.TRANSPARENT.toDrawable())
        }
    }

    override fun onStart() {
        super.onStart()
        dialog?.window?.setLayout(
            WindowManager.LayoutParams.MATCH_PARENT,
            WindowManager.LayoutParams.WRAP_CONTENT,
        )
    }

    override fun onCreateView(
        inflater: LayoutInflater,
        container: ViewGroup?,
        savedInstanceState: Bundle?,
    ): View {
        binding = DialogDailyCheckinBinding.inflate(inflater, container, false)
        return binding!!.root
    }

    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)
        val binding = binding ?: return

        binding.btnClose.setOnClickListener { dismissAllowingStateLoss() }
        binding.rvCheckInDays.apply {
            layoutManager = GridLayoutManager(requireContext(), 3).apply {
                spanSizeLookup = object : GridLayoutManager.SpanSizeLookup() {
                    override fun getSpanSize(position: Int): Int {
                        return if (daysAdapter.currentList.getOrNull(position)?.isBonusDay == true) 3 else 1
                    }
                }
            }
            adapter = daysAdapter
            itemAnimator = null
        }

        binding.btnClaim.setOnClickListener {
            FirebaseLogUtils.logEvent("daily_checkin_claim_click", "")
            viewModel.claimToday(
                onResult = {
                    FirebaseLogUtils.logEvent("daily_checkin_claim_success", it.toString())
                    renderClaimedState()
                },
                onError = {
                    ToastUtils.showErrorToast(requireContext())
                },
            )
        }

        binding.tvGetMoreCoins.setOnClickListener {
            FirebaseLogUtils.logEvent("daily_checkin_get_more_click", "")
            startActivity(Intent(requireContext(), IAPActivity::class.java))
        }

        viewLifecycleOwner.lifecycleScope.launch {
            viewLifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiState.collect { state ->
                    renderState(state)
                }
            }
        }

        viewModel.markDialogShown()
        viewModel.refresh()
    }

    private fun renderState(state: DailyCheckInUiState) {
        val binding = binding ?: return
        binding.tvStreakDays.text = getString(R.string.daily_checkin_streak_days, state.streakDays)
        daysAdapter.submitList(state.days)
        binding.btnClaim.isEnabled = state.canClaim
        binding.btnClaim.alpha = if (state.canClaim) 1f else 0.72f
        binding.btnClaim.text = if (state.canClaim) {
            getString(R.string.daily_checkin_claim)
        } else {
            getString(R.string.daily_checkin_claimed)
        }
    }

    private fun renderClaimedState() {
        val binding = binding ?: return
        binding.btnClaim.isEnabled = false
        binding.btnClaim.alpha = 0.72f
        binding.btnClaim.text = getString(R.string.daily_checkin_claimed)
    }

    override fun onDestroyView() {
        binding = null
        super.onDestroyView()
    }

    companion object {
        const val TAG = "DailyCheckInDialog"

        fun newInstance(): DailyCheckInDialogFragment = DailyCheckInDialogFragment()
    }
}
