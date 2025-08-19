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

package com.samco.trackandgraph.graphstatproviders.datasourceadapters

import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.PieChart
import com.samco.trackandgraph.data.model.DataInteractor
import javax.inject.Inject

class PieChartDataSourceAdapter @Inject constructor(
    dataInteractor: DataInteractor
) : GraphStatDataSourceAdapter<PieChart>(dataInteractor) {
    override suspend fun writeConfigToDatabase(
        graphOrStat: GraphOrStat,
        config: PieChart,
        updateMode: Boolean
    ) {
        if (updateMode) dataInteractor.updatePieChart(graphOrStat, config)
        else dataInteractor.insertPieChart(graphOrStat, config)
    }

    override suspend fun getConfigDataFromDatabase(
        graphOrStatId: Long
    ): Pair<Long, PieChart>? {
        val pieChart = dataInteractor.getPieChartByGraphStatId(graphOrStatId) ?: return null
        return Pair(pieChart.id, pieChart)
    }

    override suspend fun shouldPreen(
        graphOrStat: GraphOrStat
    ): Boolean {
        return dataInteractor.getPieChartByGraphStatId(graphOrStat.id) == null
    }

    override suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat) {
        dataInteractor.duplicatePieChart(graphOrStat)
    }
}