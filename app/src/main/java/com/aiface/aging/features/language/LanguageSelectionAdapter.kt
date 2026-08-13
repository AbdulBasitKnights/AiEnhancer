package com.aiface.aging.features.language

import android.content.Context
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import android.widget.ImageView
import android.widget.TextView
import androidx.constraintlayout.widget.ConstraintLayout
import androidx.core.content.ContextCompat
import androidx.core.content.res.ResourcesCompat
import androidx.recyclerview.widget.RecyclerView
import com.airbnb.lottie.Lottie
import com.airbnb.lottie.LottieAnimationView
import com.bumptech.glide.Glide
import com.aiface.aging.R
import kotlin.collections.forEach
import kotlin.collections.indices
import kotlin.let


class LanguageSelectionAdapter(
    private val list: List<LanguageModel>,
    private val listener: LanguageSelectionClickListener
) :
    RecyclerView.Adapter<LanguageSelectionAdapter.LanguageSelectionViewHolder>() {

    private var selectedPosition = -1

    fun setSelectedThePosition(position: Int) {
        if (position !in list.indices) return

        // Clear old selection
        if (selectedPosition in list.indices) {
            list[selectedPosition].isSelected = false
        }

        // Set new selection
        list[position].isSelected = true
        selectedPosition = position

        notifyDataSetChanged()
    }

    inner class LanguageSelectionViewHolder(itemView: View) :
        RecyclerView.ViewHolder(itemView) {
        var rootLayout: ConstraintLayout? = null
        var name: TextView? = null
        var anim : LottieAnimationView ? = null

        //var icon: ImageView? = null
        var flag: ImageView? = null
        var selector: ImageView? = null

        init {
            rootLayout = itemView.findViewById(R.id.parent)
            name = itemView.findViewById(R.id.tv_language)
            //icon = itemView.findViewById(R.id.icon)
            flag = itemView.findViewById(R.id.flag)
            selector = itemView.findViewById<ImageView>(R.id.selector)
            anim = itemView.findViewById<LottieAnimationView>(R.id.animationTap)
        }
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): LanguageSelectionViewHolder {
        val view = LayoutInflater.from(parent.context)
            .inflate(R.layout.item_language, parent, false)
        return LanguageSelectionViewHolder(view)
    }

    override fun onBindViewHolder(holder: LanguageSelectionViewHolder, position: Int) {
        val item = list[position]
        holder.name?.text = item.name
        holder.flag?.let { Glide.with(it.context).load(item.icon).into(it) }

        if (item.isSelected){
            holder.selector?.let {
                Glide.with(it.context).load(R.drawable.ic_radio_selected).into(it)
            }
            holder.rootLayout?.let {
                it?.context?.let { context->
                    holder?.rootLayout?.setBackgroundResource(R.drawable.language_bg_selected)
                }
            }
        }else{
            holder.selector?.let {
                Glide.with(it.context).load(R.drawable.ic_radio_unselected).into(it)
            }
            holder.rootLayout?.let {
                it?.context?.let { context->
                    holder?.rootLayout?.setBackgroundResource(R.drawable.language_bg_stroke)
                }
            }
        }

        val noItemSelected = list.none { it.isSelected } && item.lang == "en"
        holder.anim?.visibility = if (noItemSelected) View.VISIBLE else View.GONE

        holder.itemView.setOnClickListener {

            list.forEach {
                it.isSelected = false
            }
            item.isSelected = true
            listener.onLanguageClick(item)
            notifyDataSetChanged()

        }


        if (item.isSelected) {
            //   holder.rootLayout?.isActivated = true
            holder.selector?.let {
                Glide.with(it.context).load(R.drawable.ic_radio_selected).into(it)
            }
            holder.rootLayout?.isActivated = false
           /* holder.name?.let { name ->
                name.context?.let { context: Context ->
                    //   name.setTextColor(ContextCompat.getColor(context,R.color.black))
                    val typeface = ResourcesCompat.getFont(context, R.font.inter_semibold)
                    name.typeface = typeface
                }

            }*/
        }
        else {
            holder.selector?.let {
                Glide.with(it.context).load(R.drawable.ic_radio_unselected).into(it)
            }
            holder.rootLayout?.isActivated = false
          /*  holder.name?.let { name ->
                name.context?.let { context: Context ->
                    //   name.setTextColor(ContextCompat.getColor(context,R.color.black))
                    val typeface = ResourcesCompat.getFont(context, R.font.inter_medium)
                    name.typeface = typeface
                }

            }*/
        }
    }

    override fun getItemCount(): Int {
        return list.size
    }


    interface LanguageSelectionClickListener {
        fun onLanguageClick(language: LanguageModel?)
    }
}