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

import android.annotation.SuppressLint
import android.view.LayoutInflater
import android.view.View
import android.view.ViewGroup
import androidx.recyclerview.widget.AsyncListDiffer
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.helpers.formatDayMonthYearHourMinuteWeekDayTwoLines
import com.samco.trackandgraph.base.helpers.getDisplayValue
import com.samco.trackandgraph.databinding.ListItemDataPointBinding

class DataPointAdapter(
    private val clickListener: DataPointClickListener,
    private val weekDayNames: List<String>
) : RecyclerView.Adapter<DataPointAdapter.ViewHolder>() {
    companion object {
        private val DIFFER = object : DiffUtil.ItemCallback<DataPoint>() {
            override fun areItemsTheSame(oldItem: DataPoint, newItem: DataPoint) =
                oldItem.timestamp == newItem.timestamp

            override fun areContentsTheSame(oldItem: DataPoint, newItem: DataPoint) =
                oldItem == newItem
        }
    }

    private val differ = AsyncListDiffer(this, DIFFER)
    private var isDuration: Boolean = false
    private var isTracker: Boolean = false

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent, clickListener, weekDayNames)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(differ.currentList[position], isDuration, isTracker)
    }

    override fun getItemCount() = differ.currentList.size

    @SuppressLint("NotifyDataSetChanged")
    fun submitIsDuration(isDuration: Boolean) {
        if (this.isDuration != isDuration) {
            this.isDuration = isDuration
            notifyDataSetChanged()
        }
    }

    @SuppressLint("NotifyDataSetChanged")
    fun submitIsTracker(isTracker: Boolean) {
        if (this.isTracker != isTracker) {
            this.isTracker = isTracker
            notifyDataSetChanged()
        }
    }

    fun submitDataPoints(dataPoints: List<DataPoint>) {
        differ.submitList(dataPoints)
    }

    class ViewHolder private constructor(
        private val binding: ListItemDataPointBinding,
        private val clickListener: DataPointClickListener,
        private val weekDayNames: List<String>
    ) : RecyclerView.ViewHolder(binding.root) {

        fun bind(dataPoint: DataPoint, isDuration: Boolean, isTracker: Boolean) {
            binding.valueText.text = dataPoint.getDisplayValue(isDuration)
            binding.timestampText.text = formatDayMonthYearHourMinuteWeekDayTwoLines(
                binding.timestampText.context,
                weekDayNames,
                dataPoint.timestamp
            )
            binding.editButton.visibility = if (isTracker) View.VISIBLE else View.GONE
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
                weekDayNames: List<String>
            ): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemDataPointBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding, clickListener, weekDayNames)
            }
        }
    }
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
