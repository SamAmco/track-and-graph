/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.group

import android.view.LayoutInflater
import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.PopupMenu
import androidx.core.content.ContextCompat
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.dataVisColorList
import com.samco.trackandgraph.database.entity.Group
import com.samco.trackandgraph.databinding.ListItemGroupBinding

class GroupViewHolder private constructor(private val binding: ListItemGroupBinding) :
    GroupChildViewHolder(binding.root), PopupMenu.OnMenuItemClickListener {
    private var clickListener: GroupClickListener? = null
    private var dropElevation = 0f
    private var groupItem: Group? = null

    fun bind(
        groupItem: Group,
        clickListener: GroupClickListener
    ) {
        this.groupItem = groupItem
        this.clickListener = clickListener
        dropElevation = binding.cardView.cardElevation
        binding.graphStatGroupNameText.text = groupItem.name
        binding.cardView.setOnClickListener { clickListener.onClick(groupItem) }
        binding.menuButton.setOnClickListener { createContextMenu(binding.menuButton) }
        initCorner()
    }

    private fun initCorner() {
        val colorId = dataVisColorList[groupItem?.colorIndex ?: 8]
        binding.cornerTabImage.setColorFilter(ContextCompat.getColor(binding.root.context, colorId))
    }

    override fun elevateCard() {
        binding.cardView.postDelayed({
            binding.cardView.cardElevation = binding.cardView.cardElevation * 3f
        }, 10)
    }

    override fun dropCard() {
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
                R.id.edit -> clickListener?.onEdit(it)
                R.id.delete -> clickListener?.onDelete(it)
                R.id.moveTo -> clickListener?.onMove(it)
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

class GroupClickListener(
    val clickListener: (group: Group) -> Unit,
    val onEditListener: (group: Group) -> Unit,
    val onDeleteListener: (group: Group) -> Unit,
    val onMoveListener: (group: Group) -> Unit
) {
    fun onClick(group: Group) = clickListener(group)
    fun onEdit(group: Group) = onEditListener(group)
    fun onDelete(group: Group) = onDeleteListener(group)
    fun onMove(group: Group) = onMoveListener(group)
}
