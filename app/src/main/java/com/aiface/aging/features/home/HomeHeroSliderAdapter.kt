package com.aiface.aging.features.home

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemHomeHeroPageBinding
import com.aiface.aging.shared.setSafeClickListener

class HomeHeroSliderAdapter(
    private val slides: List<HomeHeroSlideItem>,
    private val onSlideClick: (HomeHeroSlideItem) -> Unit,
) : RecyclerView.Adapter<HomeHeroSliderAdapter.Holder>() {

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int,
    ): Holder {
        val binding =
            ItemHomeHeroPageBinding.inflate(
                LayoutInflater.from(parent.context),
                parent,
                false,
            )
        return Holder(binding, onSlideClick)
    }

    override fun onBindViewHolder(holder: Holder, position: Int) {
        holder.bind(slides[position])
    }

    override fun getItemCount(): Int = slides.size

    class Holder(
        private val binding: ItemHomeHeroPageBinding,
        private val onSlideClick: (HomeHeroSlideItem) -> Unit,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(slide: HomeHeroSlideItem) {
            binding.ivHeroSlide.setImageResource(slide.backgroundImageRes)
            binding.tvHeroTitle.text = slide.title
            binding.tvHeroSubtitle.text = slide.description
            binding.tvHeroCta.text = slide.type
            val click = { onSlideClick(slide) }
            binding.root.setSafeClickListener { click() }
            binding.btnHeroCta.setSafeClickListener { click() }
        }
    }
}
