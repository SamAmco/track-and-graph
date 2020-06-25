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

class GraphStatViewHolder(private val graphStatCardView: GraphStatCardView) :
    RecyclerView.ViewHolder(graphStatCardView), PopupMenu.OnMenuItemClickListener {
    private var clickListener: GraphStatClickListener? = null
    private var graphStat: IGraphStatViewData? = null
    private var dropElevation = 0f

    fun bind(
        graphStat: IGraphStatViewData,
        clickListener: GraphStatClickListener
    ) {
        this.dropElevation = graphStatCardView.cardView.cardElevation
        this.graphStat = graphStat
        this.clickListener = clickListener
        graphStatCardView.menuButtonClickListener = { v -> createContextMenu(v) }
        graphStatCardView.cardView.setOnClickListener { clickListener.onClick(graphStat) }
        graphStatCardView.graphStatView.initFromGraphStat(graphStat, true)
    }

    fun elevateCard() {
        graphStatCardView.cardView.postDelayed({
            graphStatCardView.cardView.cardElevation = graphStatCardView.cardView.cardElevation * 3f
        }, 10)
    }

    fun dropCard() {
        graphStatCardView.cardView.cardElevation = dropElevation
    }

    private fun createContextMenu(view: View) {
        val popup = PopupMenu(view.context, view)
        popup.menuInflater.inflate(R.menu.edit_graph_stat_context_menu, popup.menu)
        popup.setOnMenuItemClickListener(this)
        popup.show()
    }

    override fun onMenuItemClick(item: MenuItem?): Boolean {
        graphStat?.let {
            when (item?.itemId) {
                R.id.edit -> clickListener?.onEdit(it)
                R.id.delete -> clickListener?.onDelete(it)
                R.id.moveTo -> clickListener?.onMoveGraphStat(it)
                R.id.duplicate -> clickListener?.onDuplicate(it)
                else -> {
                }
            }
        }
        return false
    }

    companion object {
        fun from(parent: ViewGroup): GraphStatViewHolder {
            val graphStat = GraphStatCardView(parent.context)
            graphStat.layoutParams = FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.WRAP_CONTENT
            )
            return GraphStatViewHolder(graphStat)
        }
    }
}

class GraphStatDiffCallback : DiffUtil.ItemCallback<Pair<Instant, IGraphStatViewData>>() {
    override fun areItemsTheSame(
        oldItem: Pair<Instant, IGraphStatViewData>,
        newItem: Pair<Instant, IGraphStatViewData>
    ): Boolean {
        return oldItem.second.graphOrStat.id == newItem.second.graphOrStat.id
    }

    override fun areContentsTheSame(
        oldItem: Pair<Instant, IGraphStatViewData>,
        newItem: Pair<Instant, IGraphStatViewData>
    ): Boolean {
        return oldItem.first == newItem.first
    }
}


class GraphStatClickListener(
    private val onDelete: (graphStat: IGraphStatViewData) -> Unit,
    private val onEdit: (graphStat: IGraphStatViewData) -> Unit,
    private val onClick: (graphStat: IGraphStatViewData) -> Unit,
    private val onMoveGraphStat: (graphStat: IGraphStatViewData) -> Unit,
    private val onDuplicateGraphStat: (graphStat: IGraphStatViewData) -> Unit
) {
    fun onDelete(graphStat: IGraphStatViewData) = onDelete.invoke(graphStat)
    fun onEdit(graphStat: IGraphStatViewData) = onEdit.invoke(graphStat)
    fun onClick(graphStat: IGraphStatViewData) = onClick.invoke(graphStat)
    fun onMoveGraphStat(graphStat: IGraphStatViewData) = onMoveGraphStat.invoke(graphStat)
    fun onDuplicate(graphStat: IGraphStatViewData) = onDuplicateGraphStat.invoke(graphStat)
}