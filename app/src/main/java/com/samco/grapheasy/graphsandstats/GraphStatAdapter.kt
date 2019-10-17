package com.samco.grapheasy.graphsandstats

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.databinding.ListItemGraphStatBinding

class GraphStatAdapter(private val clickListener: GraphStatClickListener)
    : ListAdapter<GraphStat, GraphStatAdapter.ViewHolder>(GraphStatDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder(private val binding: ListItemGraphStatBinding)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(graphStat: GraphStat, clickListener: GraphStatClickListener) {
            binding.clickListener = clickListener
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemGraphStatBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class GraphStatDiffCallback() : DiffUtil.ItemCallback<GraphStat>() {
    //TODO GraphStatDiffCallback.areItemsTheSame
    override fun areItemsTheSame(oldItem: GraphStat, newItem: GraphStat) = false

    //TODO GraphStatDiffCallback.areContentsTheSame
    override fun areContentsTheSame(oldItem: GraphStat, newItem: GraphStat) = false
}

class GraphStatClickListener