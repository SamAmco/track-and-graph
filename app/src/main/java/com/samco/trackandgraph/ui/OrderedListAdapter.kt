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
package com.samco.trackandgraph.ui

import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView

//TODO we're keeping a duplicate list here with the parent list adapter, this could
// cause bugs.
abstract class OrderedListAdapter<T, G : RecyclerView.ViewHolder>(
    private val getId: (T) -> Long,
    diffCallback: DiffUtil.ItemCallback<T>
) : ListAdapter<T, G>(diffCallback) {

    init { setHasStableIds(true) }

    private var list: MutableList<T> = mutableListOf()

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = list.removeAt(fromPosition)
        list.add(toPosition, item)
        super.notifyItemMoved(fromPosition, toPosition)
    }

    override fun submitList(list: MutableList<T>?) {
        list?.let { this.list = list }
        super.submitList(list)
    }

    override fun getItemId(position: Int): Long {
        return getId(getItem(position))
    }

    fun getItems() = list
}
