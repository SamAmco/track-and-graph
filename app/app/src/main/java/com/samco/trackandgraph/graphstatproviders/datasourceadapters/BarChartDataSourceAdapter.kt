package com.samco.trackandgraph.graphstatproviders.datasourceadapters

import com.samco.trackandgraph.data.database.dto.BarChart
import com.samco.trackandgraph.data.database.dto.BarChartConfig
import com.samco.trackandgraph.data.database.dto.BarChartCreateRequest
import com.samco.trackandgraph.data.database.dto.BarChartUpdateRequest
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
        val barChartConfig = BarChartConfig(
            featureId = config.featureId,
            endDate = config.endDate,
            sampleSize = config.sampleSize,
            yRangeType = config.yRangeType,
            yTo = config.yTo,
            scale = config.scale,
            barPeriod = config.barPeriod,
            sumByCount = config.sumByCount
        )

        if (updateMode) {
            dataInteractor.updateBarChart(
                BarChartUpdateRequest(
                    graphStatId = graphOrStat.id,
                    name = graphOrStat.name,
                    config = barChartConfig
                )
            )
        } else {
            dataInteractor.createBarChart(
                BarChartCreateRequest(
                    name = graphOrStat.name,
                    groupId = graphOrStat.groupId,
                    config = barChartConfig
                )
            )
        }
    }

    override suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, BarChart>? {
        val lvs = dataInteractor.getBarChartByGraphStatId(graphOrStatId) ?: return null
        return Pair(lvs.id, lvs)
    }

    override suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat) {
        dataInteractor.duplicateBarChart(graphOrStat.id)
    }
}
