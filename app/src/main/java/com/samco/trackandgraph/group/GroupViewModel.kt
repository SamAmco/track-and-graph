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

package com.samco.trackandgraph.group

import androidx.lifecycle.*
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.DefaultDispatcher
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.base.model.di.MainDispatcher
import com.samco.trackandgraph.graphstatproviders.GraphStatInteractorProvider
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import dagger.hilt.android.lifecycle.HiltViewModel
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.*
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val io: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @MainDispatcher private val ui: CoroutineDispatcher
) : ViewModel() {

    lateinit var hasFeatures: LiveData<Boolean>

    lateinit var groupChildren: LiveData<List<GroupChild>>
        private set

    val features
        get() = groupChildren.value
            ?.filter { it.type == GroupChildType.FEATURE }
            ?.map { it.obj as DisplayFeature }
            ?: emptyList()

    private var initialized = false

    private var groupId: Long? = null

    fun setGroup(groupId: Long) {
        if (initialized) return
        this.groupId = groupId
        initialized = true
        hasFeatures = Transformations.map(dataInteractor.getAllFeatures()) { it.isNotEmpty() }
        initGroupChildren(groupId)
    }

    @OptIn(FlowPreview::class, ExperimentalCoroutinesApi::class)
    private fun initGroupChildren(groupId: Long) {
        groupChildren = dataInteractor.getDataUpdateEvents()
            .onStart { emit(Unit) }
            .debounce(100)
            .flatMapLatest {
                flow {
                    //Start getting the features and groups
                    val featureDataDeferred = getFeatureChildrenAsync(groupId)
                    val groupDataDeferred = getGroupChildrenAsync(groupId)
                    //Then get the graphs
                    getGraphViewData(groupId).collect { graphDataPairs ->
                        val featureData = featureDataDeferred.await()
                        val groupData = groupDataDeferred.await()
                        val graphData = graphsToGroupChildren(graphDataPairs)
                        val children = mutableListOf<GroupChild>().apply {
                            addAll(featureData)
                            addAll(groupData)
                            addAll(graphData)
                        }
                        sortChildren(children)
                        val next = children.toList()
                        emit(next)
                    }
                }
            }
            .flowOn(ui)
            .asLiveData(viewModelScope.coroutineContext)
    }

    private fun sortChildren(children: MutableList<GroupChild>) = children.sortWith { a, b ->
        val aInd = a.displayIndex
        val bInd = b.displayIndex
        when {
            aInd < bInd -> -1
            bInd < aInd -> 1
            else -> {
                val aId = a.id
                val bId = b.id
                when {
                    aId > bId -> -1
                    bId > aId -> 1
                    else -> 0
                }
            }
        }
    }

    private fun getFeatureChildrenAsync(groupId: Long) = viewModelScope.async(io) {
        return@async dataInteractor.getDisplayFeaturesForGroupSync(groupId).map {
            GroupChild(GroupChildType.FEATURE, it, it.id, it.displayIndex)
        }
    }

    private fun getGroupChildrenAsync(groupId: Long) = viewModelScope.async(io) {
        return@async dataInteractor.getGroupsForGroupSync(groupId).map {
            GroupChild(GroupChildType.GROUP, it, it.id, it.displayIndex)
        }
    }

    private suspend fun getGraphViewData(groupId: Long) =
        flow<List<Pair<Long, IGraphStatViewData>>> {
            val graphStats = dataInteractor.getGraphsAndStatsByGroupIdSync(groupId)
            graphStats.forEach {
                gsiProvider.getDataSourceAdapter(it.type).preen(it)
            }
            val loadingStates =
                graphStats.map { Pair(System.nanoTime(), IGraphStatViewData.loading(it)) }
            emit(loadingStates)

            val batch = mutableListOf<Deferred<IGraphStatViewData>>()
            for (index in graphStats.indices) {
                val graphOrStat = graphStats[index]
                val viewData = viewModelScope.async(defaultDispatcher) {
                    gsiProvider.getDataFactory(graphOrStat.type).getViewData(graphOrStat)
                }
                batch.add(index, viewData)
            }
            //Shouldn't really need to add one here but it just forces the times to be different
            // There was a bug previously where the loading and ready states had the same time using
            // Instant.now() which caused ready states to be missed and infinite loading to be shown
            emit(batch.map { Pair(System.nanoTime() + 1, it.await()) })
        }.flowOn(io)

    private fun graphsToGroupChildren(graphs: List<Pair<Long, IGraphStatViewData>>): List<GroupChild> {
        return graphs.map {
            GroupChild(
                GroupChildType.GRAPH,
                it,
                it.second.graphOrStat.id,
                it.second.graphOrStat.displayIndex
            )
        }
    }

    fun addDefaultFeatureValue(feature: DisplayFeature) = viewModelScope.launch(io) {
        val ts = OffsetDateTime.now()
        var defaultValue = feature.defaultValue
        if (feature.featureType == DataType.TIMESTAMP) {
            defaultValue = ts.second + ts.minute * 60.0 + ts.hour * 3600.0
        }
        val label = if (feature.featureType == DataType.DISCRETE) {
            feature.discreteValues[feature.defaultValue.toInt()].label
        } else ""
        val newDataPoint = DataPoint(
            ts,
            feature.id,
            defaultValue,
            label,
            ""
        )
        dataInteractor.insertDataPoint(newDataPoint)
    }

    fun onDeleteFeature(id: Long) = viewModelScope.launch(io) {
        dataInteractor.deleteFeature(id)
    }

    fun adjustDisplayIndexes(items: List<GroupChild>) = viewModelScope.launch(io) {
        groupId?.let { dataInteractor.updateGroupChildOrder(it, items) }
    }

    fun onDeleteGraphStat(id: Long) =
        viewModelScope.launch(io) { dataInteractor.deleteGraphOrStat(id) }

    fun onDeleteGroup(id: Long) =
        viewModelScope.launch(io) { dataInteractor.deleteGroup(id) }

    fun duplicateGraphOrStat(graphOrStatViewData: IGraphStatViewData) {
        viewModelScope.launch(io) {
            val gs = graphOrStatViewData.graphOrStat
            gsiProvider.getDataSourceAdapter(gs.type).duplicateGraphOrStat(gs)
        }
    }
}