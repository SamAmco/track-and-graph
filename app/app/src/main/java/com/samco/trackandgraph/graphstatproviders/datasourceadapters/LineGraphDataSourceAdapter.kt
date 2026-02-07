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
import com.samco.trackandgraph.data.database.dto.LineGraphConfig
import com.samco.trackandgraph.data.database.dto.LineGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphFeatureConfig
import com.samco.trackandgraph.data.database.dto.LineGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.data.interactor.DataInteractor
import javax.inject.Inject

class LineGraphDataSourceAdapter @Inject constructor(
    dataInteractor: DataInteractor
) : GraphStatDataSourceAdapter<LineGraphWithFeatures>(dataInteractor) {
    override suspend fun writeConfigToDatabase(
        graphOrStat: GraphOrStat,
        config: LineGraphWithFeatures,
        updateMode: Boolean
    ) {
        val lineGraphConfig = LineGraphConfig(
            features = config.features.map { feature ->
                LineGraphFeatureConfig(
                    featureId = feature.featureId,
                    name = feature.name,
                    colorIndex = feature.colorIndex,
                    averagingMode = feature.averagingMode,
                    plottingMode = feature.plottingMode,
                    pointStyle = feature.pointStyle,
                    offset = feature.offset,
                    scale = feature.scale,
                    durationPlottingMode = feature.durationPlottingMode
                )
            },
            sampleSize = config.sampleSize,
            yRangeType = config.yRangeType,
            yFrom = config.yFrom,
            yTo = config.yTo,
            endDate = config.endDate
        )

        if (updateMode) {
            dataInteractor.updateLineGraph(
                LineGraphUpdateRequest(
                    graphStatId = graphOrStat.id,
                    name = graphOrStat.name,
                    config = lineGraphConfig
                )
            )
        } else {
            dataInteractor.createLineGraph(
                LineGraphCreateRequest(
                    name = graphOrStat.name,
                    groupId = graphOrStat.groupId,
                    config = lineGraphConfig
                )
            )
        }
    }

    override suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, LineGraphWithFeatures>? {
        val lineGraph = dataInteractor.getLineGraphByGraphStatId(graphOrStatId) ?: return null
        return Pair(lineGraph.id, lineGraph)
    }

    override suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat) {
        dataInteractor.duplicateLineGraph(graphOrStat.id)
    }
}
