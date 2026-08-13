package com.aiface.aging.features.home.carousel

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemHomePromoBannerBinding
import com.aiface.aging.features.home.HomeItem
import com.aiface.aging.shared.setSafeClickListener

class HomePromoBannerViewHolder(
    private val binding: ItemHomePromoBannerBinding,
    private val onCtaClick: () -> Unit,
) : RecyclerView.ViewHolder(binding.root) {

    fun bind(item: HomeItem.PromoBanner) {
        binding.ivPromoBg.setImageResource(item.imageRes)
        binding.tvPromoTitle.setText(item.titleRes)
        binding.tvPromoSubtitle.setText(item.subtitleRes)
        binding.btnPromoCta.setText(item.ctaRes)
        binding.btnPromoCta.setSafeClickListener { onCtaClick() }
        binding.root.setSafeClickListener { onCtaClick() }
    }

    companion object {
        fun create(
            parent: ViewGroup,
            onCtaClick: () -> Unit,
        ): HomePromoBannerViewHolder {
            val binding = ItemHomePromoBannerBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
            return HomePromoBannerViewHolder(binding, onCtaClick)
        }
    }
}
