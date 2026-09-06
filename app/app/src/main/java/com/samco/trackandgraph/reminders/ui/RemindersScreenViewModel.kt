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

import androidx.compose.foundation.lazy.LazyListState
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.data.database.dto.ComponentDeleteRequest
import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderDisplayOrderData
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataUpdateType
import com.samco.trackandgraph.reminders.ReminderInteractor
import com.samco.trackandgraph.util.ComponentPathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.FlowPreview
import kotlinx.coroutines.TimeoutCancellationException
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.SharingStarted
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.debounce
import kotlinx.coroutines.flow.filter
import kotlinx.coroutines.flow.filterIsInstance
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flatMapLatest
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.merge
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.flow.stateIn
import kotlinx.coroutines.launch
import kotlinx.coroutines.withTimeout
import timber.log.Timber
import javax.inject.Inject

interface RemindersScreenViewModel {
    val currentReminders: StateFlow<List<ReminderViewData>>
    val loading: StateFlow<Boolean>
    val lazyListState: LazyListState
    val scrollToTopEvents: ReceiveChannel<Unit>

    fun deleteReminder(reminderViewData: ReminderViewData)
    fun duplicateReminder(reminderViewData: ReminderViewData)
    fun moveReminderToGroup(reminderId: Long, groupId: Long)
    fun moveItem(from: Int, to: Int)

    fun onDragStart()
    fun onDragSwap(from: Int, to: Int)
    fun onDragEnd()
}

internal data class ReminderScreenItem(
    val reminder: Reminder,
    val groupItemId: Long,
)

