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

import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.entity.GraphOrStat
import com.samco.trackandgraph.database.entity.TimeHistogram

class TimeHistogramDataSourceAdapter : GraphStatDataSourceAdapter<TimeHistogram>(){
    override suspend fun writeConfigToDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long,
        config: TimeHistogram,
        updateMode: Boolean
    ) {
        if (updateMode) dataSource.updateTimeHistogram(config.copy(graphStatId = graphOrStatId))
        else dataSource.insertTimeHistogram(config.copy(graphStatId = graphOrStatId))
    }

    override suspend fun getConfigDataFromDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long
    ): Pair<Long, TimeHistogram>? {
        val th = dataSource.getTimeHistogramByGraphStatId(graphOrStatId) ?: return null
        return Pair(th.id, th)
    }

    override suspend fun shouldPreen(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat
    ): Boolean {
        return dataSource.getTimeHistogramByGraphStatId(graphOrStat.id) == null
    }

    override suspend fun duplicate(
        dataSource: TrackAndGraphDatabaseDao,
        oldGraphId: Long,
        newGraphId: Long
    ) {
        val timeHistogram = dataSource.getTimeHistogramByGraphStatId(oldGraphId)
        val copy = timeHistogram?.copy(id = 0, graphStatId = newGraphId)
        copy?.let { dataSource.insertTimeHistogram(it) }
    }
}