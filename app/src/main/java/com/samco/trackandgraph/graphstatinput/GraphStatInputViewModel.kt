package com.samco.trackandgraph.graphstatinput

import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.LiveData
import androidx.lifecycle.MutableLiveData
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatinput.configviews.FeatureDataProvider
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.cancelChildren
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.lang.Exception
import javax.inject.Inject

class ValidationException(val errorMessageId: Int) : Exception()

interface GraphStatInputViewModel {
    val graphName: TextFieldValue
    val graphStatType: LiveData<GraphStatType>
    val updateMode: LiveData<Boolean>
    val loading: LiveData<Boolean>
    val formValid: LiveData<ValidationException?>
    val configData: LiveData<Any?>
    val demoViewData: LiveData<IGraphStatViewData?>
    val complete: LiveData<Boolean>

    fun initViewModel(graphStatGroupId: Long, graphStatId: Long)
    fun setGraphName(name: String)
    fun setGraphType(type: GraphStatType)
    fun onNewConfigData(config: Any?, exception: ValidationException?)
    fun createGraphOrStat()
}

@HiltViewModel
class GraphStatInputViewModelImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    @DefaultDispatcher private val worker: CoroutineDispatcher
) : ViewModel(), GraphStatInputViewModel {

    override var graphName by mutableStateOf(TextFieldValue(""))
    override val graphStatType = MutableLiveData(GraphStatType.LINE_GRAPH)
    override val updateMode = MutableLiveData(false)
    override val loading = MutableLiveData(false)
    override val formValid = MutableLiveData<ValidationException?>(null)
    override val configData = MutableLiveData<Any?>(null)
    override val demoViewData = MutableLiveData<IGraphStatViewData?>(null)
    override val complete = MutableLiveData(false)

    private var graphStatGroupId: Long = -1L
    private var graphStatId: Long? = null
    private var graphStatDisplayIndex: Int? = null
    private var configException: ValidationException? = null
    private var subConfigException: ValidationException? = null
    private val configCache = mutableMapOf<GraphStatType, Any?>()
    lateinit var featureDataProvider: FeatureDataProvider private set

    override fun initViewModel(graphStatGroupId: Long, graphStatId: Long) {
        if (this.graphStatGroupId != -1L) return
        this.graphStatGroupId = graphStatGroupId
        loading.value = true
        viewModelScope.launch(io) {
            val allFeatures = dataInteractor.getAllFeaturesSync()
            val allGroups = dataInteractor.getAllGroupsSync()
            val dataSourceData = allFeatures.map { feature ->
                FeatureDataProvider.DataSourceData(
                    feature,
                    dataInteractor.getLabelsForFeatureId(feature.featureId).toSet(),
                    dataInteractor.getDataSamplePropertiesForFeatureId(feature.featureId)
                        ?: return@map null
                )
            }.filterNotNull()
            featureDataProvider = FeatureDataProvider(dataSourceData, allGroups)
            if (graphStatId != -1L) initFromExistingGraphStat(graphStatId)
            else withContext(ui) { loading.value = false }
        }
    }

    private suspend fun initFromExistingGraphStat(graphStatId: Long) {
        val graphStat = dataInteractor.tryGetGraphStatById(graphStatId)

        if (graphStat == null) {
            withContext(ui) { loading.value = false }
            return
        }

        val configData = gsiProvider
            .getDataSourceAdapter(graphStat.type)
            .getConfigData(graphStatId)

        configData?.first?.let {
            withContext(ui) {
                this@GraphStatInputViewModelImpl.graphName = graphStat.name.asTfv()
                this@GraphStatInputViewModelImpl.graphStatType.value = graphStat.type
                this@GraphStatInputViewModelImpl.graphStatId = graphStat.id
                this@GraphStatInputViewModelImpl.graphStatDisplayIndex = graphStat.displayIndex
                this@GraphStatInputViewModelImpl.updateMode.value = true
                this@GraphStatInputViewModelImpl.configData.value = configData.second
            }
        }
        withContext(ui) { loading.value = false }
    }

    private fun String.asTfv() = TextFieldValue(this, TextRange(this.length))

    override fun setGraphName(name: String) {
        this.graphName = name.asTfv()
        validateConfiguration()
        updateDemoData()
    }

    override fun setGraphType(type: GraphStatType) {
        this.configData.value = configCache[type]
        this.graphStatType.value = type
        validateConfiguration()
        updateDemoData()
    }

    override fun onNewConfigData(config: Any?, exception: ValidationException?) {
        worker.cancelChildren()
        configData.value = config
        configCache[graphStatType.value!!] = config
        subConfigException = exception
        validateConfiguration()
        updateDemoData()
    }

    private fun updateDemoData() {
        if (formValid.value == null) updateDemoViewData()
        else demoViewData.value = null
    }

    private fun updateDemoViewData() = viewModelScope.launch(worker) {
        if (configData.value == null) return@launch
        val graphOrStat = constructGraphOrStat()

        withContext(ui) { demoViewData.value = IGraphStatViewData.loading(graphOrStat) }

        val demoData = gsiProvider
            .getDataFactory(graphOrStat.type)
            .getViewData(graphOrStat, configData.value!!)

        withContext(ui) { demoViewData.value = demoData }
    }

    private fun validateConfiguration() {
        configException = when {
            graphName.text.isEmpty() -> ValidationException(R.string.graph_stat_validation_no_name)
            graphStatType.value == null -> ValidationException(R.string.graph_stat_validation_unknown)
            else -> null
        }
        formValid.value = configException ?: subConfigException
    }

    override fun createGraphOrStat() {
        if (loading.value == true) return
        if (configData.value == null) return
        loading.value = true
        viewModelScope.launch(io) {
            gsiProvider
                .getDataSourceAdapter(graphStatType.value!!)
                .writeConfig(constructGraphOrStat(), configData.value!!, updateMode.value!!)
            withContext(ui) { complete.value = true }
        }
    }

    private fun constructGraphOrStat() = GraphOrStat(
        id = graphStatId ?: 0L,
        groupId = graphStatGroupId,
        name = graphName.text,
        type = graphStatType.value!!,
        displayIndex = graphStatDisplayIndex ?: 0
    )
}