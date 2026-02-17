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
import com.samco.trackandgraph.data.database.dto.ReminderInput
import com.samco.trackandgraph.data.database.dto.ReminderParams
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import javax.inject.Inject

interface PeriodicReminderConfigurationViewModel {
    val reminderName: StateFlow<String>
    val starts: StateFlow<OffsetDateTime>
    val ends: StateFlow<OffsetDateTime>
    val interval: StateFlow<String>
    val period: StateFlow<Period>
    val hasEndDate: StateFlow<Boolean>

    fun initializeFromReminder(reminder: Reminder?, params: ReminderParams.PeriodicParams?)
    fun updateReminderName(name: String)
    fun updateStarts(starts: OffsetDateTime)
    fun updateEnds(ends: OffsetDateTime)
    fun updateHasEndDate(hasEndDate: Boolean)
    fun updateInterval(interval: String)
    fun updatePeriod(period: Period)
    fun getReminderInput(): ReminderInput
    fun reset()
}

@HiltViewModel
class PeriodicReminderConfigurationViewModelImpl @Inject constructor() : ViewModel(), PeriodicReminderConfigurationViewModel {

    private val _reminderName = MutableStateFlow("")
    override val reminderName: StateFlow<String> = _reminderName.asStateFlow()

    private val _intervalText = MutableStateFlow("1")
    override val interval: StateFlow<String> = _intervalText.asStateFlow()

    private val _period = MutableStateFlow(Period.DAYS)
    override val period: StateFlow<Period> = _period.asStateFlow()
    
    private val _starts = MutableStateFlow(OffsetDateTime.now())
    override val starts: StateFlow<OffsetDateTime> = _starts.asStateFlow()
    
    private val _ends = MutableStateFlow<OffsetDateTime>(OffsetDateTime.now())
    override val ends: StateFlow<OffsetDateTime> = _ends.asStateFlow()
    
    private val _hasEndDate = MutableStateFlow(false)
    override val hasEndDate: StateFlow<Boolean> = _hasEndDate.asStateFlow()

    // Helper methods for conversion
    private fun localDateTimeToOffset(localDateTime: LocalDateTime): OffsetDateTime {
        return localDateTime.atZone(ZoneId.systemDefault()).toOffsetDateTime()
    }
    
    private fun offsetDateTimeToLocal(offsetDateTime: OffsetDateTime): LocalDateTime {
        return offsetDateTime.atZoneSameInstant(ZoneId.systemDefault()).toLocalDateTime()
    }

    override fun updateReminderName(name: String) {
        _reminderName.value = name
    }

    override fun updateStarts(starts: OffsetDateTime) {
        _starts.value = starts
    }
    
    override fun updateEnds(ends: OffsetDateTime) {
        _ends.value = ends
    }
    
    override fun updateHasEndDate(hasEndDate: Boolean) {
        _hasEndDate.value = hasEndDate
    }

    override fun updateInterval(interval: String) {
        _intervalText.value = interval
    }

    override fun updatePeriod(period: Period) {
        _period.value = period
    }

    override fun initializeFromReminder(reminder: Reminder?, params: ReminderParams.PeriodicParams?) {
        if (reminder != null) {
            _reminderName.value = reminder.reminderName
        }
        if (params != null) {
            _starts.value = localDateTimeToOffset(params.starts)
            _ends.value = params.ends?.let { localDateTimeToOffset(it) } ?: OffsetDateTime.now()
            _hasEndDate.value = params.ends != null
            _intervalText.value = params.interval.toString()
            _period.value = params.period
        }
    }

    override fun getReminderInput(): ReminderInput {
        return ReminderInput(
            reminderName = _reminderName.value,
            featureId = null,
            params = ReminderParams.PeriodicParams(
                starts = offsetDateTimeToLocal(_starts.value),
                ends = if (_hasEndDate.value) offsetDateTimeToLocal(_ends.value) else null,
                // If the user enters a decimal floor it to the nearest int
                interval = (_intervalText.value.toDoubleOrNull()?.toInt() ?: 1).coerceAtLeast(1),
                period = _period.value
            )
        )
    }

    override fun reset() {
        _reminderName.value = ""
        val defaultStart = LocalDateTime.now()
        val defaultEnd = LocalDateTime.now()
        _starts.value = localDateTimeToOffset(defaultStart)
        _ends.value = localDateTimeToOffset(defaultEnd)
        _hasEndDate.value = false
        _intervalText.value = "1"
        _period.value = Period.DAYS
    }
}
