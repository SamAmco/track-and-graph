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

import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.database.dto.DisplayFeature
import com.samco.trackandgraph.database.entity.Group
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData

class GroupAdapter(
    private val featureClickListener: FeatureClickListener,
    private val graphStatClickListener: GraphStatClickListener,
    private val groupClickListener: GroupClickListener
) : RecyclerView.Adapter<GroupChildViewHolder>() {
    private val groupChildren = mutableListOf<GroupChild>()

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GroupChildViewHolder {
        return when (viewType) {
            GroupChildType.GRAPH.ordinal -> GraphStatViewHolder.from(parent)
            GroupChildType.FEATURE.ordinal -> FeatureViewHolder.from(parent)
            else -> GroupViewHolder.from(parent)
        }
    }

    override fun onBindViewHolder(holder: GroupChildViewHolder, position: Int) {
        val item = groupChildren[position]
        when (item.type) {
            GroupChildType.GRAPH -> (holder as GraphStatViewHolder)
                .bind(item.obj as IGraphStatViewData, graphStatClickListener)
            GroupChildType.FEATURE -> (holder as FeatureViewHolder)
                .bind(item.obj as DisplayFeature, featureClickListener)
            GroupChildType.GROUP -> (holder as GroupViewHolder)
                .bind(item.obj as Group, groupClickListener)
        }
    }

    override fun getItemViewType(position: Int): Int {
        return groupChildren.getOrNull(position)?.type?.ordinal ?: -1
    }

    override fun getItemCount(): Int = groupChildren.size

    fun submitList(newChildren: List<GroupChild>) {
        val diffCallback = ListDiffCallback(groupChildren, newChildren)
        val diffResult = DiffUtil.calculateDiff(diffCallback)
        groupChildren.clear()
        groupChildren.addAll(newChildren)
        diffResult.dispatchUpdatesTo(this)
    }

    fun getItems(): List<GroupChild> = groupChildren

    fun moveItem(start: Int, end: Int) {
        if (start < 0 || start > groupChildren.size || end < 0 || end > groupChildren.size) return
        val child = groupChildren.removeAt(start)
        groupChildren.add(end, child)
        notifyItemMoved(start, end)
    }
}

private class ListDiffCallback(
    private val oldList: List<GroupChild>,
    private val newList: List<GroupChild>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        return old.type == new.type && old.id == new.id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        val old = oldList[oldItemPosition]
        val new = newList[newItemPosition]
        return when (old.type) {
            GroupChildType.GROUP -> (old.obj as Group) == (new.obj as Group)
            GroupChildType.FEATURE -> (old.obj as DisplayFeature) == (new.obj as DisplayFeature)
            GroupChildType.GRAPH -> (old.obj as IGraphStatViewData) == (new.obj as IGraphStatViewData)
        }
    }
}

abstract class GroupChildViewHolder(view: View) : RecyclerView.ViewHolder(view) {
    abstract fun elevateCard()
    abstract fun dropCard()
}