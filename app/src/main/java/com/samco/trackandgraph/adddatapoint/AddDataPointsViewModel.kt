package com.samco.trackandgraph.adddatapoint

import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

interface AddDataPointsViewModel {
    fun initFromArgs(
        trackerIds: List<Long>,
        dataPointTimestamp: OffsetDateTime?,
        durationSeconds: Double?
    )
}

@HiltViewModel
class AddDataPointsViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), AddDataPointsViewModel {


    override fun initFromArgs(
        trackerIds: List<Long>,
        dataPointTimestamp: OffsetDateTime?,
        durationSeconds: Double?
    ) {
/*
        if (initialized) return
        initialized = true

        _state.value = InputDataPointDialogState.LOADING
        viewModelScope.launch(io) {
            val trackers = dataInteractor.getTrackersByIdsSync(trackerIds)
            val dataPointData = dataPointTimestamp?.let {
                dataInteractor.getDataPointByTimestampAndTrackerSync(trackers[0].id, it)
            }
            uiStates = getUIStatesForFeatures(trackers, dataPointData, durationSeconds)

            withContext(ui) {
                _trackers.value = trackers
                _currentTrackerIndex.value = 0
                _state.value = InputDataPointDialogState.WAITING
            }
        }
*/
    }

}