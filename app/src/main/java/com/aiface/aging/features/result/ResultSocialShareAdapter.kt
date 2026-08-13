package com.aiface.aging.features.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.annotation.DrawableRes
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemResultSocialShareBinding
import com.aiface.aging.shared.setSafeClickListener

data class ResultSocialShareOption(
    val id: Int,
    @DrawableRes val iconRes: Int,
    val title: String,
    val packageName: String?,
)

class ResultSocialShareAdapter(
    private val onClick: (ResultSocialShareOption) -> Unit,
) : RecyclerView.Adapter<ResultSocialShareAdapter.Holder>() {

    private val items = mutableListOf<ResultSocialShareOption>()

    fun submit(list: List<ResultSocialShareOption>) {
        items.clear()
        items.addAll(list)
        notifyDataSetChanged()
    }

    override fun getItemCount(): Int = items.size

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): Holder {
        val binding = ItemResultSocialShareBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return Holder(binding)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(items[position])
    }

    inner class Holder(
        private val binding: ItemResultSocialShareBinding,
    ) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: ResultSocialShareOption) {
            binding.ivSocialIcon.setImageResource(item.iconRes)
            binding.tvSocialTitle.text = item.title
            binding.root.setSafeClickListener { onClick(item) }
        }
    }
}
