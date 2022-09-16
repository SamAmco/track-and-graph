package com.samco.trackandgraph.featurehistory

import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import javax.inject.Inject

@HiltViewModel
class FeatureHistoryViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {
    private val _isDuration = MutableLiveData(false)
    val isDuration: LiveData<Boolean> get() = _isDuration

    private val _dataPoints = MutableLiveData<List<DataPoint>>(emptyList())
    val dataPoints: LiveData<List<DataPoint>> = _dataPoints

    private val _feature = MutableLiveData<Feature?>(null)
    val feature: LiveData<Feature?> get() = _feature

    private val _tracker = MutableLiveData<Tracker?>(null)
    val tracker: LiveData<Tracker?> get() = _tracker

    var currentActionDataPoint: DataPoint? = null

    private var updateJob = Job()
    private val ioScope = CoroutineScope(Dispatchers.IO + updateJob)

    private var featureId: Long? = null

    fun initViewModel(featureId: Long) {
        if (this.featureId != null) return
        this.featureId = featureId
        ioScope.launch(io) {
            val dataSample = dataInteractor.getDataSampleForFeatureId(featureId)
            dataSample.iterator().apply { while (hasNext()) next() }
            val rawData = dataSample.getRawDataPoints()
            val feature = dataInteractor.getFeatureById(featureId)
            val tracker = dataInteractor.getTrackerByFeatureId(featureId)

            withContext(ui) {
                _isDuration.value = dataSample.dataSampleProperties.isDuration
                _dataPoints.value = rawData
                _tracker.value = tracker
                _feature.value = feature
            }
        }
    }

    fun deleteDataPoint() = currentActionDataPoint?.let {
        ioScope.launch { dataInteractor.deleteDataPoint(it) }
    }

    override fun onCleared() {
        super.onCleared()
        updateJob.cancel()
    }
}