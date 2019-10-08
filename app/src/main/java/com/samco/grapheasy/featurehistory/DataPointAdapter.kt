package com.samco.grapheasy.featurehistory

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.ListAdapter
import androidx.recyclerview.widget.RecyclerView
import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.databinding.ListItemDataPointBinding

class DataPointAdapter : ListAdapter<DataPoint, DataPointAdapter.ViewHolder>(DataPointDiffCallback()) {
    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ViewHolder {
        return ViewHolder.from(parent)
    }

    override fun onBindViewHolder(holder: ViewHolder, position: Int) {
        holder.bind(getItem(position))
    }

    class ViewHolder private constructor(private val binding: ListItemDataPointBinding)
        : RecyclerView.ViewHolder(binding.root) {

        //TODO add a click callback
        fun bind(dataPoint: DataPoint) {
            binding.dataPoint = dataPoint
        }

        companion object {
            fun from(parent: ViewGroup): ViewHolder {
                val layoutInflater = LayoutInflater.from(parent.context)
                val binding = ListItemDataPointBinding.inflate(layoutInflater, parent, false)
                return ViewHolder(binding)
            }
        }
    }
}
class DataPointDiffCallback : DiffUtil.ItemCallback<DataPoint>() {
    override fun areItemsTheSame(oldItem: DataPoint, newItem: DataPoint) = oldItem == newItem
    override fun areContentsTheSame(oldItem: DataPoint, newItem: DataPoint) = oldItem.id == newItem.id
}