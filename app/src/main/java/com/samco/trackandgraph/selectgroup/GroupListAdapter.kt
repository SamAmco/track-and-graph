/* 
* This file is part of Track & Graph
* 
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
* 
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
* 
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.selectgroup

import android.view.*
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.entity.Group
import com.samco.trackandgraph.databinding.ListItemGroupBinding
import com.samco.trackandgraph.ui.OrderedListAdapter

class GroupListAdapter(
    private val clickListener: GroupClickListener,
    private val trackColor: Int,
    private val graphStatColor: Int
) : OrderedListAdapter<Group, GroupViewHolder>({ it.id }, GroupItemDiffCallback()) {
    override fun onBindViewHolder(holder: GroupViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener, trackColor, graphStatColor)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupViewHolder {
        return GroupViewHolder.from(parent)
    }
}

class GroupViewHolder private constructor(private val binding: ListItemGroupBinding) :
    RecyclerView.ViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {
    private var clickListener: GroupClickListener? = null
    private var dropElevation = 0f
    private var groupItem: Group? = null

    fun bind(
        groupItem: Group,
        clickListener: GroupClickListener,
        trackColor: Int,
        graphStatColor: Int
    ) {
        this.groupItem = groupItem
        this.clickListener = clickListener
        dropElevation = binding.cardView.cardElevation
        binding.graphStatGroupNameText.text = groupItem.name
        binding.cardView.setOnClickListener { clickListener.onClick(groupItem) }
        binding.menuButton.setOnClickListener { createContextMenu(binding.menuButton) }
        initCorner(trackColor, graphStatColor)
    }

    private fun initCorner(trackColor: Int, graphStatColor: Int) {
        binding.cornerTabImage.setColorFilter(trackColor)
        binding.trackIcon.visibility = View.VISIBLE
        binding.graphIcon.visibility = View.INVISIBLE
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
                else -> {
                }
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

class GroupItemDiffCallback : DiffUtil.ItemCallback<Group>() {
    override fun areItemsTheSame(oldItem: Group, newItem: Group): Boolean {
        return oldItem.id == newItem.id
    }

    override fun areContentsTheSame(oldItem: Group, newItem: Group): Boolean {
        return oldItem == newItem
    }
}


class GroupClickListener(
    val clickListener: (group: Group) -> Unit,
    val onRenameListener: (group: Group) -> Unit,
    val onDeleteListener: (group: Group) -> Unit
) {
    fun onClick(group: Group) = clickListener(group)
    fun onRename(group: Group) = onRenameListener(group)
    fun onDelete(group: Group) = onDeleteListener(group)
}