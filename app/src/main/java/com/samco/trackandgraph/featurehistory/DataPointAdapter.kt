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
package com.samco.trackandgraph.featurehistory

import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.entity.DataType
import com.samco.trackandgraph.databinding.ListItemDataPointBinding
import com.samco.trackandgraph.ui.formatDayMonthYearHourMinuteWeekDayTwoLines
import com.samco.trackandgraph.ui.getDisplayValue

class DataPointAdapter(
    private val clickListener: DataPointClickListener,
    private val weekDayNames: List<String>,
    private val dataType: DataType
) : ListAdapter<DataPoint, DataPointAdapter.ViewHolder>(DataPointDiffCallback()) {

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, clickListener, weekDayNames, dataType)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }


    class ViewHolder private constructor(
        private val binding: ListItemDataPointBinding,
        private val clickListener: DataPointClickListener,
        private val weekDayNames: List<String>,
        private val dataType: DataType
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dataPoint: DataPoint) {
            binding.valueText.text = dataPoint.getDisplayValue(dataType)
            binding.timestampText.text = formatDayMonthYearHourMinuteWeekDayTwoLines(
                binding.timestampText.context,
                weekDayNames,
                dataPoint.timestamp
            )
            binding.editButton.setOnClickListener { clickListener.editClicked(dataPoint) }
            binding.deleteButton.setOnClickListener { clickListener.deleteClicked(dataPoint) }
            if (dataPoint.note.isEmpty()) {
                binding.cardView.isClickable = false
                binding.noteText.visibility = View.GONE
                binding.noteText.text = dataPoint.note
            } else {
                binding.cardView.isClickable = true
                binding.cardView.setOnClickListener { clickListener.viewClicked(dataPoint) }
                binding.noteText.visibility = View.VISIBLE
                binding.noteText.text = dataPoint.note
            }
        }

        companion object {
            fun from(
                parent: ViewGroup,
                clickListener: DataPointClickListener,
                weekDayNames: List<String>,
                dataType: DataType
            ): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemDataPointBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, clickListener, weekDayNames, dataType)
            }
        }
    }
}

class DataPointDiffCallback : DiffUtil.ItemCallback<DataPoint>() {
    override fun areItemsTheSame(oldItem: DataPoint, newItem: DataPoint) =
        oldItem.timestamp == newItem.timestamp

    override fun areContentsTheSame(oldItem: DataPoint, newItem: DataPoint) = oldItem == newItem
}

class DataPointClickListener(
    private val onEditDataPoint: (DataPoint) -> Unit,
    private val onDeleteDataPoint: (DataPoint) -> Unit,
    private val onViewDataPoint: (DataPoint) -> Unit
) {
    fun editClicked(dataPoint: DataPoint) = onEditDataPoint(dataPoint)
    fun deleteClicked(dataPoint: DataPoint) = onDeleteDataPoint(dataPoint)
    fun viewClicked(dataPoint: DataPoint) = onViewDataPoint(dataPoint)
}
