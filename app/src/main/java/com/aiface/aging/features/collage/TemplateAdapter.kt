package com.aiface.aging.features.collage


import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.Button
import android.widget.FrameLayout
import android.widget.ImageView
import android.widget.TextView
import androidx.recyclerview.widget.RecyclerView
import androidx.recyclerview.widget.StaggeredGridLayoutManager
import com.bumptech.glide.Glide
import com.google.android.libraries.ads.mobile.sdk.nativead.MediaView
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAd
import com.google.android.libraries.ads.mobile.sdk.nativead.NativeAdView
import com.aiface.aging.R
import com.aiface.aging.shared.ads.AdsHelper.isProVersion
import com.aiface.aging.features.collage.dynamic.layout.slant.NumberSlantLayout
import com.aiface.aging.features.collage.dynamic.layout.straight.NumberStraightLayout
import com.aiface.aging.features.collage.dynamic.puzzle.PuzzleLayout
import com.aiface.aging.features.collage.dynamic.puzzle.SquarePuzzleView
import com.aiface.aging.features.collage.model.TemplateItem

private const val VIEW_TYPE_DYNAMIC = 0
private const val VIEW_TYPE_STATIC = 1
private const val VIEW_TYPE_NATIVE_AD = 2

// 7th adapter item means index 6
private const val NATIVE_AD_POSITION = 6

