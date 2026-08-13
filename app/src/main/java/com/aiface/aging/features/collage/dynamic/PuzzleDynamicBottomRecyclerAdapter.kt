package com.aiface.aging.features.collage.dynamic

import android.annotation.SuppressLint
import android.content.Context
import android.graphics.Bitmap
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.bumptech.glide.Glide
import com.aiface.aging.R
import com.aiface.aging.features.collage.dynamic.layout.slant.NumberSlantLayout
import com.aiface.aging.features.collage.dynamic.layout.straight.NumberStraightLayout
import com.aiface.aging.features.collage.dynamic.puzzle.PuzzleLayout
import com.aiface.aging.features.collage.dynamic.puzzle.SquarePuzzleView


class PuzzleDynamicBottomRecyclerAdapter(private val context: Context) :
    RecyclerView.Adapter<PuzzleDynamicBottomRecyclerAdapter.PuzzleViewHolder>() {

    private var dynamicList: List<PuzzleLayout> = ArrayList()
    private var customThumbnailsList: List<String> = ArrayList()
    private var bitmapData: List<Bitmap>? = null
    private var dynamicItemClickListener: PuzzleItemClickListener? = null
    var selectedPosition = 0

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): PuzzleViewHolder {
            val rootView = LayoutInflater.from(context).inflate(R.layout.item_puzzle_bottom, parent, false)
           return PuzzleViewHolder(rootView)
    }

    override fun onBindViewHolder(holder: PuzzleViewHolder, position: Int) {
        val puzzleLayout = dynamicList[position]
        if (customThumbnailsList.isNotEmpty())
            Glide.with(holder.thumbnail.context)
                .load(customThumbnailsList[position])
                .into(holder.thumbnail)
        holder.puzzleView.apply {
            isNeedDrawLine = true
            isNeedDrawOuterLine = true
            isTouchEnable = false
            setPuzzleLayout(puzzleLayout)

            holder.puzzleCard.setOnClickListener {
                selectedPosition = holder.adapterPosition
                dynamicItemClickListener?.let {
                    val theme = when (puzzleLayout) {
                        is NumberSlantLayout -> puzzleLayout.theme
                        is NumberStraightLayout -> puzzleLayout.theme
                        else -> 0
                    }
                    it.onPuzzleItemClick(puzzleLayout, theme)
                }
                notifyDataSetChanged()
            }

            bitmapData?.let {
                val bitmapSize = it.size

                if (puzzleLayout.areaCount > bitmapSize) {
                    for (i in 0 until puzzleLayout.areaCount) {
                        holder.puzzleView.addPiece(it[i % bitmapSize])
                    }
                } else {
                    holder.puzzleView.addPieces(it)
                }
            }


        }
        if (selectedPosition==position){
         //   holder.puzzleCard.setBackgroundColor(context.getColor(R.color.colorHighlightBlueDark))
            holder.puzzleCard.foreground=ContextCompat.getDrawable(context,R.drawable.bg_collage_bottom_selected)
        }else  holder.puzzleCard.foreground=ContextCompat.getDrawable(context,R.drawable.bg_collage_bottom_unselected)
    }


    override fun getItemCount(): Int = dynamicList.size

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

    fun setPuzzleItemClickListener(listener: PuzzleItemClickListener) {
        this.dynamicItemClickListener = listener
    }

    inner class PuzzleViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        val puzzleView: SquarePuzzleView = itemView.findViewById(R.id.puzzle)
        val puzzleCard: FrameLayout = itemView.findViewById(R.id.cardView)
        val thumbnail: ImageView = itemView.findViewById(R.id.thumbnail)
    }
}

interface PuzzleItemClickListener {
    fun onPuzzleItemClick(layout: PuzzleLayout, themeId: Int)
}