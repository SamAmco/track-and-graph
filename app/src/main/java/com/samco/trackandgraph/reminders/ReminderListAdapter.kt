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

package com.samco.trackandgraph.reminders

import android.view.ViewGroup
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.ComposeView
import androidx.compose.ui.platform.ViewCompositionStrategy
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

internal class ReminderListAdapter(
    private val onDeleteReminder: (ReminderViewData) -> Unit
) : RecyclerView.Adapter<ReminderViewHolder>() {

    private val reminders = mutableListOf<ReminderViewData>()

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(reminders[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder.from(parent, onDeleteReminder)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = reminders.removeAt(fromPosition)
        reminders.add(toPosition, item)
        super.notifyItemMoved(fromPosition, toPosition)
    }

    fun getItems(): List<ReminderViewData> = reminders.toList()

    fun submitList(newReminders: List<ReminderViewData>) {
        if (reminders.size != newReminders.size) {
            //If an item was added or removed we want to animate the transition
            val diffCallback = ListDiffCallback(reminders, newReminders)
            val diffResult = DiffUtil.calculateDiff(diffCallback)
            this.reminders.clear()
            this.reminders.addAll(newReminders)
            diffResult.dispatchUpdatesTo(this)
        } else if (reminders.zip(newReminders).any { it.first.id != it.second.id }) {
            //If the ids have changed for any reason we need to align the view holders
            // so they send commands with the correct item id
            this.reminders.clear()
            this.reminders.addAll(newReminders)
            notifyDataSetChanged()
        } else {
            //Otherwise update our list but don't tell the view holders, they will have made
            // the same update to themselves when they requested the update and we don't want
            // un-necessary glitches/animations while typing for example
            this.reminders.clear()
            this.reminders.addAll(newReminders)
        }
    }

    override fun getItemCount(): Int = reminders.size
}

private class ListDiffCallback(
    private val oldList: List<ReminderViewData>,
    private val newList: List<ReminderViewData>
) : DiffUtil.Callback() {
    override fun getOldListSize(): Int = oldList.size

    override fun getNewListSize(): Int = newList.size

    override fun areItemsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition].id == newList[newItemPosition].id
    }

    override fun areContentsTheSame(oldItemPosition: Int, newItemPosition: Int): Boolean {
        return oldList[oldItemPosition] == newList[newItemPosition]
    }
}

internal class ReminderViewHolder private constructor(
    private val composeView: ComposeView,
    private val onDeleteReminder: (ReminderViewData) -> Unit
) : RecyclerView.ViewHolder(composeView) {

    private var isElevated by mutableStateOf(false)

    init {
        composeView.setViewCompositionStrategy(ViewCompositionStrategy.DisposeOnViewTreeLifecycleDestroyed)
    }

    fun bind(reminder: ReminderViewData) {
        composeView.setContent {
            TnGComposeTheme {
                Reminder(
                    isElevated = isElevated,
                    reminderViewData = reminder,
                    onDeleteClick = { onDeleteReminder(reminder) }
                )
            }
        }

        //This fixes a bug because compose views don't calculate their height immediately,
        // scrolling up through a recycler view causes jumpy behavior. See this issue:
        // https://issuetracker.google.com/issues/240449681
        composeView.getChildAt(0)?.requestLayout()
    }

    fun elevateCard() {
        isElevated = true
    }

    fun dropCard() {
        isElevated = false
    }

    companion object {
        fun from(parent: ViewGroup, onDeleteReminder: (ReminderViewData) -> Unit): ReminderViewHolder {
            val composeView = ComposeView(parent.context)
            return ReminderViewHolder(composeView, onDeleteReminder)
        }
    }
}
