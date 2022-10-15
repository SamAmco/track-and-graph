package com.samco.trackandgraph.adddatapoint

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.launch
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

interface AddDataPointViewModel {

}

interface AddDataPointsViewModel {
    val updateMode: LiveData<Boolean>
    val indexText: LiveData<String>
    val skipButtonVisible: LiveData<Boolean>
    val dataPointViewModels: LiveData<List<AddDataPointViewModel>>

    fun onCancelClicked()
    fun onSkipClicked()
    fun onAddClicked()
}

interface AddDataPointsNavigationViewModel : AddDataPointsViewModel {
    val dismiss: LiveData<Boolean>

    fun initFromArgs(
        trackerIds: List<Long>,
        dataPointTimestamp: OffsetDateTime?,
        customInitialValue: Double?
    )
}

@HiltViewModel
class AddDataPointsViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel(), AddDataPointsNavigationViewModel {

    private data class Config(
        val tracker: Tracker,
        val timestamp: OffsetDateTime?,
        val label: String?,
        val value: Double?,
        val note: String?
    )

    private val configFlow = MutableStateFlow<List<Config>>(emptyList())

    private val indexFlow = MutableStateFlow(0)

    override val dismiss = MutableLiveData(false)

    override val dataPointViewModels = configFlow.map {
        emptyList<AddDataPointViewModel>()
    }.asLiveData(viewModelScope.coroutineContext)

    override val updateMode: LiveData<Boolean> = configFlow.map {
        it.size == 1 && it[0].timestamp != null
    }.asLiveData(viewModelScope.coroutineContext)

    override val indexText = combine(indexFlow, configFlow) { index, configs ->
        if (configs.size == 1) ""
        else "$index / ${configs.size}"
    }.asLiveData(viewModelScope.coroutineContext)

    override val skipButtonVisible = combine(indexFlow, configFlow) { index, configs ->
        if (configs.size == 1) false
        else index < configs.size - 1
    }.asLiveData(viewModelScope.coroutineContext)

    private var initialized = false

    override fun onCancelClicked() {
        dismiss.value = true
    }

    override fun onSkipClicked() {
        if (indexFlow.value < configFlow.value.size - 1)
            indexFlow.value = indexFlow.value + 1
    }

    override fun onAddClicked() {
        //TODO("Not yet implemented")
    }

    override fun initFromArgs(
        trackerIds: List<Long>,
        dataPointTimestamp: OffsetDateTime?,
        customInitialValue: Double?
    ) {
        if (initialized) return
        initialized = true

        viewModelScope.launch(io) {

            val configs = trackerIds
                .mapNotNull { dataInteractor.getTrackerById(it) }
                .map { tracker ->
                    val dataPoint = dataPointTimestamp?.let { timestamp ->
                        dataInteractor.getDataPointByTimestampAndTrackerSync(
                            tracker.id,
                            timestamp
                        )
                    }
                    Config(
                        tracker,
                        dataPointTimestamp,
                        dataPoint?.label,
                        customInitialValue ?: dataPoint?.value,
                        dataPoint?.note
                    )
                }

            if (configs.isEmpty()) dismiss.value = true
            else configFlow.emit(configs)
        }
    }

}