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
import com.samco.trackandgraph.base.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
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

sealed interface ConfigData<T> {
    val config: T

    class LineGraphConfigData(
        override val config: LineGraphWithFeatures
    ) : ConfigData<LineGraphWithFeatures>
}

interface GraphStatInputViewModel {
    val graphName: TextFieldValue
    val graphStatType: LiveData<GraphStatType>
    val updateMode: LiveData<Boolean>
    val loading: LiveData<Boolean>
    val validationException: LiveData<ValidationException?>
    val demoViewData: LiveData<IGraphStatViewData?>
    val complete: LiveData<Boolean>

    fun initViewModel(graphStatGroupId: Long, graphStatId: Long)
    fun setGraphName(name: String)
    fun setGraphType(type: GraphStatType)

    fun updateConfigData(configData: ConfigData<*>)
    fun createGraphOrStat()

    fun onValidationException(exception: ValidationException?)
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
    override val validationException = MutableLiveData<ValidationException?>(null)
    override val demoViewData = MutableLiveData<IGraphStatViewData?>(null)
    override val complete = MutableLiveData(false)

    private var configData: ConfigData<*>? = null
    private var graphStatGroupId: Long = -1L
    private var graphStatId: Long? = null
    private var graphStatDisplayIndex: Int? = null
    private var subConfigException: ValidationException? = null

    override fun initViewModel(graphStatGroupId: Long, graphStatId: Long) {
        if (this.graphStatGroupId != -1L) return
        this.graphStatGroupId = graphStatGroupId
        loading.value = true
        viewModelScope.launch(io) {
            if (graphStatId != -1L) initFromExistingGraphStat(graphStatId)
            else withContext(ui) { loading.value = false }
        }
    }

    private suspend fun initFromExistingGraphStat(graphStatId: Long) {
        val graphStat = withContext(io) {
            dataInteractor.tryGetGraphStatById(graphStatId)
        }

        withContext(ui) {
            if (graphStat == null) {
                loading.value = false
                return@withContext
            }

            this@GraphStatInputViewModelImpl.graphName = graphStat.name.asTfv()
            this@GraphStatInputViewModelImpl.graphStatType.value = graphStat.type
            this@GraphStatInputViewModelImpl.graphStatId = graphStat.id
            this@GraphStatInputViewModelImpl.graphStatDisplayIndex = graphStat.displayIndex
            this@GraphStatInputViewModelImpl.updateMode.value = true
            loading.value = false
        }
    }

    private fun String.asTfv() = TextFieldValue(this, TextRange(this.length))

    override fun setGraphName(name: String) {
        this.graphName = name.asTfv()
        updateDemoData()
    }

    override fun setGraphType(type: GraphStatType) {
        this.graphStatType.value = type
        updateDemoData()
    }

    override fun updateConfigData(configData: ConfigData<*>) {
        this.configData = configData
        updateDemoData()
    }

    override fun createGraphOrStat() {
        if (loading.value == true) return
        configData?.config?.let {
            loading.value = true
            viewModelScope.launch(io) {
                gsiProvider
                    .getDataSourceAdapter(graphStatType.value!!)
                    .writeConfig(constructGraphOrStat(), it, updateMode.value!!)
                withContext(ui) { complete.value = true }
            }
        }
    }

    override fun onValidationException(exception: ValidationException?) {
        this.subConfigException = exception
        validateConfiguration()
    }

    private fun updateDemoData() {
        worker.cancelChildren()
        validateConfiguration()
        if (validationException.value == null) updateDemoViewData()
        else demoViewData.value = null
    }

    private fun updateDemoViewData() = viewModelScope.launch(worker) {
        configData?.config?.let {
            val graphOrStat = constructGraphOrStat()

            withContext(ui) { demoViewData.value = IGraphStatViewData.loading(graphOrStat) }

            val demoData = gsiProvider
                .getDataFactory(graphOrStat.type)
                .getViewData(graphOrStat, it)

            withContext(ui) { demoViewData.value = demoData }
        }
    }

    private fun validateConfiguration() {
        val configException = when {
            graphName.text.isEmpty() -> ValidationException(R.string.graph_stat_validation_no_name)
            graphStatType.value == null -> ValidationException(R.string.graph_stat_validation_unknown)
            else -> null
        }
        validationException.value = configException ?: subConfigException
    }

    private fun constructGraphOrStat() = GraphOrStat(
        id = graphStatId ?: 0L,
        groupId = graphStatGroupId,
        name = graphName.text,
        type = graphStatType.value!!,
        displayIndex = graphStatDisplayIndex ?: 0
    )
}