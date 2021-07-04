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
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.dto.DisplayFeature
import com.samco.trackandgraph.database.entity.Group
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.Job
import org.threeten.bp.Instant

class GroupChildrenLiveData(
    updateJob: Job,
    groupId: Long,
    dataSource: TrackAndGraphDatabaseDao
) : LiveData<List<GroupChild>>() {
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
        considerPost()
    }
    private val graphStatDataObserver = Observer<List<GroupChild>> {
        latestGraphs = it
        considerPost()
    }
    private val groupDataObserver = Observer<List<GroupChild>> {
        latestGroups = it
        considerPost()
    }

    @Synchronized
    private fun considerPost() {
        if (latestFeatures != null && latestGroups != null && latestGraphs != null) {
            val combined = mutableListOf<GroupChild>()
            latestFeatures?.let { combined.addAll(it) }
            latestGraphs?.let { combined.addAll(it) }
            latestGroups?.let { combined.addAll(it) }
            combined.sortBy { it.displayIndex() }
            postValue(combined)
        }
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