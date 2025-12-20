/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.reminders.ui

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.MainDispatcher
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.reminders.ReminderInteractor
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.Job
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import timber.log.Timber
import javax.inject.Inject

interface AddReminderViewModel {
    fun loadStateForReminder(editReminderId: Long?)

    val loading: StateFlow<Boolean>
    val editMode: StateFlow<Boolean>
    val editingReminder: StateFlow<Reminder?>
    val onComplete: ReceiveChannel<Unit>

    fun upsertReminder(reminder: Reminder)
}

@HiltViewModel
class AddReminderViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val reminderInteractor: ReminderInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
) : ViewModel(), AddReminderViewModel {

    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    private var currentLoadingJob: Job? = null
    private val _editingReminder = MutableStateFlow<Reminder?>(null)
    
    override val editingReminder: StateFlow<Reminder?> = _editingReminder.asStateFlow()
    override val editMode: StateFlow<Boolean> = _editingReminder
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    override val onComplete: Channel<Unit> = Channel()

    override fun loadStateForReminder(editReminderId: Long?) {
        currentLoadingJob?.cancel()
        currentLoadingJob = viewModelScope.launch(io) {
            withContext(ui) { _loading.value = true }
            if (editReminderId == null) {
                _editingReminder.value = null
                withContext(ui) { _loading.value = false }
                return@launch
            }
            _editingReminder.value = dataInteractor.getReminderById(editReminderId)
            withContext(ui) { _loading.value = false }
        }
    }

    override fun upsertReminder(reminder: Reminder) {
        viewModelScope.launch(io) {
            try {
                currentLoadingJob?.join()
                withContext(ui) { _loading.value = true }

                val editingReminder = _editingReminder.value

                if (editingReminder != null) {
                    updateReminder(editingReminder, reminder)
                } else {
                    insertReminder(reminder)
                }
            } catch (t: Throwable) {
                Timber.e(t)
            } finally {
                withContext(ui) {
                    _loading.value = false
                    onComplete.send(Unit)
                }
            }
        }
    }

    private suspend fun updateReminder(oldReminder: Reminder, newReminder: Reminder) {
        // Update existing reminder
        val updatedReminder = newReminder.copy(
            id = oldReminder.id,
            displayIndex = oldReminder.displayIndex,
        )
        
        try {
            reminderInteractor.scheduleNext(updatedReminder)
            dataInteractor.updateReminder(updatedReminder)
        } catch (t: Throwable) {
            Timber.e(t)
            reminderInteractor.cancelReminderNotifications(updatedReminder)
        }
    }

    private suspend fun insertReminder(reminder: Reminder) {
        // Get existing reminders and shift their display indices down
        val existingReminders = dataInteractor.getAllRemindersSync()
        val shiftedReminders = existingReminders.map { existingReminder ->
            existingReminder.copy(displayIndex = existingReminder.displayIndex + 1)
        }

        // Update existing reminders with shifted display indices
        shiftedReminders.forEach { shiftedReminder ->
            dataInteractor.updateReminder(shiftedReminder)
        }

        // Insert new reminder
        val insertedId = dataInteractor.insertReminder(reminder)
        val insertedReminder = reminder.copy(id = insertedId)

        // Schedule next notification
        reminderInteractor.scheduleNext(insertedReminder)
    }

    override fun onCleared() {
        super.onCleared()
        currentLoadingJob?.cancel()
        onComplete.close()
    }
}