package com.aiface.aging.features.look.facemakeup

import android.annotation.SuppressLint
import android.graphics.Color
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.ImageView
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import androidx.core.graphics.toColorInt

class ColorAdapter(
    private val colors: List<String>,
    private val onColorSelected: (String) -> Unit,
    private val onColorPickerClicked: () -> Unit
) : RecyclerView.Adapter<ColorAdapter.ColorViewHolder>() {

    private var selectedPosition = -1

    companion object {
        const val TYPE_COLOR_PICKER = 0
        const val TYPE_COLOR = 1
    }

    override fun getItemViewType(position: Int): Int {
        return if (position == 0) TYPE_COLOR_PICKER else TYPE_COLOR
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ColorViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_color_option, parent, false)
        return ColorViewHolder(view)
    }

    override fun onBindViewHolder(holder: ColorViewHolder, position: Int) {
        holder.bind(position)
    }

    override fun getItemCount(): Int = colors.size + 1 // +1 for color picker

    inner class ColorViewHolder(itemView: View) : RecyclerView.ViewHolder(itemView) {
        private val colorCircle: View = itemView.findViewById(R.id.colorCircle)
        private val colorPickerIcon: ImageView = itemView.findViewById(R.id.colorPickerIcon)

        @SuppressLint("UseCompatLoadingForDrawables")
        fun bind(position: Int) {
            when (getItemViewType(position)) {
                TYPE_COLOR_PICKER -> {
                    // Color picker item
                    colorPickerIcon.visibility = View.VISIBLE
                    itemView.setOnClickListener {
                        onColorPickerClicked()
                    }
                }
                TYPE_COLOR -> {
                    // Regular color item
                    val colorString = colors[position - 1] // -1 because first item is picker
                    colorPickerIcon.visibility = View.GONE
                    
                    // Update selection state
                    if (position == selectedPosition) {
                        // Selected state - add black border
                        itemView.setPadding(2, 2, 2, 2)
                        itemView.background = itemView.context.getDrawable(R.drawable.circle_selector_selected)
                        colorCircle.background.setTint(colorString.toColorInt())
                    } else {
                        // Normal state - no border
                        itemView.setPadding(0, 0, 0, 0)
                        itemView.background = null
                        colorCircle.background.setTint(colorString.toColorInt())
                    }
                    
                    itemView.setOnClickListener {
                        val previousSelected = selectedPosition
                        selectedPosition = position
                        notifyItemChanged(previousSelected)
                        notifyItemChanged(selectedPosition)
                        onColorSelected(colorString)
                    }
                }
            }
        }
    }

    fun setSelectedColor(colorString: String) {
        val newPosition = colors.indexOf(colorString) + 1 // +1 for picker offset
        if (newPosition > 0 && newPosition != selectedPosition) {
            val previousSelected = selectedPosition
            selectedPosition = newPosition
            notifyItemChanged(previousSelected)
            notifyItemChanged(selectedPosition)
        }
    }
} 