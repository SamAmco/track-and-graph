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
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import javax.inject.Inject

interface MonthDayReminderConfigurationViewModel {
    val reminderName: StateFlow<String>
    val selectedTime: StateFlow<LocalTime>
    val occurrence: StateFlow<MonthDayOccurrence>
    val dayType: StateFlow<MonthDayType>
    val endsEnabled: StateFlow<Boolean>
    val ends: StateFlow<LocalDateTime>

    fun updateReminderName(name: String)
    fun updateSelectedTime(time: LocalTime)
    fun updateOccurrence(occurrence: MonthDayOccurrence)
    fun updateDayType(dayType: MonthDayType)
    fun updateEndsEnabled(enabled: Boolean)
    fun updateEnds(ends: LocalDateTime)
    fun getReminder(): Reminder
    fun initializeFromReminder(reminder: Reminder?, params: ReminderParams.MonthDayParams?)
    fun reset()
}

@HiltViewModel
class MonthDayReminderConfigurationViewModelImpl @Inject constructor() :
    ViewModel(), MonthDayReminderConfigurationViewModel {

    private val _reminderName = MutableStateFlow("")
    override val reminderName: StateFlow<String> = _reminderName.asStateFlow()

    private val _selectedTime = MutableStateFlow(LocalTime.of(9, 0))
    override val selectedTime: StateFlow<LocalTime> = _selectedTime.asStateFlow()

    private val _occurrence = MutableStateFlow(MonthDayOccurrence.FIRST)
    override val occurrence: StateFlow<MonthDayOccurrence> = _occurrence.asStateFlow()

    private val _dayType = MutableStateFlow(MonthDayType.MONDAY)
    override val dayType: StateFlow<MonthDayType> = _dayType.asStateFlow()

    private val _endsEnabled = MutableStateFlow(false)
    override val endsEnabled: StateFlow<Boolean> = _endsEnabled.asStateFlow()

    private val _ends = MutableStateFlow<LocalDateTime>(LocalDateTime.now())
    override val ends: StateFlow<LocalDateTime> = _ends.asStateFlow()

    private var editingReminder: Reminder? = null

    override fun updateReminderName(name: String) {
        _reminderName.value = name
    }

    override fun updateSelectedTime(time: LocalTime) {
        _selectedTime.value = time
    }

    override fun updateOccurrence(occurrence: MonthDayOccurrence) {
        _occurrence.value = occurrence
    }

    override fun updateDayType(dayType: MonthDayType) {
        _dayType.value = dayType
    }

    override fun updateEndsEnabled(enabled: Boolean) {
        _endsEnabled.value = enabled
    }

    override fun updateEnds(ends: LocalDateTime) {
        _ends.value = ends
    }

    override fun getReminder(): Reminder {
        val params = ReminderParams.MonthDayParams(
            time = _selectedTime.value,
            occurrence = _occurrence.value,
            dayType = _dayType.value,
            ends = if (endsEnabled.value) _ends.value else null
        )

        return editingReminder?.copy(
            reminderName = _reminderName.value,
            params = params
        ) ?: Reminder(
            id = 0L,
            displayIndex = 0,
            reminderName = _reminderName.value,
            groupId = null,
            featureId = null,
            params = params
        )
    }

    override fun initializeFromReminder(
        reminder: Reminder?,
        params: ReminderParams.MonthDayParams?
    ) {
        editingReminder = reminder

        if (reminder != null) {
            _reminderName.value = reminder.reminderName
        }
        if (params != null) {
            _selectedTime.value = params.time
            _occurrence.value = params.occurrence
            _dayType.value = params.dayType
            _endsEnabled.value = params.ends != null
            _ends.value = params.ends ?: LocalDateTime.now()
        }
    }

    override fun reset() {
        editingReminder = null
        _reminderName.value = ""
        _selectedTime.value = LocalTime.of(9, 0)
        _occurrence.value = MonthDayOccurrence.FIRST
        _endsEnabled.value = false
        _dayType.value = MonthDayType.MONDAY
        _ends.value = LocalDateTime.now()
    }
}
