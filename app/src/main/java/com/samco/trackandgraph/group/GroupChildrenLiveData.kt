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
import androidx.lifecycle.Transformations
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.entity.queryresponse.DisplayFeature
import com.samco.trackandgraph.base.database.entity.Group
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.*
import org.threeten.bp.Instant

/**
 * This live data represents all the children in a given group. The children are retrieved via the
 * dataSource and observed in 3 separate calls for features, graphs and groups. When any of these
 * observed live data are changed, this live data will schedule a delayed post which will contain
 * all children combined and sorted by display index and then ID. If more updates are observed before
 * that scheduled post executes, it is cancelled and scheduled again to try and minimise the number
 * of down stream update events.
 */
//TODO I think this could probably be done much more nicely using a Flow that combines the LiveData's?
class GroupChildrenLiveData(
    updateJob: Job,
    groupId: Long,
    dataSource: TrackAndGraphDatabaseDao
) : LiveData<List<GroupChild>>() {
    private val workScope = CoroutineScope(Dispatchers.Default + updateJob)
    private var job: Job? = null

    val graphStatLiveData = GraphStatLiveData(updateJob, groupId, dataSource)

    private val graphStatData = Transformations.map(
        graphStatLiveData,
        this::graphsToGroupChildren
    )
    private val featureData = Transformations.map(
        dataSource.getDisplayFeaturesForGroup(groupId),
        this::featuresToGroupChildren
    )
    private val groupData = Transformations.map(
        dataSource.getGroupsForGroup(groupId),
        this::groupsToGroupChildren
    )

    private var latestFeatures: List<GroupChild>? = null
    private var latestGroups: List<GroupChild>? = null
    private var latestGraphs: List<GroupChild>? = null

    private val featureDataObserver = Observer<List<GroupChild>> {
        latestFeatures = it
        schedulePost()
    }
    private val graphStatDataObserver = Observer<List<GroupChild>> {
        latestGraphs = it
        schedulePost()
    }
    private val groupDataObserver = Observer<List<GroupChild>> {
        latestGroups = it
        schedulePost()
    }

    //We schedule a delayed post to avoid UI glitches in situations where multiple data sources
    // are changed at once in the database and we have no way of knowing that we are about to
    // receive several updates in quick succession
    @Synchronized
    private fun schedulePost() {
        job?.cancel()
        job = workScope.launch {
            delay(200)
            if (isActive) postLatest()
        }
    }

    @Synchronized
    private fun postLatest() {
        val combined = mutableListOf<GroupChild>()
        latestFeatures?.let { combined.addAll(it) }
        latestGraphs?.let { combined.addAll(it) }
        latestGroups?.let { combined.addAll(it) }
        combined.sortWith { a, b ->
            val aInd = a.displayIndex()
            val bInd = b.displayIndex()
            when {
                aInd < bInd -> -1
                bInd < aInd -> 1
                else -> {
                    val aId = a.id()
                    val bId = b.id()
                    when {
                        aId > bId -> -1
                        bId > aId -> 1
                        else -> 0
                    }
                }
            }
        }
        postValue(combined)
    }

    override fun onActive() {
        featureData.observeForever(featureDataObserver)
        graphStatData.observeForever(graphStatDataObserver)
        groupData.observeForever(groupDataObserver)
    }

    override fun onInactive() {
        featureData.removeObserver(featureDataObserver)
        graphStatData.removeObserver(graphStatDataObserver)
        groupData.removeObserver(groupDataObserver)
    }

    private fun featuresToGroupChildren(displayFeatures: List<DisplayFeature>): List<GroupChild> {
        return displayFeatures.map {
            GroupChild(GroupChildType.FEATURE, it, it::id, it::displayIndex)
        }
    }

    private fun groupsToGroupChildren(groups: List<Group>): List<GroupChild> {
        return groups.map {
            GroupChild(GroupChildType.GROUP, it, it::id, it::displayIndex)
        }
    }

    private fun graphsToGroupChildren(graphs: List<Pair<Instant, IGraphStatViewData>>): List<GroupChild> {
        return graphs.map {
            GroupChild(
                GroupChildType.GRAPH,
                it,
                { it.second.graphOrStat.id },
                { it.second.graphOrStat.displayIndex }
            )
        }
    }
}