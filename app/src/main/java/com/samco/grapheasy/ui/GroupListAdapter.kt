package com.samco.grapheasy.ui

import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.R
import com.samco.grapheasy.databinding.ListItemGroupBinding

class GroupListAdapter(private val clickListener: GroupClickListener)
    : ListAdapter<GroupItem, GroupListAdapter.ViewHolder>(GroupItemDiffCallback()) {

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    class ViewHolder private constructor(private val binding: ListItemGroupBinding)
        : RecyclerView.ViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {
        var clickListener: GroupClickListener? = null
        var groupItem: GroupItem? = null

        fun bind(groupItem: GroupItem, clickListener: GroupClickListener) {
            this.groupItem = groupItem
            this.clickListener = clickListener
            binding.graphStatGroupNameText.text = groupItem.name
            binding.cardView.setOnClickListener { clickListener.onClick(groupItem) }
            binding.menuButton.setOnClickListener { createContextMenu(binding.menuButton) }
        }

        private fun createContextMenu(view: View) {
            val popup = PopupMenu(view.context, view)
            popup.menuInflater.inflate(R.menu.edit_group_context_menu, popup.menu)
            popup.setOnMenuItemClickListener(this)
            popup.show()
        }

        override fun onMenuItemClick(item: MenuItem?): Boolean {
            groupItem?.let {
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
                val binding = ListItemGroupBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}

class GroupItemDiffCallback : DiffUtil.ItemCallback<GroupItem>() {
    override fun areItemsTheSame(oldItem: GroupItem, newItem: GroupItem): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: GroupItem, newItem: GroupItem): Boolean {
        return oldItem == newItem
    }
}

data class GroupItem (val id: Long, val name: String)

class GroupClickListener(val clickListener: (groupItem: GroupItem) -> Unit,
                         val onRenameListener: (groupItem: GroupItem) -> Unit,
                         val onDeleteListener: (groupItem: GroupItem) -> Unit) {
    fun onClick(groupItem: GroupItem) = clickListener(groupItem)
    fun onRename(groupItem: GroupItem) = onRenameListener(groupItem)
    fun onDelete(groupItem: GroupItem) = onDeleteListener(groupItem)
}