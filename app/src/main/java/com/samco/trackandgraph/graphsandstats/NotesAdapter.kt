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

package com.samco.trackandgraph.graphsandstats

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.database.DataPoint
import com.samco.trackandgraph.databinding.ListItemDataPointBinding
import com.samco.trackandgraph.databinding.ListItemNoteBinding

class NotesAdapter(private val clickListener: NoteClickListener)
    : ListAdapter<DataPoint, NotesAdapter.ViewHolder>(DataPointDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, clickListener)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder private constructor(private val binding: ListItemNoteBinding,
                                         private val clickListener: NoteClickListener)
        : RecyclerView.ViewHolder(binding.root) {

        fun bind(dataPoint: DataPoint) {
            binding.valueText.text = dataPoint.getDisplayValue()
            binding.timestampText.text = dataPoint.getDisplayTimestamp()
            binding.featureNameText.text = "TODO inject the feature names somehow"
            binding.cardView.setOnClickListener { clickListener.viewClicked(dataPoint) }
            binding.noteText.visibility = View.VISIBLE
            binding.noteText.text = dataPoint.note
        }

        companion object {
            fun from(parent: ViewGroup, clickListener: NoteClickListener): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemNoteBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, clickListener)
            }
        }
    }
}

class DataPointDiffCallback : DiffUtil.ItemCallback<DataPoint>() {
    override fun areItemsTheSame(oldItem: DataPoint, newItem: DataPoint) = oldItem.timestamp == newItem.timestamp && oldItem.featureId == newItem.featureId
    override fun areContentsTheSame(oldItem: DataPoint, newItem: DataPoint) = oldItem == newItem
}
class NoteClickListener(private val onViewDataPoint: (DataPoint) -> Unit) {
    fun viewClicked(dataPoint: DataPoint) = onViewDataPoint(dataPoint)
}
