/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatinput

import android.content.Context
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import androidx.lifecycle.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.database.dto.*
import com.samco.trackandgraph.data.model.DataInteractor
import com.samco.trackandgraph.data.model.di.DefaultDispatcher
import com.samco.trackandgraph.data.model.di.IODispatcher
import com.samco.trackandgraph.data.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.remoteconfig.UrlNavigator
import dagger.hilt.android.lifecycle.HiltViewModel
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.*
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.channels.ReceiveChannel
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import java.lang.Exception
import javax.inject.Inject

sealed interface GraphStatConfigEvent {
    data class ValidationException(val errorMessageId: Int) : Exception(), GraphStatConfigEvent
    object Loading : GraphStatConfigEvent

    sealed interface ConfigData<T> : GraphStatConfigEvent {
        val config: T

        data class LineGraphConfigData(
            override val config: LineGraphWithFeatures
        ) : ConfigData<LineGraphWithFeatures>

        data class PieChartConfigData(
            override val config: PieChart
        ) : ConfigData<PieChart>

        data class AverageTimeBetweenConfigData(
            override val config: AverageTimeBetweenStat
        ) : ConfigData<AverageTimeBetweenStat>

        data class TimeHistogramConfigData(
            override val config: TimeHistogram
        ) : ConfigData<TimeHistogram>

        data class LastValueConfigData(
            override val config: LastValueStat
        ) : ConfigData<LastValueStat>

        data class BarChartConfigData(
            override val config: BarChart
        ) : ConfigData<BarChart>

        data class LuaConfigData(
            override val config: LuaGraphWithFeatures
        ) : ConfigData<LuaGraphWithFeatures>
    }
}

interface GraphStatInputViewModel {
    val graphName: TextFieldValue
    val graphStatType: LiveData<GraphStatType>
    val updateMode: LiveData<Boolean>
    val loading: LiveData<Boolean>
    val validationException: LiveData<GraphStatConfigEvent.ValidationException?>
    val demoViewData: LiveData<IGraphStatViewData?>
    val showLuaFirstTimeUserDialog: LiveData<Boolean>
    val complete: ReceiveChannel<Unit>

    fun initViewModel(graphStatGroupId: Long, graphStatId: Long)
    fun setGraphStatName(name: TextFieldValue)
    fun setGraphType(type: GraphStatType)

    fun onConfigEvent(configData: GraphStatConfigEvent?)
    fun createGraphOrStat()
    fun onLuaFirstTimeUserDialogDismiss()
    fun onOpenLuaTutorialPath()
}

