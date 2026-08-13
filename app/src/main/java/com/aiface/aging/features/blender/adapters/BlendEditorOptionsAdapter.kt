package com.aiface.aging.features.blender.adapters

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.core.content.ContextCompat
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.R
import com.aiface.aging.databinding.EditorOptionLayoutBinding
import com.aiface.aging.features.blender.model.BlendEditorOptionsModel

class BlendEditorOptionsAdapter(private var itemClick: ((text: String) -> Unit)) :
    RecyclerView.Adapter<BlendEditorOptionsAdapter.EditorOptionsViewHolder>() {

    private var editorOptionList: ArrayList<BlendEditorOptionsModel> = ArrayList()
    private var selectedPosition = -1


    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): EditorOptionsViewHolder {

        val inflater = LayoutInflater.from(parent.context)
        val binding = EditorOptionLayoutBinding.inflate(inflater, parent, false)
        return EditorOptionsViewHolder(binding)
    }


    override fun onBindViewHolder(holder: EditorOptionsViewHolder, position: Int) {
        holder.binding.icon.setImageResource(editorOptionList[position].iconId)
        holder.binding.text.text = editorOptionList[position].title

        if (selectedPosition == position) {
            holder.binding.icon.setColorFilter(
                ContextCompat.getColor(
                    holder.itemView.context, R.color.purple
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )

            holder.binding.text.setTextColor(holder.itemView.context.getColor(R.color.purple))

        } else {
            holder.binding.icon.setColorFilter(
                ContextCompat.getColor(
                    holder.itemView.context, R.color.grayIcon
                ), android.graphics.PorterDuff.Mode.SRC_IN
            )
            holder.binding.text.setTextColor(holder.itemView.context.getColor(R.color.grayIcon))
        }


        holder.itemView.setOnClickListener {
            itemClick(editorOptionList[position].title)
            selectedPosition = holder.adapterPosition
            notifyDataSetChanged()
        }
    }


    override fun getItemCount(): Int {
        return editorOptionList.size
    }


    fun updateList(editorOptionList: ArrayList<BlendEditorOptionsModel>) {
        this.editorOptionList = editorOptionList
        notifyDataSetChanged()
    }

    fun updatePosition(position: Int) {
        this.selectedPosition = position
        notifyDataSetChanged()
    }




    inner class EditorOptionsViewHolder(var binding: EditorOptionLayoutBinding) :
        RecyclerView.ViewHolder(binding.root)
}