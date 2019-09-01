package com.samco.grapheasy.selecttrackgroup

import android.annotation.SuppressLint
import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.R
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

    class ViewHolder private constructor(val binding: ListItemTrackGroupBinding)
        : RecyclerView.ViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {
        var clickListener: TrackGroupListener? = null
        var trackGroup: TrackGroup? = null

        fun bind(trackGroup: TrackGroup, clickListener: TrackGroupListener) {
            this.trackGroup = trackGroup
            this.clickListener = clickListener
            binding.trackGroup = trackGroup
            binding.clickListener = clickListener
            binding.menuButton.setOnClickListener { createContextMenu(binding.menuButton) }
        }

        private fun createContextMenu(view: View) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.edit_track_group_context_menu, popup.menu)
            popup.setOnMenuItemClickListener(this)
            popup.show()
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            trackGroup?.let {
                when (item?.itemId) {
                    R.id.rename -> clickListener?.onRename(it)
                    R.id.delete -> clickListener?.onDelete(it)
                    else -> {}
                }
            }
            return false
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


class TrackGroupListener(val clickListener: (trackGroup: TrackGroup) -> Unit,
                         val onRenameListener: (trackGroup: TrackGroup) -> Unit,
                         val onDeleteListener: (trackGroup: TrackGroup) -> Unit) {
    fun onClick(trackGroup: TrackGroup) = clickListener(trackGroup)
    fun onRename(trackGroup: TrackGroup) = onRenameListener(trackGroup)
    fun onDelete(trackGroup: TrackGroup) = onDeleteListener(trackGroup)
}