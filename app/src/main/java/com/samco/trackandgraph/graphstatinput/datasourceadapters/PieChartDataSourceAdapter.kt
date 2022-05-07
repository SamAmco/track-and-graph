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
import com.samco.trackandgraph.base.database.dto.PieChart
import com.samco.trackandgraph.base.model.DataInteractor

class PieChartDataSourceAdapter : GraphStatDataSourceAdapter<PieChart>() {
    override suspend fun writeConfigToDatabase(
        dataInteractor: DataInteractor,
        graphOrStatId: Long,
        config: PieChart,
        updateMode: Boolean
    ) {
        if (updateMode) dataInteractor.updatePieChart(config.copy(graphStatId = graphOrStatId))
        else dataInteractor.insertPieChart(config.copy(graphStatId = graphOrStatId))
    }

    override suspend fun getConfigDataFromDatabase(
        dataInteractor: DataInteractor,
        graphOrStatId: Long
    ): Pair<Long, PieChart>? {
        val pieChart = dataInteractor.getPieChartByGraphStatId(graphOrStatId) ?: return null
        return Pair(pieChart.id, pieChart)
    }

    override suspend fun shouldPreen(
        dataInteractor: DataInteractor,
        graphOrStat: GraphOrStat
    ): Boolean {
        return dataInteractor.getPieChartByGraphStatId(graphOrStat.id) == null
    }

    override suspend fun duplicate(
        dataInteractor: DataInteractor,
        oldGraphId: Long,
        newGraphId: Long
    ) {
        val pieChart = dataInteractor.getPieChartByGraphStatId(oldGraphId)
        val copy = pieChart?.copy(id = 0, graphStatId = newGraphId)
        copy?.let { dataInteractor.insertPieChart(it) }
    }
}