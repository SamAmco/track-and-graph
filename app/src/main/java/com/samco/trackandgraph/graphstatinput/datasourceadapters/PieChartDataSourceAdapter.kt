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
import com.samco.trackandgraph.database.entity.PieChart

class PieChartDataSourceAdapter : GraphStatDataSourceAdapter<PieChart>() {
    override suspend fun writeConfigToDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long,
        config: PieChart,
        updateMode: Boolean
    ) {
        if (updateMode) dataSource.updatePieChart(config.copy(graphStatId = graphOrStatId))
        else dataSource.insertPieChart(config.copy(graphStatId = graphOrStatId))
    }

    override suspend fun getConfigDataFromDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long
    ): Pair<Long, PieChart>? {
        val pieChart = dataSource.getPieChartByGraphStatId(graphOrStatId) ?: return null
        return Pair(pieChart.id, pieChart)
    }

    override suspend fun shouldPreen(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat
    ): Boolean {
        return dataSource.getPieChartByGraphStatId(graphOrStat.id) == null
    }

    override suspend fun duplicate(
        dataSource: TrackAndGraphDatabaseDao,
        oldGraphId: Long,
        newGraphId: Long
    ) {
        val pieChart = dataSource.getPieChartByGraphStatId(oldGraphId)
        val copy = pieChart?.copy(id = 0, graphStatId = newGraphId)
        copy?.let { dataSource.insertPieChart(it) }
    }
}