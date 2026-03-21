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
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.interactor.DataUpdateType
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.time.TimeProvider
import com.samco.trackandgraph.reminders.NextScheduled
import com.samco.trackandgraph.reminders.ReminderInteractor
import org.threeten.bp.Instant
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
import kotlinx.coroutines.flow.filterNotNull
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
import org.threeten.bp.LocalDateTime
import timber.log.Timber
import javax.inject.Inject

interface RemindersScreenViewModel {
    val currentReminders: StateFlow<List<ReminderViewData>>
    val loading: StateFlow<Boolean>
    val lazyListState: LazyListState
    val scrollToTopEvents: ReceiveChannel<Unit>

    fun deleteReminder(reminderViewData: ReminderViewData)
    fun duplicateReminder(reminderViewData: ReminderViewData)
    fun moveItem(from: Int, to: Int)

    fun onDragStart()
    fun onDragSwap(from: Int, to: Int)
    fun onDragEnd()
}

@HiltViewModel
@OptIn(ExperimentalCoroutinesApi::class)
class RemindersScreenViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val reminderInteractor: ReminderInteractor,
    private val timeProvider: TimeProvider,
    private val dataSampler: DataSampler,
    @IODispatcher private val io: CoroutineDispatcher,
) : ViewModel(), RemindersScreenViewModel {

    override val lazyListState = LazyListState()

    override val scrollToTopEvents = Channel<Unit>(1)

    // IDs of reminders created via duplicate — excluded from scroll-to-top
    private val duplicatedReminderIds = mutableSetOf<Long>()

    private val isDragging = MutableStateFlow(false)
    private val temporaryReminders = MutableStateFlow<List<ReminderViewData>>(emptyList())

    // Observable pattern: listen for reminder updates and reload data.
    // Display indices are fetched in the same flatMapLatest block as the reminder list so that
    // the sorted result is always consistent — there is no race between two separate flows.
    @OptIn(FlowPreview::class)
    private val allReminders: StateFlow<LoadingState> =
        merge(
            dataInteractor.getDataUpdateEvents()
                .filter {
                    it == DataUpdateType.Reminder ||
                    it is DataUpdateType.ReminderScreenDisplayOrder ||
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
                    val indices = dataInteractor.getDisplayIndicesForRemindersScreen()
                    val groupItemIdByReminderId = indices.associate { it.id to it.groupItemId }
                    val displayIndexByReminderId = indices.associate { it.id to it.displayIndex }
                    val viewData = reminders.map { reminder ->
                        convertToReminderViewData(
                            reminder,
                            groupItemIdByReminderId[reminder.id] ?: 0L,
                        )
                    }
                    emit(LoadingState.Loaded(viewData.sortedBy { displayIndexByReminderId[it.id] ?: Int.MAX_VALUE }))
                }
            }
            .flowOn(io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, LoadingState.Loading)

    /**
     * A flow of display indices from the database for the reminders screen, represented as a
     * map from reminder ID to displayIndex for O(1) lookups during sorting.
     *
     * Used only by [onDragEnd] to confirm the database has persisted the new order before
     * switching back from the temporary drag list to the real data.
     */
    @OptIn(FlowPreview::class)
    private val dbDisplayIndices: StateFlow<List<GroupChildDisplayIndex>?> =
        dataInteractor.getDataUpdateEvents()
            .filter {
                it is DataUpdateType.ReminderScreenDisplayOrder ||
                it is DataUpdateType.Reminder ||
                it is DataUpdateType.Unknown
            }
            .debounce(10L)
            .onStart { emit(DataUpdateType.ReminderScreenDisplayOrder) }
            .map { dataInteractor.getDisplayIndicesForRemindersScreen() }
            .flowOn(io)
            .stateIn(viewModelScope, SharingStarted.Eagerly, null)

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
                reminderInteractor.cancelReminderNotifications(reminder)
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

            // Wait until dbDisplayIndices reflects the new order before switching back
            // to the real reminders, otherwise the UI could glitch swapping back and forth.
            val expectedIndices = orders.associate { it.id to it.displayIndex }
            try {
                withTimeout(500) {
                    dbDisplayIndices.filterNotNull().first { indices ->
                        val byReminderId = indices.associate { it.id to it.displayIndex }
                        expectedIndices.all { (id, expectedIndex) ->
                            byReminderId[id] == expectedIndex
                        }
                    }
                }
            } catch (e: TimeoutCancellationException) {
                Timber.e(
                    e,
                    "The database did not update the reminder indexes within 500ms. Drag and drop update may have failed."
                )
            }

            // Switch back to showing the real reminders from the database
            isDragging.value = false
            temporaryReminders.value = emptyList()
        }
    }

    private suspend fun convertToReminderViewData(reminder: Reminder, groupItemId: Long): ReminderViewData {
        val nextScheduled =
            when (val nextScheduled = reminderInteractor.getNextScheduled(reminder)) {
                is NextScheduled.AtInstant -> LocalDateTime
                    .ofInstant(nextScheduled.instant, timeProvider.defaultZone())

                is NextScheduled.Never -> null
            }

        // For time-since-last reminders, fetch the last tracked instant
        val lastTrackedInstant = getLastTrackedInstant(reminder)

        return ReminderViewData.fromReminder(reminder, groupItemId, nextScheduled, lastTrackedInstant)
    }

    private suspend fun getLastTrackedInstant(reminder: Reminder): Instant? {
        if (reminder.params !is ReminderParams.TimeSinceLastParams) return null

        val featureId = reminder.featureId ?: return null
        val dataSample = dataSampler.getRawDataSampleForFeatureId(featureId) ?: return null

        return try {
            dataSample.iterator().asSequence().firstOrNull()?.timestamp?.toInstant()
        } finally {
            dataSample.dispose()
        }
    }

    private sealed class LoadingState {
        data object Loading : LoadingState()
        data class Loaded(val data: List<ReminderViewData>) : LoadingState()
    }
}