internal fun buildReminderScreenItems(
    reminders: List<Reminder>,
    globalIndices: List<GroupChildDisplayIndex>,
): List<ReminderScreenItem> {
    val placementByReminderId = globalIndices.associateBy { it.id }
    return reminders.mapNotNull { reminder ->
        placementByReminderId[reminder.id]?.let { placement ->
            ReminderScreenItem(
                reminder = reminder,
                groupItemId = placement.groupItemId,
            ) to placement.displayIndex
        }
    }
        .sortedBy { (_, displayIndex) -> displayIndex }
        .map { (item, _) -> item }
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RemindersScreenViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val reminderInteractor: ReminderInteractor,
    private val reminderViewDataFactory: ReminderViewDataFactory,
    @IODispatcher private val io: CoroutineDispatcher,
) : ViewModel(), RemindersScreenViewModel {

    override val lazyListState = LazyListState()

    override val scrollToTopEvents = Channel<Unit>(1)

    // IDs of reminders created via duplicate — excluded from scroll-to-top
    private val duplicatedReminderIds = mutableSetOf<Long>()

    private val isDragging = MutableStateFlow(false)
    private val temporaryReminders = MutableStateFlow<List<ReminderViewData>>(emptyList())

    // Observable pattern: listen for reminder updates and reload data.
    @OptIn(FlowPreview::class)
    private val allReminders: StateFlow<LoadingState> =
        merge(
            dataInteractor.getDataUpdateEvents()
                .filter {
                    it is DataUpdateType.Reminder ||
                    it is DataUpdateType.ReminderScreenDisplayOrder ||
                    it is DataUpdateType.GroupUpdated ||
                    it is DataUpdateType.GroupDeleted ||
                    it is DataUpdateType.SymlinkCreated ||
                    it is DataUpdateType.Unknown
                }
                .map { },
            reminderInteractor.schedulingEvents.map { }
        )
            .debounce(100)
            .onStart { emit(Unit) } // Emit initial event to load data
            .flatMapLatest {
                flow {
                    val reminders = dataInteractor.getAllRemindersSync()
                    val globalIndices = dataInteractor.getDisplayIndicesForRemindersScreen()
                    val pathProvider = ComponentPathProvider(dataInteractor.getGroupGraphSync())
                    val viewData = buildReminderScreenItems(reminders, globalIndices).map { item ->
                        reminderViewDataFactory.create(
                            item.reminder,
                            item.groupItemId,
                            path = pathProvider
                                .getAllPathsForReminder(item.reminder.id)
                                .firstOrNull(),
                        )
                    }
                    emit(LoadingState.Loaded(viewData))
                }
            }
            .flowOn(io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, LoadingState.Loading)

    override val currentReminders: StateFlow<List<ReminderViewData>> = isDragging
        .flatMapLatest { dragging ->
            if (dragging) temporaryReminders
            else allReminders.filterIsInstance<LoadingState.Loaded>().map { it.data }
        }
        .stateIn(viewModelScope, SharingStarted.Lazily, emptyList())

    override val loading: StateFlow<Boolean> = allReminders
        .map { it !is LoadingState.Loaded }
        .stateIn(viewModelScope, SharingStarted.Lazily, true)

    init {
        // Emit scroll-to-top when a genuinely new (non-duplicate) reminder is added
        var previousIds = emptySet<Long>()
        viewModelScope.launch {
            currentReminders.collect { reminders ->
                val currentIds = reminders.map { it.id }.toSet()
                val newIds = currentIds - previousIds
                if (newIds.isNotEmpty() && previousIds.isNotEmpty()) {
                    val isAllDuplicates = synchronized(duplicatedReminderIds) {
                        duplicatedReminderIds.containsAll(newIds).also {
                            duplicatedReminderIds.removeAll(newIds)
                        }
                    }
                    if (!isAllDuplicates) scrollToTopEvents.send(Unit)
                }
                previousIds = currentIds
            }
        }
    }

    override fun deleteReminder(reminderViewData: ReminderViewData) {
        viewModelScope.launch(io) {
            reminderViewData.reminderDto?.let { reminder ->
                reminderInteractor.cancelReminderNotifications(reminder.id)
                dataInteractor.deleteReminder(
                    ComponentDeleteRequest(
                        groupItemId = reminderViewData.groupItemId,
                        deleteEverywhere = true,
                    )
                )
            }
        }
    }

    override fun duplicateReminder(reminderViewData: ReminderViewData) {
        viewModelScope.launch(io) {
            val created = dataInteractor.duplicateReminder(reminderViewData.groupItemId)
            synchronized(duplicatedReminderIds) { duplicatedReminderIds.add(created.componentId) }
            val newReminder = dataInteractor.getReminderById(created.componentId)
            if (newReminder != null) {
                reminderInteractor.scheduleNext(newReminder)
            }
        }
    }

    override fun moveReminderToGroup(reminderId: Long, groupId: Long) {
        viewModelScope.launch(io) {
            dataInteractor.moveReminderToGroup(reminderId, groupId)
        }
    }

    override fun moveItem(from: Int, to: Int) {
        if (from == to) return
        onDragStart()
        onDragSwap(from, to)
        onDragEnd()
    }

    override fun onDragStart() {
        if (isDragging.value) return
        // Create a temporary copy of the current reminders for faster
        // responsive mutations while dragging
        temporaryReminders.value = currentReminders.value.toMutableList()
        isDragging.value = true
    }

    override fun onDragSwap(from: Int, to: Int) {
        if (!isDragging.value) return
        if (from !in temporaryReminders.value.indices) return
        if (to !in temporaryReminders.value.indices) return
        // Swap the temporary reminders in place synchronously
        temporaryReminders.value = temporaryReminders.value.toMutableList()
            .apply { add(to, removeAt(from)) }
    }

    override fun onDragEnd() {
        if (!isDragging.value) return

        viewModelScope.launch {
            // Build display order data from the temporary (reordered) list
            val orders = temporaryReminders.value.mapIndexed { index, reminderViewData ->
                ReminderDisplayOrderData(
                    id = reminderViewData.id,
                    displayIndex = index
                )
            }

            // Update all display indices in one call (null groupId = reminders screen)
            dataInteractor.updateReminderScreenDisplayOrder(orders = orders)

            // Wait until allReminders reflects the new order before switching back
            // to the real reminders. We must wait on allReminders (not just dbDisplayIndices)
            // because currentReminders switches to allReminders when isDragging becomes false.
            // If allReminders still has the old order (due to its 100ms debounce), the UI
            // would briefly show items in the old positions before snapping to the new ones.
            val expectedOrder = orders.sortedBy { it.displayIndex }.map { it.id }
            try {
                withTimeout(500) {
                    allReminders.filterIsInstance<LoadingState.Loaded>().first { loaded ->
                        loaded.data.map { it.id } == expectedOrder
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e(
                    e,
                    "The reminder list did not update within 500ms. Drag and drop update may have failed."
                )
            }

            // Switch back to showing the real reminders from the database
            isDragging.value = false
            temporaryReminders.value = emptyList()
        }
    }

    private sealed class LoadingState {
        data object Loading : LoadingState()
        data class Loaded(val data: List<ReminderViewData>) : LoadingState()
    }
}
