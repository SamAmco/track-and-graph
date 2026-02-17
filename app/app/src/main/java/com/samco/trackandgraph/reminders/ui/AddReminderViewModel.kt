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
import com.samco.trackandgraph.data.database.dto.ReminderCreateRequest
import com.samco.trackandgraph.data.database.dto.ReminderInput
import com.samco.trackandgraph.data.database.dto.ReminderUpdateRequest
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
    /**
     * Initialize the view model for creating or editing a reminder.
     *
     * @param editReminderId If non-null, load this reminder for editing. If null, prepare for creating.
     * @param groupId The group to create the reminder in (only used when editReminderId is null).
     *                Pass null for reminders that appear on the main reminders screen.
     */
    fun loadStateForReminder(editReminderId: Long?, groupId: Long? = null)

    val loading: StateFlow<Boolean>
    val editMode: StateFlow<Boolean>
    val editingReminder: StateFlow<Reminder?>
    val hasAnyFeatures: StateFlow<Boolean>
    val onComplete: ReceiveChannel<Unit>

    fun saveReminder(input: ReminderInput)
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
    private var createGroupId: Long? = null

    override val editingReminder: StateFlow<Reminder?> = _editingReminder.asStateFlow()
    override val editMode: StateFlow<Boolean> = _editingReminder
        .map { it != null }
        .stateIn(viewModelScope, SharingStarted.Eagerly, false)

    private val _hasAnyFeatures = MutableStateFlow(false)
    override val hasAnyFeatures: StateFlow<Boolean> = _hasAnyFeatures.asStateFlow()

    override val onComplete: Channel<Unit> = Channel()

    init {
        viewModelScope.launch(io) {
            _hasAnyFeatures.value = dataInteractor.hasAnyFeatures()
        }
    }

    override fun loadStateForReminder(editReminderId: Long?, groupId: Long?) {
        currentLoadingJob?.cancel()
        currentLoadingJob = viewModelScope.launch(io) {
            withContext(ui) { _loading.value = true }
            if (editReminderId == null) {
                _editingReminder.value = null
                createGroupId = groupId
                withContext(ui) { _loading.value = false }
                return@launch
            }
            _editingReminder.value = dataInteractor.getReminderById(editReminderId)
            createGroupId = null // Not used when editing
            withContext(ui) { _loading.value = false }
        }
    }

    override fun saveReminder(input: ReminderInput) {
        viewModelScope.launch(io) {
            try {
                currentLoadingJob?.join()
                withContext(ui) { _loading.value = true }

                val editingReminder = _editingReminder.value

                if (editingReminder != null) {
                    updateReminder(editingReminder, input)
                } else {
                    createReminder(input)
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

    private suspend fun updateReminder(existingReminder: Reminder, input: ReminderInput) {
        // Build the updated reminder for scheduling
        val updatedReminder = existingReminder.copy(
            reminderName = input.reminderName,
            featureId = input.featureId,
            params = input.params
        )

        try {
            reminderInteractor.scheduleNext(updatedReminder)
            dataInteractor.updateReminder(
                ReminderUpdateRequest(
                    id = existingReminder.id,
                    reminderName = input.reminderName,
                    featureId = input.featureId,
                    params = input.params
                )
            )
        } catch (t: Throwable) {
            Timber.e(t)
            reminderInteractor.cancelReminderNotifications(updatedReminder)
        }
    }

    private suspend fun createReminder(input: ReminderInput) {
        // Create the new reminder (data layer handles display index shifting)
        val insertedId = dataInteractor.createReminder(
            ReminderCreateRequest(
                reminderName = input.reminderName,
                groupId = createGroupId,
                featureId = input.featureId,
                params = input.params
            )
        )

        // Fetch the created reminder for scheduling
        val insertedReminder = dataInteractor.getReminderById(insertedId)
            ?: error("Failed to fetch newly created reminder with id $insertedId")
        reminderInteractor.scheduleNext(insertedReminder)
    }

    override fun onCleared() {
        super.onCleared()
        currentLoadingJob?.cancel()
        onComplete.close()
    }
}