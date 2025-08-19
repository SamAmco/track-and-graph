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
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.data.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime
import java.util.Collections
import javax.inject.Inject

interface RemindersViewModel {
    val currentReminders: StateFlow<List<ReminderViewData>>
    val remindersChanged: StateFlow<Boolean>
    val loading: StateFlow<Boolean>

    fun saveChanges()
    fun addReminder(defaultName: String)
    fun deleteReminder(reminderViewData: ReminderViewData)
    fun moveItem(from: Int, to: Int)
}

@OptIn(FlowPreview::class)
@HiltViewModel
class RemindersViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val alarmInteractor: AlarmInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), RemindersViewModel {

    private val _currentReminders = MutableStateFlow<List<ReminderViewData>>(emptyList())
    override val currentReminders: StateFlow<List<ReminderViewData>> = _currentReminders.asStateFlow()

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
                _currentReminders.value = allReminders.map { ReminderViewData.fromReminder(it) }
                _loading.value = false
            }
        }

        // Observe changes to the current reminders list and set up state observation
        viewModelScope.launch {
            _currentReminders.collectLatest { reminders ->
                reminders
                    .map { it.stateChanges }
                    .merge()
                    .debounce(50)
                    .collect { onRemindersUpdated() }
            }
        }
    }

    override fun saveChanges() {
        viewModelScope.launch(io) {
            _loading.value = true
            _currentReminders.value.let { reminders ->
                val withDisplayIndices = reminders.mapIndexed { index, reminderViewData ->
                    reminderViewData.toReminder().copy(displayIndex = index)
                }
                dataInteractor.updateReminders(withDisplayIndices)
                alarmInteractor.syncAlarms()
                val allReminders = dataInteractor.getAllRemindersSync()
                savedReminders.clear()
                savedReminders.addAll(allReminders)
                _remindersChanged.value = false
                _currentReminders.value = allReminders.map { ReminderViewData.fromReminder(it) }
            }
            _loading.value = false
        }
    }

    private fun onRemindersUpdated() {
        val a = savedReminders.toList() // Create a snapshot to avoid concurrent modification
        val b = currentReminders.value.map { it.toReminder() }
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
        _currentReminders.value = _currentReminders.value.plus(ReminderViewData.fromReminder(newReminder))
        onRemindersUpdated()
    }

    private fun getNextDisplayIndex(): Int {
        return _currentReminders.value.let {
            if (it.isEmpty()) 0
            else it.maxOf { r -> r.displayIndex } + 1
        }
    }

    override fun deleteReminder(reminderViewData: ReminderViewData) {
        _currentReminders.value = _currentReminders.value.filter { it.id != reminderViewData.id }
        onRemindersUpdated()
    }

    override fun moveItem(from: Int, to: Int) {
        if (from == to) return
        _currentReminders.value = _currentReminders.value.toMutableList()
            .apply { add(to, removeAt(from)) }
        onRemindersUpdated()
    }
}