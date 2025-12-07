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
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

interface AddReminderViewModel {
    val loading: StateFlow<Boolean>
    val onAddComplete: ReceiveChannel<Unit>

    fun addReminder(reminder: Reminder)
}

@HiltViewModel
class AddReminderViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val reminderInteractor: ReminderInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), AddReminderViewModel {

    private val _loading = MutableStateFlow(false)
    override val loading: StateFlow<Boolean> = _loading.asStateFlow()

    override val onAddComplete: Channel<Unit> = Channel()

    override fun addReminder(reminder: Reminder) {
        viewModelScope.launch(io) {
            withContext(ui) { _loading.value = true }

            try {
                withContext(io) {
                    // Get existing reminders and shift their display indices down
                    val existingReminders = dataInteractor.getAllRemindersSync()
                    val shiftedReminders = existingReminders.map { existingReminder ->
                        existingReminder.copy(displayIndex = existingReminder.displayIndex + 1)
                    }

                    // Insert new reminder first
                    val insertedId = dataInteractor.insertReminder(reminder)
                    val insertedReminder = reminder.copy(id = insertedId)

                    // Update existing reminders with shifted display indices
                    shiftedReminders.forEach { shiftedReminder ->
                        dataInteractor.updateReminder(shiftedReminder)
                    }
                    reminderInteractor.scheduleNext(insertedReminder)
                }
            } finally {
                withContext(ui) {
                    _loading.value = false
                    onAddComplete.send(Unit)
                }
            }
        }
    }
}