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

import androidx.lifecycle.LiveData
import androidx.lifecycle.Observer
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.graphclassmappings.graphStatTypes
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.*
import org.threeten.bp.Instant

class GraphStatLiveData(
    updateJob: Job,
    groupId: Long,
    private val dataSource: TrackAndGraphDatabaseDao
) :
    LiveData<List<Pair<Instant, IGraphStatViewData>>>(),
    Observer<List<GraphOrStat>> {

    private val sourceData = dataSource.getGraphsAndStatsByGroupId(groupId)

    override fun onActive() = sourceData.observeForever(this)
    override fun onInactive() = sourceData.removeObserver(this)

    private val workScope = CoroutineScope(Dispatchers.Default + updateJob)

    override fun onChanged(graphsAndStats: List<GraphOrStat>?) {
        if (graphsAndStats == null) return
        val loadingStates = graphsAndStats
            .map { Pair(Instant.now(), IGraphStatViewData.loading(it)) }
        updateGraphStats(loadingStates)
        workScope.launch { iterateGraphStatDataFactories(graphsAndStats, false) }
    }

    private fun updateGraphStats(graphStats: List<Pair<Instant, IGraphStatViewData>>) {
        postValue(graphStats)
    }

    @Synchronized
    private fun updateGraphStatsAtIndex(index: Int, data: IGraphStatViewData) {
        val value = this.value?.toMutableList() ?: mutableListOf()
        if (index >= 0 && index < value.size) {
            value.removeAt(index)
            value.add(index, Pair(Instant.now(), data))
            postValue(value)
        }
    }

    fun updateAllGraphStats() {
        workScope.launch {
            iterateGraphStatDataFactories(
                value?.map { it.second.graphOrStat } ?: emptyList(),
                false
            )
        }
    }

    suspend fun preenGraphStats() {
        sourceData.value?.forEach {
            graphStatTypes[it.type]?.dataSourceAdapter?.preen(
                dataSource,
                it
            )
        }
    }

    private suspend fun iterateGraphStatDataFactories(
        graphsAndStats: List<GraphOrStat>,
        batchUpdate: Boolean
    ) {
        val batch = mutableListOf<IGraphStatViewData>()
        for (index in graphsAndStats.indices) {
            val graphOrStat = graphsAndStats[index]
            //I think calling run here may be more optimal because presumably the work will be split
            //across the thread pool
            val viewData = workScope.run {
                graphStatTypes[graphOrStat.type]?.dataFactory!!.getViewData(dataSource, graphOrStat)
            }
            if (batchUpdate) batch.add(index, viewData)
            else withContext(Dispatchers.Main) { updateGraphStatsAtIndex(index, viewData) }
        }
        if (batchUpdate) {
            val newItems = batch.map { Pair(Instant.now(), it) }
            updateGraphStats(newItems)
        }
    }
}
