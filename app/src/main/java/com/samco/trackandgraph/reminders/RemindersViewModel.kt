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

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import org.threeten.bp.LocalTime
import java.util.Collections
import javax.inject.Inject

interface RemindersViewModel {
    val currentReminders: StateFlow<List<ReminderViewData>>
    val remindersChanged: StateFlow<Boolean>
    val loading: StateFlow<Boolean>
    val lazyListState: LazyListState
    val scrollToNewItem: Flow<Int>

    fun saveChanges()
    fun addReminder(defaultName: String)
    fun deleteReminder(reminderViewData: ReminderViewData)
    fun moveItem(from: Int, to: Int)
}

@OptIn(FlowPreview::class)
@HiltViewModel
class RemindersViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val reminderInteractor: ReminderInteractor,
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

    override val lazyListState = LazyListState()
    
    private val _scrollToNewItem = Channel<Int>(Channel.UNLIMITED)
    override val scrollToNewItem = _scrollToNewItem.receiveAsFlow()

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
                // Find reminders that were removed (exist in saved but not in current)
                val currentIds = reminders.map { it.id }.toSet()
                val removedReminders = savedReminders.filter { it.id !in currentIds }
                
                // Delete alarms for removed reminders
                removedReminders.forEach { removedReminder ->
                    reminderInteractor.cancelReminderNotifications(removedReminder)
                }
                
                val withDisplayIndices = reminders.mapIndexed { index, reminderViewData ->
                    reminderViewData.toReminder().copy(displayIndex = index)
                }
                dataInteractor.updateReminders(withDisplayIndices)
                reminderInteractor.syncReminderNotifications()
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
            id = getNextReminderId(),
            displayIndex = getNextDisplayIndex(),
            reminderName = defaultName,
            groupId = null,
            featureId = null,
            params = ReminderParams.WeekDayParams(
                time = LocalTime.now(),
                checkedDays = CheckedDays.none()
            ),
        )
        _currentReminders.value = _currentReminders.value.plus(ReminderViewData.fromReminder(newReminder))
        onRemindersUpdated()
        
        // Trigger scroll to the new item (last item in the list)
        val newItemIndex = _currentReminders.value.size - 1
        _scrollToNewItem.trySend(newItemIndex)
    }

    private fun getNextReminderId(): Long {
        // Find the highest ID from both saved reminders (database snapshot) and current reminders (including unsaved)
        val savedMaxId = savedReminders.maxOfOrNull { it.id } ?: 0L
        val currentMaxId = _currentReminders.value.maxOfOrNull { it.id } ?: 0L
        return maxOf(savedMaxId, currentMaxId) + 1L
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