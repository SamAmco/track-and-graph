package com.samco.grapheasy.displaytrackgroup

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.databinding.ListItemFeatureBinding

class FeatureAdapter(
    private val clickListener: FeatureClickListener,
    private val viewModel: DisplayTrackGroupViewModel
) : ListAdapter<Feature, FeatureAdapter.ViewHolder>(FeatureDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, viewModel)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    class ViewHolder private constructor(private val binding: ListItemFeatureBinding,
                                         private val viewModel: DisplayTrackGroupViewModel)
        : RecyclerView.ViewHolder(binding.root) {
        var clickListener: FeatureClickListener? = null
        var feature: Feature? = null

        fun bind(feature: Feature, clickListener: FeatureClickListener) {
            this.feature = feature
            this.clickListener = clickListener
            binding.feature = feature
            binding.clickListener = clickListener
            binding.viewModel = viewModel
            //TODO set on click listeners..
        }

        companion object {
            fun from(parent: ViewGroup, viewModel: DisplayTrackGroupViewModel): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemFeatureBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, viewModel)
            }
        }
    }
}

class FeatureDiffCallback : DiffUtil.ItemCallback<Feature>() {
    override fun areItemsTheSame(oldItem: Feature, newItem: Feature): Boolean {
        return oldItem.id == newItem.id
    }
    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: Feature, newItem: Feature): Boolean {
        return oldItem == newItem
    }
}

class FeatureClickListener
