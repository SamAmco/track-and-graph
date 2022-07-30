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
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

@HiltViewModel
class GroupViewModel @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val gsiProvider: GraphStatInteractorProvider,
    @IODispatcher private val ioDispatcher: CoroutineDispatcher,
    @DefaultDispatcher private val defaultDispatcher: CoroutineDispatcher,
    @MainDispatcher private val mainDispatcher: CoroutineDispatcher
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
                    val featureDataDeferred = getFeatureChildrenAsync(groupId)
                    val groupDataDeferred = getGroupChildrenAsync(groupId)
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
            .flowOn(mainDispatcher)
            .asLiveData()
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

    private fun getFeatureChildrenAsync(groupId: Long) = viewModelScope.async(ioDispatcher) {
        return@async dataInteractor.getDisplayFeaturesForGroupSync(groupId).map {
            GroupChild(GroupChildType.FEATURE, it, it.id, it.displayIndex)
        }
    }

    private fun getGroupChildrenAsync(groupId: Long) = viewModelScope.async(ioDispatcher) {
        return@async dataInteractor.getGroupsForGroupSync(groupId).map {
            GroupChild(GroupChildType.GROUP, it, it.id, it.displayIndex)
        }
    }

    private suspend fun getGraphViewData(groupId: Long) =
        flow<List<Pair<Instant, IGraphStatViewData>>> {
            val graphStats = dataInteractor.getGraphsAndStatsByGroupIdSync(groupId)
            graphStats.forEach {
                gsiProvider.getDataSourceAdapter(it.type).preen(it)
            }
            val loadingStates =
                graphStats.map { Pair(Instant.now(), IGraphStatViewData.loading(it)) }
            emit(loadingStates)

            val batch = mutableListOf<Deferred<IGraphStatViewData>>()
            for (index in graphStats.indices) {
                val graphOrStat = graphStats[index]
                val viewData = viewModelScope.async(defaultDispatcher) {
                    gsiProvider.getDataFactory(graphOrStat.type).getViewData(graphOrStat)
                }
                batch.add(index, viewData)
            }
            emit(batch.map { Pair(Instant.now(), it.await()) })
        }.flowOn(ioDispatcher)

    private fun graphsToGroupChildren(graphs: List<Pair<Instant, IGraphStatViewData>>): List<GroupChild> {
        return graphs.map {
            GroupChild(
                GroupChildType.GRAPH,
                it,
                it.second.graphOrStat.id,
                it.second.graphOrStat.displayIndex
            )
        }
    }

    fun addDefaultFeatureValue(feature: DisplayFeature) = viewModelScope.launch(ioDispatcher) {
        val label = if (feature.featureType == DataType.DISCRETE) {
            feature.discreteValues[feature.defaultValue.toInt()].label
        } else ""
        val newDataPoint = DataPoint(
            OffsetDateTime.now(),
            feature.id,
            feature.defaultValue,
            label,
            ""
        )
        dataInteractor.insertDataPoint(newDataPoint)
    }

    fun onDeleteFeature(id: Long) = viewModelScope.launch(ioDispatcher) {
        dataInteractor.deleteFeature(id)
    }

    fun adjustDisplayIndexes(items: List<GroupChild>) = viewModelScope.launch(ioDispatcher) {
        groupId?.let { dataInteractor.updateGroupChildOrder(it, items) }
    }

    fun onDeleteGraphStat(id: Long) =
        viewModelScope.launch(ioDispatcher) { dataInteractor.deleteGraphOrStat(id) }

    fun onDeleteGroup(id: Long) =
        viewModelScope.launch(ioDispatcher) { dataInteractor.deleteGroup(id) }

    fun duplicateGraphOrStat(graphOrStatViewData: IGraphStatViewData) {
        viewModelScope.launch(ioDispatcher) {
            val gs = graphOrStatViewData.graphOrStat
            gsiProvider.getDataSourceAdapter(gs.type).duplicateGraphOrStat(gs)
        }
    }
}