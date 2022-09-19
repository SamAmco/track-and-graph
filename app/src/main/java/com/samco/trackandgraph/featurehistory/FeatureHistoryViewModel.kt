package com.samco.trackandgraph.featurehistory

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import javax.inject.Inject

@HiltViewModel
class FeatureHistoryViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
) : ViewModel() {
    private val featureIdFlow = MutableSharedFlow<Long>(replay = 1, extraBufferCapacity = 1)

    private val dataSample =
        combine(
            featureIdFlow,
            dataInteractor.getDataUpdateEvents().onStart { emit(Unit) }) { id, _ -> id }
            .map {
                val dataSample = dataInteractor.getDataSampleForFeatureId(it)
                dataSample.iterator().apply { while (hasNext()) next() }
                dataSample
            }
            .flowOn(io)

    val isDuration: LiveData<Boolean> = dataSample.map {
        it.dataSampleProperties.isDuration
    }.asLiveData(viewModelScope.coroutineContext)

    val dataPoints: LiveData<List<DataPoint>> = dataSample.map {
        it.getRawDataPoints()
    }.asLiveData(viewModelScope.coroutineContext)

    val feature: LiveData<Feature?> = featureIdFlow.map {
        dataInteractor.getFeatureById(it)
    }.asLiveData(viewModelScope.coroutineContext)

    val tracker: LiveData<Tracker?> = featureIdFlow
        .map { dataInteractor.getTrackerByFeatureId(it) }
        .asLiveData(viewModelScope.coroutineContext)

    var currentActionDataPoint: DataPoint? = null

    private var featureId: Long? = null

    fun initViewModel(featureId: Long) {
        if (this.featureId != null) return
        this.featureId = featureId
        viewModelScope.launch(io) { featureIdFlow.emit(featureId) }
    }

    fun deleteDataPoint() = currentActionDataPoint?.let {
        viewModelScope.launch(io) { dataInteractor.deleteDataPoint(it) }
    }

}