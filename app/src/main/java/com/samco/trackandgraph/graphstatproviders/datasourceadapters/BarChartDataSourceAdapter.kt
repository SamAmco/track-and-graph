package com.samco.trackandgraph.graphstatproviders.datasourceadapters

import com.samco.trackandgraph.data.database.dto.BarChart
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.interactor.DataInteractor
import javax.inject.Inject

class BarChartDataSourceAdapter @Inject constructor(
    dataInteractor: DataInteractor
) : GraphStatDataSourceAdapter<BarChart>(dataInteractor) {
    override suspend fun writeConfigToDatabase(
        graphOrStat: GraphOrStat,
        config: BarChart,
        updateMode: Boolean
    ) {
        if (updateMode) dataInteractor.updateBarChart(graphOrStat, config)
        else dataInteractor.insertBarChart(graphOrStat, config)
    }

    override suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, BarChart>? {
        val lvs = dataInteractor.getBarChartByGraphStatId(graphOrStatId) ?: return null
        return Pair(lvs.id, lvs)
    }

    override suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat) {
        dataInteractor.duplicateBarChart(graphOrStat)
    }
}