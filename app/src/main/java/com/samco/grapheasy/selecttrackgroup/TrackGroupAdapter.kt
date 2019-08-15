package com.samco.grapheasy.selecttrackgroup

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.database.TrackGroup
import com.samco.grapheasy.databinding.ListItemTrackGroupBinding

class TrackGroupAdapter(val clickListener: TrackGroupListener)
    : ListAdapter<TrackGroup, TrackGroupAdapter.ViewHolder>(TrackGroupDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(val binding: ListItemTrackGroupBinding) : RecyclerView.ViewHolder(binding.root) {
        fun bind(item: TrackGroup, clickListener: TrackGroupListener) {
            binding.trackGroup = item
            binding.clickListener = clickListener
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemTrackGroupBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class TrackGroupDiffCallback : DiffUtil.ItemCallback<TrackGroup>() {
    override fun areItemsTheSame(oldItem: TrackGroup, newItem: TrackGroup): Boolean {
        return oldItem.id == newItem.id
    }
    @SuppressLint("DiffUtilEquals")
    override fun areContentsTheSame(oldItem: TrackGroup, newItem: TrackGroup): Boolean {
        return oldItem == newItem
    }
}


class TrackGroupListener(val clickListener: (groupId: Long) -> Unit) {
    fun onClick(trackGroup: TrackGroup) = clickListener(trackGroup.id)
}