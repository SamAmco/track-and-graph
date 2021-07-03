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
package com.samco.trackandgraph.graphsandstats

import android.view.MenuItem
import android.view.View
import android.view.ViewGroup
import android.widget.FrameLayout
import android.widget.PopupMenu
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.R
import com.samco.trackandgraph.graphstatview.GraphStatCardView
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.ui.OrderedListAdapter
import org.threeten.bp.Instant

private val getId: (Pair<Instant, IGraphStatViewData>) -> Long = { it.second.graphOrStat.id }

class GraphStatAdapter(
    private val clickListener: GraphStatClickListener
) : OrderedListAdapter<Pair<Instant, IGraphStatViewData>, GraphStatViewHolder>(
    getId,
    GraphStatDiffCallback()
) {
    override fun onBindViewHolder(holder: GraphStatViewHolder, position: Int) {
        holder.bind(getItems()[position].second, clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): GraphStatViewHolder {
        return GraphStatViewHolder.from(parent)
    }
}
