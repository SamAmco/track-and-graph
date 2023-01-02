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

import android.app.TimePickerDialog
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.databinding.ListItemReminderBinding
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

internal class ReminderListAdapter(
    private val clickListener: ReminderClickListener
) : RecyclerView.Adapter<ReminderViewHolder>() {

    private val reminders = mutableListOf<Reminder>()

    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(reminders[position])
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder.from(parent, clickListener)
    }

    fun moveItem(fromPosition: Int, toPosition: Int) {
        val item = reminders.removeAt(fromPosition)
        reminders.add(toPosition, item)
        super.notifyItemMoved(fromPosition, toPosition)
    }

    fun getItems(): List<Reminder> = reminders.toList()

    fun submitList(newReminders: List<Reminder>) {
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
    private val oldList: List<Reminder>,
    private val newList: List<Reminder>
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
    private val binding: ListItemReminderBinding,
    private val clickListener: ReminderClickListener
) : RecyclerView.ViewHolder(binding.root) {

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    private val checkboxes = listOf(
        binding.monBox, binding.tueBox, binding.wedBox,
        binding.thuBox, binding.friBox, binding.satBox, binding.sunBox
    )

    private var dropElevation = 0f
    private var reminder: Reminder? = null

    init {
        dropElevation = binding.cardView.cardElevation
        binding.deleteButton.setOnClickListener {
            reminder?.let { clickListener.delete(it) }
        }
        listenToCheckboxes()
        listenToTimeButton()
        listenToName()
    }

    fun bind(reminder: Reminder) {
        this.reminder = reminder
        setTime(reminder.time)
        setReminderNameText(reminder.alarmName)
        setCheckedDays(reminder.checkedDays)
    }

    private fun setCheckedDays(checkedDays: CheckedDays) {
        val boolList = checkedDays.toList()
        checkboxes.forEachIndexed { index, checkBox ->
            checkBox.isChecked = boolList[index]
        }
    }

    private fun listenToName() {
        binding.reminderNameText.addTextChangedListener(afterTextChanged = {
            reminder = reminder?.copy(alarmName = it.toString())
            reminder?.let { rem -> clickListener.nameChanged(rem, it.toString()) }
        })
        binding.reminderNameText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.cardView.requestFocus()
                clickListener.hideKeyboard()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun setReminderNameText(text: String) {
        if (binding.reminderNameText.text.toString() != text) {
            reminder = reminder?.copy(alarmName = text)
            binding.reminderNameText.setText(text)
            binding.reminderNameText.setSelection(text.length)
        }
    }

    private fun setTime(t: LocalTime) {
        if (reminder?.time != t) reminder = reminder?.copy(time = t)
        val text = t.format(timeDisplayFormatter)
        if (binding.timeText.text != text) binding.timeText.text = text
    }

    private fun listenToTimeButton() {
        binding.timeText.setOnClickListener {
            val picker = TimePickerDialog(
                binding.root.context,
                { _, hour, minute ->
                    val time = LocalTime.of(hour, minute)
                    setTime(time)
                    clickListener.timeChanged(reminder!!, time)
                }, reminder!!.time.hour, reminder!!.time.minute, true
            )
            picker.show()
        }
    }

    private fun listenToCheckboxes() {
        checkboxes.forEach { cb ->
            cb.setOnCheckedChangeListener { _, _ ->
                val boolList = checkboxes.map { x -> x.isChecked }
                val newCheckedDays = CheckedDays.fromList(boolList)
                reminder = reminder?.copy(checkedDays = newCheckedDays)
                reminder?.let { clickListener.daysChanged(it, newCheckedDays) }
            }
        }
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
        fun from(parent: ViewGroup, clickListener: ReminderClickListener): ReminderViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemReminderBinding.inflate(layoutInflater, parent, false)
            return ReminderViewHolder(binding, clickListener)
        }
    }
}

internal class ReminderClickListener(
    private val onDelete: (Reminder) -> Unit,
    private val onDaysChanged: (Reminder, CheckedDays) -> Unit,
    private val onTimeChanged: (Reminder, LocalTime) -> Unit,
    private val onNameChanged: (Reminder, String) -> Unit,
    private val onHideKeyboard: () -> Unit
) {
    fun delete(reminder: Reminder) = onDelete(reminder)
    fun daysChanged(reminder: Reminder, checkedDays: CheckedDays) =
        onDaysChanged(reminder, checkedDays)

    fun timeChanged(reminder: Reminder, localTime: LocalTime) = onTimeChanged(reminder, localTime)
    fun nameChanged(reminder: Reminder, name: String) = onNameChanged(reminder, name)
    fun hideKeyboard() = onHideKeyboard()
}
