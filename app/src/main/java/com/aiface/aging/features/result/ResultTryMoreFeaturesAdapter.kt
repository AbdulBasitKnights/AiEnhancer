package com.aiface.aging.features.result

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.RecyclerView
import com.aiface.aging.databinding.ItemResultTryMoreFeatureBinding

class ResultTryMoreFeaturesAdapter(
    private val onFeatureClick: (ResultTryMoreFeature) -> Unit,
) : RecyclerView.Adapter<ResultTryMoreFeaturesAdapter.FeatureViewHolder>() {

    private val items = ResultTryMoreFeature.entries.toList()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): FeatureViewHolder {
        val binding = ItemResultTryMoreFeatureBinding.inflate(
            LayoutInflater.from(parent.context),
            parent,
            false,
        )
        return FeatureViewHolder(binding)
    }

    override fun onBindViewHolder(holder: FeatureViewHolder, position: Int) {
        holder.bind(items[position])
    }

    override fun getItemCount(): Int = items.size

    inner class FeatureViewHolder(
        private val binding: ItemResultTryMoreFeatureBinding,
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(feature: ResultTryMoreFeature) {
            binding.tvFeatureLabel.setText(feature.titleRes)
            binding.ivFeature.setImageResource(feature.imageRes)
            binding.root.setOnClickListener { onFeatureClick(feature) }
        }
    }
}
