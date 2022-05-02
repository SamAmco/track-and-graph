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

package com.samco.trackandgraph.graphstatinput.datasourceadapters

import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.AverageTimeBetweenStat
import com.samco.trackandgraph.base.database.entity.GraphOrStat


class AverageTimeBetweenDataSourceAdapter : GraphStatDataSourceAdapter<AverageTimeBetweenStat>() {
    override suspend fun writeConfigToDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long,
        config: AverageTimeBetweenStat,
        updateMode: Boolean
    ) {
        if (updateMode) dataSource.updateAverageTimeBetweenStat(config.copy(graphStatId = graphOrStatId))
        else dataSource.insertAverageTimeBetweenStat(config.copy(graphStatId = graphOrStatId))
    }

    override suspend fun getConfigDataFromDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long
    ): Pair<Long, AverageTimeBetweenStat>? {
        val ats = dataSource.getAverageTimeBetweenStatByGraphStatId(graphOrStatId) ?: return null
        return Pair(ats.id, ats)
    }

    override suspend fun shouldPreen(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat
    ): Boolean {
        return dataSource.getAverageTimeBetweenStatByGraphStatId(graphOrStat.id) == null
    }

    override suspend fun duplicate(
        dataSource: TrackAndGraphDatabaseDao,
        oldGraphId: Long,
        newGraphId: Long
    ) {
        val avTimeStat = dataSource.getAverageTimeBetweenStatByGraphStatId(oldGraphId)
        val copy = avTimeStat?.copy(id = 0, graphStatId = newGraphId)
        copy?.let { dataSource.insertAverageTimeBetweenStat(it) }
    }
}