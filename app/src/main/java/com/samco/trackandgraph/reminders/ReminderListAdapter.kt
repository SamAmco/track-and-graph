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
 * along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.reminders

import android.view.LayoutInflater
import android.view.ViewGroup
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.database.Reminder
import com.samco.trackandgraph.databinding.ListItemReminderBinding
import com.samco.trackandgraph.ui.OrderedListAdapter

class ReminderListAdapter(private val clickListener: ReminderClickListener)
    : OrderedListAdapter<Reminder, ReminderViewHolder>(ReminderDiffCallback()) {
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder.from(parent)
    }
}

class ReminderViewHolder private constructor(private val binding: ListItemReminderBinding)
    : RecyclerView.ViewHolder(binding.root) {
    private var clickListener: ReminderClickListener? = null
    private var dropElevation = 0f
    private var reminder: Reminder? = null

    fun bind(reminder: Reminder, clickListener: ReminderClickListener) {
        this.reminder = reminder
        this.clickListener = clickListener
        dropElevation = binding.cardView.cardElevation
        //TODO set click listeners
    }

    fun elevateCard() {
        binding.cardView.postDelayed({
            binding.cardView.cardElevation = binding.cardView.cardElevation * 3f
        }, 10)
    }

    fun dropCard() {
        binding.cardView.cardElevation = dropElevation
    }

    companion object {
        fun from(parent: ViewGroup): ReminderViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemReminderBinding.inflate(layoutInflater, parent, false)
            return ReminderViewHolder(binding)
        }
    }
}

class ReminderDiffCallback : DiffUtil.ItemCallback<Reminder>() {
    override fun areItemsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
        return oldItem.id == newItem.id
    }
    override fun areContentsTheSame(oldItem: Reminder, newItem: Reminder): Boolean {
        return oldItem == newItem
    }
}


class ReminderClickListener() { }
