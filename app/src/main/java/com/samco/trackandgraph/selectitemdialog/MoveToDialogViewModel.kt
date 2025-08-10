package com.samco.trackandgraph.selectitemdialog

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.Group
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.timers.TimerServiceInteractor
import com.samco.trackandgraph.util.GroupPathProvider
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import javax.inject.Inject

enum class MoveToDialogState { INITIALIZING, WAITING, MOVING, MOVED }

@HiltViewModel
class MoveToDialogViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val timerServiceInteractor: TimerServiceInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {
    private lateinit var mode: MoveDialogType

    private val _state = MutableLiveData(MoveToDialogState.INITIALIZING)
    val state: LiveData<MoveToDialogState> get() = _state

    private val _availableGroups = MutableLiveData<GroupPathProvider>()
    val availableGroups: LiveData<GroupPathProvider> get() = _availableGroups

    private var tracker: Tracker? = null
    private var graphStat: GraphOrStat? = null
    private var group: Group? = null

    private var initialized = false

    fun init(mode: MoveDialogType, id: Long) {
        if (initialized) return
        initialized = true

        this.mode = mode
        viewModelScope.launch(io) {
            val groups = dataInteractor.getAllGroupsSync().toMutableList()
            val groupPathProvider = GroupPathProvider(
                groups = groups,
                groupFilterId = if (mode == MoveDialogType.GROUP) id else null
            )
            when (mode) {
                MoveDialogType.TRACKER -> tracker = dataInteractor.getTrackerById(id)
                MoveDialogType.GRAPH -> graphStat = dataInteractor.getGraphStatById(id)
                MoveDialogType.GROUP -> group = dataInteractor.getGroupById(id)
            }
            withContext(ui) {
                _availableGroups.value = groupPathProvider
                _state.value = MoveToDialogState.WAITING
            }
        }
    }

    fun moveTo(newGroupId: Long) = viewModelScope.launch(io) {
        if (_state.value != MoveToDialogState.WAITING) return@launch
        withContext(ui) { _state.value = MoveToDialogState.MOVING }
        when (mode) {
            MoveDialogType.TRACKER -> {
                tracker?.copy(groupId = newGroupId)?.let {
                    dataInteractor.updateTracker(it)
                    timerServiceInteractor.requestWidgetUpdatesForFeatureId(it.featureId)
                }
            }
            MoveDialogType.GRAPH -> {
                graphStat?.copy(groupId = newGroupId)?.let {
                    dataInteractor.updateGraphOrStat(it)
                }
            }
            MoveDialogType.GROUP -> {
                group?.copy(parentGroupId = newGroupId)?.let {
                    dataInteractor.updateGroup(it)
                }
            }
        }
        withContext(ui) { _state.value = MoveToDialogState.MOVED }
    }
}