class TemplateAdapter(private val context: Context) :
    RecyclerView.Adapter<RecyclerView.ViewHolder>() {

    var isDynamicView = true

    private var dynamicList: List<PuzzleLayout> = ArrayList()
    private var customThumbnailsList: List<String> = ArrayList()
    private var bitmapData: List<Bitmap>? = null
    private var dynamicItemClickListener: PuzzleItemClickListener? = null

    private var staticList: ArrayList<TemplateItem>? = null
    private var staticItemClickListener: TemplateViewHolder.OnTemplateItemClickListener? = null

    /**
     * Pass your already-inflated native ad view here.
     * Example: adapter.setNativeAdView(adView)
     */
    private var nativeAd: NativeAd? = null

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): RecyclerView.ViewHolder {
        return when (viewType) {

            VIEW_TYPE_NATIVE_AD -> {
                val rootView = LayoutInflater.from(context)
                    .inflate(R.layout.layout_native_ads_recycler, parent, false)
                NativeAdViewHolder(rootView)
            }

            VIEW_TYPE_DYNAMIC -> {
                val rootView = LayoutInflater.from(context)
                    .inflate(R.layout.item_puzzle, parent, false)
                PuzzleViewHolder(rootView)
            }

            VIEW_TYPE_STATIC -> {
                val rootView = LayoutInflater.from(context)
                    .inflate(R.layout.item_template, parent, false)
                TemplateViewHolder(rootView)
            }

            else -> {
                val rootView = LayoutInflater.from(context)
                    .inflate(R.layout.item_template, parent, false)
                TemplateViewHolder(rootView)
            }
        }
    }

    override fun onBindViewHolder(holder: RecyclerView.ViewHolder, position: Int) {
        when (holder) {

            is NativeAdViewHolder -> {
                nativeAd?.let { holder.bind(it) }


            }

            is PuzzleViewHolder -> {
                val realPosition = getRealItemPosition(position)

                if (realPosition !in dynamicList.indices) return

                val puzzleLayout = dynamicList[realPosition]

                val thumbUrl = customThumbnailsList.getOrNull(realPosition)
                if (!thumbUrl.isNullOrEmpty()) {
                    Glide.with(holder.thumbnail.context)
                        .load(thumbUrl)
                        .override(800)
                        .into(holder.thumbnail)
                } else {
                    holder.thumbnail.setImageDrawable(null)
                }

                holder.puzzleView.apply {
                    isNeedDrawLine = true
                    isNeedDrawOuterLine = true
                    isTouchEnable = false
                    setPuzzleLayout(puzzleLayout)

                    bitmapData?.let { bitmaps ->
                        val bitmapSize = bitmaps.size

                        if (bitmapSize > 0) {
                            if (puzzleLayout.areaCount > bitmapSize) {
                                for (i in 0 until puzzleLayout.areaCount) {
                                    holder.puzzleView.addPiece(bitmaps[i % bitmapSize])
                                }
                            } else {
                                holder.puzzleView.addPieces(bitmaps)
                            }
                        }
                    }
                }

                holder.puzzleCard.setOnClickListener {
                    dynamicItemClickListener?.let { listener ->
                        val theme = when (puzzleLayout) {
                            is NumberSlantLayout -> puzzleLayout.theme
                            is NumberStraightLayout -> puzzleLayout.theme
                            else -> 0
                        }

                        listener.onPuzzleItemClick(puzzleLayout, theme)
                    }
                }
            }

            is TemplateViewHolder -> {
                val realPosition = getRealItemPosition(position)
                val item = staticList?.getOrNull(realPosition)
                holder.bindItem(item, staticItemClickListener)
            }
        }
    }

    override fun getItemCount(): Int {
        val originalSize = getOriginalItemCount()
        return originalSize + if (canShowNativeAd()) 1 else 0
    }

    override fun getItemViewType(position: Int): Int {
        return if (isNativeAdPosition(position)) {
            VIEW_TYPE_NATIVE_AD
        } else {
            if (isDynamicView) VIEW_TYPE_DYNAMIC else VIEW_TYPE_STATIC
        }
    }

    /**
     * Use this in Fragment/Activity GridLayoutManager spanSizeLookup.
     */
    fun isNativeAdItem(position: Int): Boolean {
        return getItemViewType(position) == VIEW_TYPE_NATIVE_AD
    }

    private fun getOriginalItemCount(): Int {
        return if (isDynamicView) {
            dynamicList.size
        } else {
            staticList?.size ?: 0
        }
    }

    private fun canShowNativeAd(): Boolean {
        return nativeAd != null && getOriginalItemCount() >= NATIVE_AD_POSITION && isProVersion.value == false

    }

    private fun isNativeAdPosition(position: Int): Boolean {
        return canShowNativeAd() && position == NATIVE_AD_POSITION
    }

    private fun getRealItemPosition(adapterPosition: Int): Int {
        return if (canShowNativeAd() && adapterPosition > NATIVE_AD_POSITION) {
            adapterPosition - 1
        } else {
            adapterPosition
        }
    }

    override fun onViewAttachedToWindow(holder: RecyclerView.ViewHolder) {
        super.onViewAttachedToWindow(holder)

        if (holder is NativeAdViewHolder) {
            val layoutParams = holder.itemView.layoutParams
            if (layoutParams is StaggeredGridLayoutManager.LayoutParams) {
                layoutParams.isFullSpan = true
            }
        }
    }


    @SuppressLint("NotifyDataSetChanged")
    fun setNativeAd(nativeAd: NativeAd?) {
        this.nativeAd = nativeAd
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun clearNativeAd() {
        this.nativeAd = null
        notifyDataSetChanged()
    }
    @SuppressLint("NotifyDataSetChanged")
    fun addPuzzleData(layoutData: List<PuzzleLayout>, bitmapData: List<Bitmap>?) {
        this.dynamicList = layoutData
        if (bitmapData != null) this.bitmapData = bitmapData
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addCustomThumbs(thumbsList: List<String>) {
        this.customThumbnailsList = thumbsList
        notifyDataSetChanged()
    }

    @SuppressLint("NotifyDataSetChanged")
    fun addStaticData(staticList: ArrayList<TemplateItem>) {
        this.staticList = staticList
        notifyDataSetChanged()
    }

    fun setTemplateClickListener(listener: TemplateViewHolder.OnTemplateItemClickListener) {
        this.staticItemClickListener = listener
    }

    fun setPuzzleItemClickListener(listener: PuzzleItemClickListener) {
        this.dynamicItemClickListener = listener
    }

    inner class PuzzleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val puzzleView: SquarePuzzleView = itemView.findViewById(R.id.puzzle)
        val puzzleCard: FrameLayout = itemView.findViewById(R.id.cardView)
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
    }

    inner class NativeAdViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {

        private val nativeAdView: NativeAdView = itemView.findViewById(R.id.nativeAdView)
        private val mediaView: MediaView = itemView.findViewById(R.id.ad_media)
        private val headlineView: TextView = itemView.findViewById(R.id.ad_headline)
      //  private val bodyView: TextView = itemView.findViewById(R.id.adBody)
    //    private val iconView: ImageView = itemView.findViewById(R.id.adIcon)
        private val callToActionView: Button = itemView.findViewById(R.id.ad_call_to_action)

        fun bind(nativeAd: NativeAd) {
            nativeAdView.headlineView = headlineView
            nativeAdView.callToActionView = callToActionView

            headlineView.text = nativeAd.headline

            if (nativeAd.callToAction.isNullOrEmpty()) {
                callToActionView.visibility = View.GONE
            } else {
                callToActionView.visibility = View.VISIBLE
                callToActionView.text = nativeAd.callToAction
            }

            nativeAdView.registerNativeAd(nativeAd, mediaView)
        }
    }
}

interface PuzzleItemClickListener {
    fun onPuzzleItemClick(layout: PuzzleLayout, themeId: Int)
}