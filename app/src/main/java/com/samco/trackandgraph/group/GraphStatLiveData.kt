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

/**
 * This live data observes the graphs and stats in the database, uses their data source adapters
 * to generate their data in a coroutine and emits an observable list of pairs of [IGraphStatViewData]
 * objects with the [Instant] that data was calculated so that the observer can determine if a
 * graph has changed since the last emission. It operates only on the given groupId.
 *
 * A list of graph/stats will be emitted as soon as they are observed from the database with
 * loading state view data, then again later when the data has been calculated.
 */
class GraphStatLiveData(
    updateJob: Job,
    groupId: Long,
    private val dataSource: TrackAndGraphDatabaseDao
) :
    LiveData<List<Pair<Instant, IGraphStatViewData>>>(),
    Observer<List<GraphOrStat>> {

    private var hasPostedFirstValue = false
    private val sourceData = dataSource.getGraphsAndStatsByGroupId(groupId)

    override fun onActive() = sourceData.observeForever(this)
    override fun onInactive() {
        sourceData.removeObserver(this)
        //If this live data is observed again we need to preen the graphs again
        hasPostedFirstValue = false
    }

    private val workScope = CoroutineScope(Dispatchers.Default + updateJob)

    override fun onChanged(graphsAndStats: List<GraphOrStat>?) {
        if (graphsAndStats == null) return
        //When first receiving the graphs, preen them to remove invalid graphs in case
        // their dependencies have been removed
        if (!hasPostedFirstValue) workScope.launch { preenGraphStats(graphsAndStats) }
        hasPostedFirstValue = true
        val loadingStates = graphsAndStats
            .map { Pair(Instant.now(), IGraphStatViewData.loading(it)) }
        updateGraphStats(loadingStates)
        workScope.launch { iterateGraphStatDataFactories(graphsAndStats) }
    }

    private fun updateGraphStats(graphStats: List<Pair<Instant, IGraphStatViewData>>) {
        postValue(graphStats)
    }

    fun updateAllGraphStats() {
        workScope.launch {
            iterateGraphStatDataFactories(
                value?.map { it.second.graphOrStat } ?: emptyList()
            )
        }
    }

    private suspend fun preenGraphStats(graphsAndStats: List<GraphOrStat>) {
        graphsAndStats.forEach {
            graphStatTypes[it.type]?.dataSourceAdapter?.preen(
                dataSource,
                it
            )
        }
    }

    suspend fun preenGraphStats() {
        sourceData.value?.let { preenGraphStats(it) }
    }

    private suspend fun iterateGraphStatDataFactories(
        graphsAndStats: List<GraphOrStat>,
    ) {
        val batch = mutableListOf<IGraphStatViewData>()
        for (index in graphsAndStats.indices) {
            val graphOrStat = graphsAndStats[index]
            //I think calling run here may be more optimal because presumably the work will be split
            //across the thread pool
            val viewData = workScope.run {
                graphStatTypes[graphOrStat.type]?.dataFactory!!.getViewData(dataSource, graphOrStat)
            }
            batch.add(index, viewData)
        }
        val newItems = batch.map { Pair(Instant.now(), it) }
        updateGraphStats(newItems)
    }
}
