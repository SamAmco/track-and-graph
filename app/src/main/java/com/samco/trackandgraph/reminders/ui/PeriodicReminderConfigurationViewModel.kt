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
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.threeten.bp.LocalDateTime
import javax.inject.Inject

interface PeriodicReminderConfigurationViewModel {
    val reminderName: StateFlow<String>
    val starts: StateFlow<LocalDateTime>
    val ends: StateFlow<LocalDateTime?>
    val interval: StateFlow<Int>
    val period: StateFlow<Period>

    fun updateReminderName(name: String)
    fun updateStarts(starts: LocalDateTime)
    fun updateEnds(ends: LocalDateTime?)
    fun updateInterval(interval: Int)
    fun updatePeriod(period: Period)
    fun initializeFromReminder(reminder: Reminder?, params: ReminderParams.PeriodicParams?)
    fun getReminder(): Reminder
    fun reset()
}

@HiltViewModel
class PeriodicReminderConfigurationViewModelImpl @Inject constructor() : ViewModel(), PeriodicReminderConfigurationViewModel {

    private val _reminderName = MutableStateFlow("")
    override val reminderName: StateFlow<String> = _reminderName.asStateFlow()

    private val _starts = MutableStateFlow(LocalDateTime.now().plusHours(1))
    override val starts: StateFlow<LocalDateTime> = _starts.asStateFlow()

    private val _ends = MutableStateFlow<LocalDateTime?>(null)
    override val ends: StateFlow<LocalDateTime?> = _ends.asStateFlow()

    private val _interval = MutableStateFlow(1)
    override val interval: StateFlow<Int> = _interval.asStateFlow()

    private val _period = MutableStateFlow(Period.DAYS)
    override val period: StateFlow<Period> = _period.asStateFlow()

    private var editingReminder: Reminder? = null

    override fun updateReminderName(name: String) {
        _reminderName.value = name
    }

    override fun updateStarts(starts: LocalDateTime) {
        _starts.value = starts
    }

    override fun updateEnds(ends: LocalDateTime?) {
        _ends.value = ends
    }

    override fun updateInterval(interval: Int) {
        _interval.value = interval.coerceAtLeast(1)
    }

    override fun updatePeriod(period: Period) {
        _period.value = period
    }

    override fun initializeFromReminder(reminder: Reminder?, params: ReminderParams.PeriodicParams?) {
        editingReminder = reminder
        
        if (params != null) {
            _reminderName.value = reminder?.reminderName ?: ""
            _starts.value = params.starts
            _ends.value = params.ends
            _interval.value = params.interval
            _period.value = params.period
        }
    }

    override fun getReminder(): Reminder {
        val params = ReminderParams.PeriodicParams(
            starts = _starts.value,
            ends = _ends.value,
            interval = _interval.value,
            period = _period.value
        )

        return editingReminder?.copy(
            reminderName = _reminderName.value,
            params = params
        ) ?: Reminder(
            id = 0L, // Will be assigned by database
            displayIndex = 0, // Will be assigned by interactor
            reminderName = _reminderName.value,
            groupId = null,
            featureId = null,
            params = params
        )
    }

    override fun reset() {
        _reminderName.value = ""
        _starts.value = LocalDateTime.now().plusHours(1)
        _ends.value = null
        _interval.value = 1
        _period.value = Period.DAYS
        editingReminder = null
    }
}
