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

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.database.dto.CheckedDays
import com.samco.trackandgraph.base.database.dto.Reminder
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime
import java.util.Collections
import javax.inject.Inject

interface RemindersViewModel {
    val currentReminders: StateFlow<List<Reminder>>
    val remindersChanged: StateFlow<Boolean>
    val loading: StateFlow<Boolean>

    fun saveChanges()
    fun addReminder(defaultName: String)
    fun deleteReminder(reminder: Reminder)
    fun adjustDisplayIndexes(indexUpdate: List<Reminder>)
    fun daysChanged(reminder: Reminder, checkedDays: CheckedDays)
    fun onTimeChanged(reminder: Reminder, localTime: LocalTime)
    fun onNameChanged(reminder: Reminder, name: String)
}

@HiltViewModel
class RemindersViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val alarmInteractor: AlarmInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), RemindersViewModel {

    private val _currentReminders = MutableStateFlow<List<Reminder>>(emptyList())
    override val currentReminders: StateFlow<List<Reminder>> = _currentReminders.asStateFlow()

    private val savedReminders: MutableList<Reminder> = Collections.synchronizedList(mutableListOf())

    private val _remindersChanged = MutableStateFlow(false)
    override val remindersChanged: StateFlow<Boolean> = _remindersChanged.asStateFlow()

    private val _loading = MutableStateFlow(true)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    init {
        viewModelScope.launch(io) {
            withContext(ui) { _loading.value = true }
            val allReminders = dataInteractor.getAllRemindersSync()
            savedReminders.addAll(allReminders)
            withContext(ui) {
                _currentReminders.value = allReminders
                _loading.value = false
            }
        }
    }

    override fun saveChanges() {
        viewModelScope.launch(io) {
            withContext(ui) { _loading.value = true }
            _currentReminders.value.let {
                val withDisplayIndices = it
                    .mapIndexed { index, reminder -> reminder.copy(displayIndex = index) }
                dataInteractor.updateReminders(withDisplayIndices)
                alarmInteractor.syncAlarms()
                val allReminders = dataInteractor.getAllRemindersSync()
                savedReminders.clear()
                savedReminders.addAll(allReminders)
                withContext(ui) {
                    // Copy so they're not the same object because we
                    // want to be able to update them independently
                    _currentReminders.value = allReminders.toMutableList()
                    onRemindersUpdated()
                }
            }
            withContext(ui) { _loading.value = false }
        }
    }

    private fun onRemindersUpdated() {
        val a = savedReminders.toList() // Create a snapshot to avoid concurrent modification
        val b = currentReminders.value
        //If the two lists are not equal we have an update
        _remindersChanged.value = !(a.size == b.size && a.zip(b).all { it.first == it.second })
    }

    override fun addReminder(defaultName: String) {
        val newReminder = Reminder(
            //We just want a unique ID for now,
            // this won't be used when it's added to the db
            System.nanoTime(),
            getNextDisplayIndex(),
            defaultName,
            LocalTime.now(),
            CheckedDays.none()
        )
        _currentReminders.value = _currentReminders.value.plus(newReminder)
        onRemindersUpdated()
    }

    private fun getNextDisplayIndex(): Int {
        return _currentReminders.value.let {
            if (it.isEmpty()) 0
            else it.maxOf { r -> r.displayIndex } + 1
        }
    }

    override fun deleteReminder(reminder: Reminder) {
        _currentReminders.value = _currentReminders.value.filter { it.id != reminder.id }
        onRemindersUpdated()
    }

    override fun adjustDisplayIndexes(indexUpdate: List<Reminder>) {
        _currentReminders.value = indexUpdate.mapIndexed { i, r ->
            _currentReminders.value.firstOrNull { it.id == r.id }?.copy(displayIndex = i)
        }.filterNotNull()
        onRemindersUpdated()
    }

    override fun daysChanged(reminder: Reminder, checkedDays: CheckedDays) =
        updateReminder(reminder, reminder.copy(checkedDays = checkedDays))

    override fun onTimeChanged(reminder: Reminder, localTime: LocalTime) =
        updateReminder(reminder, reminder.copy(time = localTime))

    override fun onNameChanged(reminder: Reminder, name: String) =
        updateReminder(reminder, reminder.copy(alarmName = name))

    private fun updateReminder(from: Reminder, to: Reminder) {
        val mutable = _currentReminders.value.toMutableList()
        val index = mutable.indexOfFirst { it.id == from.id }
        if (index >= 0) {
            mutable.removeAt(index)
            mutable.add(index, to)
        }
        _currentReminders.value = mutable
        onRemindersUpdated()
    }
}