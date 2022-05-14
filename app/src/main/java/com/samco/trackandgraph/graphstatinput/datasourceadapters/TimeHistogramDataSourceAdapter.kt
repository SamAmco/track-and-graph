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

import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.TimeHistogram
import com.samco.trackandgraph.base.model.DataInteractor
import javax.inject.Inject

class TimeHistogramDataSourceAdapter @Inject constructor(
    dataInteractor: DataInteractor
) : GraphStatDataSourceAdapter<TimeHistogram>(dataInteractor) {
    override suspend fun writeConfigToDatabase(
        graphOrStatId: Long,
        config: TimeHistogram,
        updateMode: Boolean
    ) {
        if (updateMode) dataInteractor.updateTimeHistogram(config.copy(graphStatId = graphOrStatId))
        else dataInteractor.insertTimeHistogram(config.copy(graphStatId = graphOrStatId))
    }

    override suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, TimeHistogram>? {
        val th = dataInteractor.getTimeHistogramByGraphStatId(graphOrStatId) ?: return null
        return Pair(th.id, th)
    }

    override suspend fun shouldPreen(graphOrStat: GraphOrStat): Boolean {
        return dataInteractor.getTimeHistogramByGraphStatId(graphOrStat.id) == null
    }

    override suspend fun duplicate(oldGraphId: Long, newGraphId: Long) {
        val timeHistogram = dataInteractor.getTimeHistogramByGraphStatId(oldGraphId)
        val copy = timeHistogram?.copy(id = 0, graphStatId = newGraphId)
        copy?.let { dataInteractor.insertTimeHistogram(it) }
    }
}