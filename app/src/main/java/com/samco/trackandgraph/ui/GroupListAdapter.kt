package com.samco.trackandgraph.ui

import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.databinding.ListItemGroupBinding
import java.util.*

class GroupListAdapter(private val clickListener: GroupClickListener)
    : ListAdapter<GroupItem, GroupViewHolder>(GroupItemDiffCallback()) {

    private var list: MutableList<GroupItem> = mutableListOf()

    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder.from(parent)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = list.removeAt(fromPosition)
        list.add(toPosition, item)
        super.notifyItemMoved(fromPosition, toPosition)
    }

    override fun submitList(list: MutableList<GroupItem>?) {
        list?.let { this.list = list }
        super.submitList(list)
    }

    fun getItems() = list
}

class GroupViewHolder private constructor(private val binding: ListItemGroupBinding)
    : RecyclerView.ViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {
    private var clickListener: GroupClickListener? = null
    private var dropElevation = 0f
    var groupItem: GroupItem? = null
        private set

    fun bind(groupItem: GroupItem, clickListener: GroupClickListener) {
        this.groupItem = groupItem
        this.clickListener = clickListener
        dropElevation = binding.cardView.cardElevation
        binding.graphStatGroupNameText.text = groupItem.name
        binding.cardView.setOnClickListener { clickListener.onClick(groupItem) }
        binding.menuButton.setOnClickListener { createContextMenu(binding.menuButton) }
    }

    fun elevateCard() {
        binding.cardView.postDelayed({
            binding.cardView.cardElevation = binding.cardView.cardElevation * 3f
        }, 10)
    }

    fun dropCard() {
        binding.cardView.cardElevation = dropElevation
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
        fun from(parent: ViewGroup): GroupViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemGroupBinding.inflate(layoutInflater, parent, false)
            return GroupViewHolder(binding)
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

data class GroupItem (val id: Long, val name: String, val displayIndex: Int)

class GroupClickListener(val clickListener: (groupItem: GroupItem) -> Unit,
                         val onRenameListener: (groupItem: GroupItem) -> Unit,
                         val onDeleteListener: (groupItem: GroupItem) -> Unit) {
    fun onClick(groupItem: GroupItem) = clickListener(groupItem)
    fun onRename(groupItem: GroupItem) = onRenameListener(groupItem)
    fun onDelete(groupItem: GroupItem) = onDeleteListener(groupItem)
}