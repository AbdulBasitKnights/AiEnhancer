package com.aiface.aging.features.credits

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.view.isVisible
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.databinding.ItemCheckinDayBinding
import com.aiface.aging.databinding.ItemCheckinDayBonusBinding
import com.aiface.aging.domain.model.CheckInDayStatus
import com.aiface.aging.domain.model.CheckInDayUi

class CheckInDaysAdapter :
    ListAdapter<CheckInDayUi, RecyclerView.ViewHolder>(Diff) {

    override fun getItemViewType(position: Int): Int {
        return if (getItem(position).isBonusDay) VIEW_BONUS else VIEW_DAY
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        val inflater = LayoutInflater.from(parent.context)
        return when (viewType) {
            VIEW_BONUS -> BonusViewHolder(
                ItemCheckinDayBonusBinding.inflate(inflater, parent, false),
            )
            else -> DayViewHolder(
                ItemCheckinDayBinding.inflate(inflater, parent, false),
            )
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {
            is DayViewHolder -> holder.bind(getItem(position))
            is BonusViewHolder -> holder.bind(getItem(position))
        }
    }

    private class DayViewHolder(
        private val binding: ItemCheckinDayBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CheckInDayUi) {
            val context = binding.root.context
            binding.tvDayLabel.text = context.getString(R.string.daily_checkin_day_label, item.dayNumber)
            binding.ivCheck.isVisible = item.showCheckmark
            binding.ivCoin.isVisible = !item.showCheckmark

            val backgroundRes = when (item.status) {
                CheckInDayStatus.CURRENT -> R.drawable.bg_checkin_day_current
                CheckInDayStatus.CLAIMED -> R.drawable.bg_checkin_day_claimed
                CheckInDayStatus.LOCKED -> R.drawable.bg_checkin_day_locked
            }
            binding.dayCell.setBackgroundResource(backgroundRes)

            val badgeBackground = if (item.showCheckmark) {
                R.drawable.bg_checkin_badge_claimed
            } else {
                R.drawable.bg_checkin_badge_coin
            }
            binding.badgeContainer.setBackgroundResource(badgeBackground)
        }
    }

    private class BonusViewHolder(
        private val binding: ItemCheckinDayBonusBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: CheckInDayUi) {
            val context = binding.root.context
            binding.tvDayLabel.text = context.getString(R.string.daily_checkin_day_label, item.dayNumber)
            binding.groupCoins.isVisible = !item.showCheckmark
            binding.ivBonusCheck.isVisible = item.showCheckmark
            val backgroundRes = when (item.status) {
                CheckInDayStatus.CURRENT -> R.drawable.bg_checkin_day_current_wide
                CheckInDayStatus.CLAIMED -> R.drawable.bg_checkin_day_claimed_wide
                CheckInDayStatus.LOCKED -> R.drawable.bg_checkin_day_locked_wide
            }
            binding.bonusCell.setBackgroundResource(backgroundRes)
        }
    }

    private object Diff : DiffUtil.ItemCallback<CheckInDayUi>() {
        override fun areItemsTheSame(oldItem: CheckInDayUi, newItem: CheckInDayUi): Boolean =
            oldItem.dayNumber == newItem.dayNumber

        override fun areContentsTheSame(oldItem: CheckInDayUi, newItem: CheckInDayUi): Boolean =
            oldItem == newItem
    }

    companion object {
        private const val VIEW_DAY = 0
        private const val VIEW_BONUS = 1
    }
}