@HiltViewModel
class GraphStatInputViewModelImpl @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    private val urlNavigator: UrlNavigator,
    @IODispatcher private val io: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher,
    @DefaultDispatcher private val worker: CoroutineDispatcher
) : ViewModel(), GraphStatInputViewModel {

    private val thisIsLoading = MutableStateFlow(false)
    private val configIsLoading = MutableStateFlow(false)

    override var graphName by mutableStateOf(TextFieldValue(""))
    override val graphStatType = MutableLiveData(GraphStatType.LINE_GRAPH)
    override val updateMode = MutableLiveData(false)
    override val loading = combine(thisIsLoading, configIsLoading) { a, b -> a || b }
        .asLiveData(viewModelScope.coroutineContext)
    override val validationException =
        MutableLiveData<GraphStatConfigEvent.ValidationException?>(null)
    override val demoViewData = MutableLiveData<IGraphStatViewData?>(null)
    override val complete = Channel<Unit>(1)

    private val _showLuaFirstTimeUserDialog = MutableLiveData(false)
    override val showLuaFirstTimeUserDialog: LiveData<Boolean> = _showLuaFirstTimeUserDialog

    private var updateJob: Job? = null
    private var configData: GraphStatConfigEvent.ConfigData<*>? = null
    private var graphStatGroupId: Long = -1L
    private var graphStatId: Long? = null
    private var graphStatDisplayIndex: Int? = null
    private var subConfigException: GraphStatConfigEvent.ValidationException? = null

    override fun initViewModel(graphStatGroupId: Long, graphStatId: Long) {
        if (this.graphStatGroupId != -1L) return
        this.graphStatGroupId = graphStatGroupId
        viewModelScope.launch(io) {
            thisIsLoading.value = true
            if (graphStatId != -1L) initFromExistingGraphStat(graphStatId)
            thisIsLoading.value = false
        }
    }

    private suspend fun initFromExistingGraphStat(graphStatId: Long) {
        val graphStat = withContext(io) {
            dataInteractor.tryGetGraphStatById(graphStatId)
        }

        withContext(ui) {
            if (graphStat == null) {
                return@withContext
            }

            this@GraphStatInputViewModelImpl.graphName = graphStat.name.asTfv()
            this@GraphStatInputViewModelImpl.graphStatType.value = graphStat.type
            this@GraphStatInputViewModelImpl.graphStatId = graphStat.id
            this@GraphStatInputViewModelImpl.graphStatDisplayIndex = graphStat.displayIndex
            this@GraphStatInputViewModelImpl.updateMode.value = true
        }
    }

    private fun String.asTfv() = TextFieldValue(this, TextRange(this.length))

    override fun setGraphStatName(name: TextFieldValue) {
        if (this.graphName != name) {
            this.graphName = name
            updateDemoData()
        }
    }

    override fun setGraphType(type: GraphStatType) {
        configData = null
        this.graphStatType.value = type

        if (type == GraphStatType.LUA_SCRIPT) {
            viewModelScope.launch {
                if (!dataInteractor.hasAnyLuaGraphs()) {
                    _showLuaFirstTimeUserDialog.value = true
                }
            }
        }
    }

    override fun onConfigEvent(configData: GraphStatConfigEvent?) {
        configIsLoading.value = false
        when (configData) {
            is GraphStatConfigEvent.ValidationException -> onValidationException(configData)
            is GraphStatConfigEvent.Loading -> configIsLoading.value = true
            is GraphStatConfigEvent.ConfigData<*> -> updateConfigData(configData)
            null -> {}
        }
    }

    private fun updateConfigData(configData: GraphStatConfigEvent.ConfigData<*>) {
        subConfigException = null
        this.configData = configData
        updateDemoData()
    }

    override fun createGraphOrStat() {
        if (thisIsLoading.value) return
        configData?.config?.let {
            thisIsLoading.value = true
            viewModelScope.launch(io) {
                gsiProvider
                    .getDataSourceAdapter(graphStatType.value!!)
                    .writeConfig(constructGraphOrStat(), it, updateMode.value!!)
                withContext(ui) { complete.send(Unit) }
            }
        }
    }

    override fun onLuaFirstTimeUserDialogDismiss() {
        _showLuaFirstTimeUserDialog.value = false
    }

    override fun onOpenLuaTutorialPath() {
        viewModelScope.launch {
            urlNavigator.navigateTo(context, UrlNavigator.Location.TUTORIAL_LUA)
        }
    }

    private fun onValidationException(exception: GraphStatConfigEvent.ValidationException?) {
        this.subConfigException = exception
        validateConfiguration()
    }

    private fun updateDemoData() {
        validateConfiguration()
        if (validationException.value == null) updateDemoViewData()
        else demoViewData.value = null
    }

    private fun updateDemoViewData() {
        updateJob?.cancel()
        updateJob = viewModelScope.launch(worker) {
            configData?.config?.let {
                val graphOrStat = constructGraphOrStat()

                withContext(ui) { demoViewData.value = IGraphStatViewData.loading(graphOrStat) }

                val demoData = gsiProvider
                    .getDataFactory(graphOrStat.type)
                    .getViewData(graphOrStat, it)

                withContext(ui) { demoViewData.value = demoData }
            }
        }
    }

    private fun validateConfiguration() {
        val configException = when {
            graphName.text.isEmpty() -> GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_no_name)
            graphStatType.value == null -> GraphStatConfigEvent.ValidationException(R.string.graph_stat_validation_unknown)
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
