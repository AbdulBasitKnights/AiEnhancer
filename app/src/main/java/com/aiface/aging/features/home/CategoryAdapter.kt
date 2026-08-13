package com.aiface.aging.features.home

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.fragment.app.FragmentActivity
import androidx.recyclerview.widget.RecyclerView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.shared.ads.HomeChildItem
import com.aiface.aging.shared.ads.HomeNativeAdManager
import com.aiface.aging.databinding.ItemHomeAiChildBinding
import com.aiface.aging.databinding.ItemNativeAdBinding
import com.aiface.aging.domain.model.Template
import com.aiface.aging.shared.TemplateThumbLoader
import com.aiface.aging.shared.setSafeClickListener

class CategoryAdapter(
    private val onTemplateClick: (Template, String, String) -> Unit,
    private val categoryName: String,
    private val categoryId: String = "",
    private val context: FragmentActivity
) : RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    companion object {
        private const val VIEW_TYPE_ITEM = 0
        private const val VIEW_TYPE_AD = 1
    }

    private val items = mutableListOf<HomeChildItem>()

    fun submitList(
        templates: List<Template>,
        nativeAds: List<NativeAd>
    ) {

        items.clear()

        if (templates.isEmpty()) {
            notifyDataSetChanged()
            return
        }

        var adIndex = 0

        templates.forEachIndexed { index, template ->

            items.add(HomeChildItem.TemplateItem(template))

            // Add ad after every 3 items
           /* val shouldShowAd =
                (index + 1) % 3 == 0 &&
                        index != templates.lastIndex &&
                        adIndex < nativeAds.size

            if (shouldShowAd) {
                items.add(
                    HomeChildItem.NativeAdItem(nativeAds[adIndex])
                )
                adIndex++
            }*/
        }

        // Remove last ad if somehow last item becomes ad
        if (items.lastOrNull() is HomeChildItem.NativeAdItem) {
            items.removeAt(items.lastIndex)
        }

        notifyDataSetChanged()
    }

    override fun getItemViewType(position: Int): Int {
        return when (items[position]) {
            is HomeChildItem.TemplateItem -> VIEW_TYPE_ITEM
            is HomeChildItem.NativeAdItem -> VIEW_TYPE_AD
        }
    }

    override fun onCreateViewHolder(
        parent: ViewGroup,
        viewType: Int
    ): RecyclerView.ViewHolder {

        return when (viewType) {

            VIEW_TYPE_ITEM -> {

                val binding = ItemHomeAiChildBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                TemplateViewHolder(binding)
            }

            else -> {

                val binding = ItemNativeAdBinding.inflate(
                    LayoutInflater.from(parent.context),
                    parent,
                    false
                )

                NativeAdViewHolder(binding)
            }
        }
    }

    override fun getItemCount(): Int = items.size

    override fun onBindViewHolder(
        holder: RecyclerView.ViewHolder,
        position: Int
    ) {

        when (val item = items[position]) {

            is HomeChildItem.TemplateItem -> {
                (holder as TemplateViewHolder).bind(item.template)
            }

            is HomeChildItem.NativeAdItem -> {
                (holder as NativeAdViewHolder).bind(item.nativeAd)
            }
        }
    }

    override fun onViewRecycled(holder: RecyclerView.ViewHolder) {
        super.onViewRecycled(holder)
        if (holder is TemplateViewHolder) {
            holder.clearImage()
        }
    }

    inner class TemplateViewHolder(
        private val binding: ItemHomeAiChildBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(item: Template) {

            binding.tvTitle.text =
                item.title.takeUnless { it.isNullOrBlank() }
                    ?: item.prompt.orEmpty()

            TemplateThumbLoader.load(
                imageView = binding.image,
                thumbnailUrl = item.thumbnailUrl,
                mediaUrl = item.mediaUrl,
                context = context,
                gifUrl = item.gifUrl,
            )

            if (item.isPro && isProVersion.value == false){
                binding?.tvPremium?.visibility = View.VISIBLE
            }else{
                binding?.tvPremium?.visibility = View.GONE
            }

            binding.root.setSafeClickListener {
                onTemplateClick(item, categoryName, categoryId)
            }
        }

        fun clearImage() {
            TemplateThumbLoader.clear(binding.image)
        }
    }

    inner class NativeAdViewHolder(
        private val binding: ItemNativeAdBinding
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(nativeAd: NativeAd) {

            if (isProVersion.value == false){
                val adView = binding.root as NativeAdView

                adView.headlineView = binding.adHeadline
                adView.bodyView = binding.adBody
                adView.callToActionView = binding.adCallToAction
                adView.iconView = binding.adAppIcon

                binding.adHeadline.text = nativeAd.headline
                binding.adBody.text = nativeAd.body
                binding.adCallToAction.text = nativeAd.callToAction

                val icon = nativeAd.icon

                if (icon != null) {
                    binding.adAppIcon.setImageDrawable(icon.drawable)
                    binding.adAppIcon.visibility = View.VISIBLE
                } else {
                    binding.adAppIcon.visibility = View.GONE
                }

                adView.registerNativeAd(nativeAd, null)
            }else{
                HomeNativeAdManager.destroyAds()
            }

        }
    }
}