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
import android.content.Context
import android.view.LayoutInflater
import android.view.ViewGroup
import android.view.inputmethod.EditorInfo
import androidx.core.widget.addTextChangedListener
import androidx.recyclerview.widget.DiffUtil
import androidx.recyclerview.widget.RecyclerView
import com.samco.trackandgraph.base.database.entity.CheckedDays
import com.samco.trackandgraph.base.database.entity.Reminder
import com.samco.trackandgraph.databinding.ListItemReminderBinding
import com.samco.trackandgraph.ui.OrderedListAdapter
import org.threeten.bp.LocalTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter

private val getIdForReminder = { r: Reminder -> r.id }

internal class ReminderListAdapter(private val clickListener: ReminderClickListener, private val context: Context)
    : OrderedListAdapter<Reminder, ReminderViewHolder>(getIdForReminder, ReminderDiffCallback()) {
    override fun onBindViewHolder(holder: ReminderViewHolder, position: Int) {
        holder.bind(getItem(position), clickListener)
    }

    override fun onCreateViewHolder(parent: ViewGroup, viewType: Int): ReminderViewHolder {
        return ReminderViewHolder.from(parent, context)
    }
}

internal class ReminderViewHolder private constructor(
    private val binding: ListItemReminderBinding,
    private val context: Context) : RecyclerView.ViewHolder(binding.root) {

    private val timeDisplayFormatter: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    private var clickListener: ReminderClickListener? = null
    private var dropElevation = 0f
    private var reminder: Reminder? = null

    fun bind(reminder: Reminder, clickListener: ReminderClickListener) {
        this.reminder = reminder
        this.clickListener = clickListener
        dropElevation = binding.cardView.cardElevation

        binding.deleteButton.setOnClickListener { clickListener.delete(reminder) }
        listenToCheckboxes()
        listenToTimeButton()
        listenToName()
    }

    private fun listenToName() {
        binding.reminderNameText.setText(reminder!!.alarmName)
        binding.reminderNameText.addTextChangedListener {
            clickListener?.nameChanged(reminder!!, it.toString())
        }
        binding.reminderNameText.setOnEditorActionListener { _, actionId, _ ->
            if (actionId == EditorInfo.IME_ACTION_DONE) {
                binding.cardView.requestFocus()
                clickListener?.hideKeyboard()
                return@setOnEditorActionListener true
            }
            return@setOnEditorActionListener false
        }
    }

    private fun listenToTimeButton() {
        val setTimeText = {t: LocalTime ->
            binding.timeText.text = t.format(timeDisplayFormatter)
        }
        setTimeText(reminder!!.time)
        binding.timeText.setOnClickListener {
            val picker = TimePickerDialog(context,
                TimePickerDialog.OnTimeSetListener { _, hour, minute ->
                    val time = LocalTime.of(hour, minute)
                    clickListener?.timeChanged(reminder!!, time)
                    setTimeText(time)
                }, reminder!!.time.hour, reminder!!.time.minute, true
            )
            picker.show()
        }
    }

    private fun listenToCheckboxes() {
        val checkboxes = listOf(binding.monBox, binding.tueBox, binding.wedBox,
            binding.thuBox, binding.friBox, binding.satBox, binding.sunBox)
        checkboxes.forEachIndexed { i, cb ->
            cb.setOnCheckedChangeListener(null)
            cb.isChecked = reminder!!.checkedDays.toList()[i]
            cb.setOnCheckedChangeListener { _, _ ->
                val boolList = checkboxes.map { x -> x.isChecked }
                val newCheckedDays = CheckedDays.fromList(boolList)
                clickListener?.daysChanged(reminder!!, newCheckedDays)
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
        fun from(parent: ViewGroup, context: Context): ReminderViewHolder {
            val layoutInflater = LayoutInflater.from(parent.context)
            val binding = ListItemReminderBinding.inflate(layoutInflater, parent, false)
            return ReminderViewHolder(binding, context)
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


internal class ReminderClickListener(
    private val onDelete: (Reminder) -> Unit,
    private val onDaysChanged: (Reminder, CheckedDays) -> Unit,
    private val onTimeChanged: (Reminder, LocalTime) -> Unit,
    private val onNameChanged: (Reminder, String) -> Unit,
    private val onHideKeyboard: () -> Unit
) {
    fun delete(reminder: Reminder) = onDelete(reminder)
    fun daysChanged(reminder: Reminder, checkedDays: CheckedDays) = onDaysChanged(reminder, checkedDays)
    fun timeChanged(reminder: Reminder, localTime: LocalTime) = onTimeChanged(reminder, localTime)
    fun nameChanged(reminder: Reminder, name: String) = onNameChanged(reminder, name)
    fun hideKeyboard() = onHideKeyboard()
}
