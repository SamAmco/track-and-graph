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

package com.samco.trackandgraph.reminders.ui

import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.threeten.bp.LocalTime
import javax.inject.Inject

interface WeekDayReminderConfigurationViewModel {
    val reminderName: StateFlow<String>
    val selectedTime: StateFlow<LocalTime>
    val checkedDays: StateFlow<CheckedDays>

    fun updateReminderName(name: String)
    fun updateSelectedTime(time: LocalTime)
    fun updateCheckedDays(days: CheckedDays)
    fun getReminder(): Reminder
    fun initializeFromReminder(reminder: Reminder?, params: ReminderParams.WeekDayParams?)
    fun reset()
}

@HiltViewModel
class WeekDayReminderConfigurationViewModelImpl @Inject constructor() :
    ViewModel(), WeekDayReminderConfigurationViewModel {

    private val _reminderName = MutableStateFlow("")
    override val reminderName: StateFlow<String> = _reminderName.asStateFlow()

    private val _selectedTime = MutableStateFlow(LocalTime.of(9, 0))
    override val selectedTime: StateFlow<LocalTime> = _selectedTime.asStateFlow()

    private val _checkedDays = MutableStateFlow(CheckedDays.all())
    override val checkedDays: StateFlow<CheckedDays> = _checkedDays.asStateFlow()

    override fun updateReminderName(name: String) {
        _reminderName.value = name
    }

    override fun updateSelectedTime(time: LocalTime) {
        _selectedTime.value = time
    }

    override fun updateCheckedDays(days: CheckedDays) {
        _checkedDays.value = days
    }

    override fun getReminder(): Reminder {
        return Reminder(
            id = 0L,
            displayIndex = 0,
            reminderName = _reminderName.value,
            groupId = null,
            featureId = null,
            params = ReminderParams.WeekDayParams(
                time = _selectedTime.value,
                checkedDays = _checkedDays.value
            )
        )
    }

    override fun initializeFromReminder(
        reminder: Reminder?,
        params: ReminderParams.WeekDayParams?
    ) {
        if (reminder != null) {
            _reminderName.value = reminder.reminderName
        }
        if (params != null) {
            _selectedTime.value = params.time
            _checkedDays.value = params.checkedDays
        }
    }

    override fun reset() {
        _reminderName.value = ""
        _selectedTime.value = LocalTime.of(9, 0)
        _checkedDays.value = CheckedDays.all()
    }
}
