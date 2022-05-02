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
import com.samco.trackandgraph.base.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.base.database.entity.GraphOrStat


class LineGraphDataSourceAdapter : GraphStatDataSourceAdapter<LineGraphWithFeatures>() {
    override suspend fun writeConfigToDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long,
        config: LineGraphWithFeatures,
        updateMode: Boolean
    ) {
        val newLineGraphId = if (updateMode) {
            dataSource.deleteFeaturesForLineGraph(config.id)
            dataSource.updateLineGraph(config.toLineGraph().copy(graphStatId = graphOrStatId))
            config.id
        } else {
            dataSource.insertLineGraph(config.toLineGraph().copy(graphStatId = graphOrStatId))
        }
        val lineGraphFeatures = config.features
            .map { it.copy(lineGraphId = newLineGraphId) }
        dataSource.insertLineGraphFeatures(lineGraphFeatures)
    }

    override suspend fun getConfigDataFromDatabase(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStatId: Long
    ): Pair<Long, LineGraphWithFeatures>? {
        val lineGraph = dataSource.getLineGraphByGraphStatId(graphOrStatId) ?: return null
        return Pair(lineGraph.id, lineGraph)
    }

    override suspend fun shouldPreen(
        dataSource: TrackAndGraphDatabaseDao,
        graphOrStat: GraphOrStat
    ): Boolean {
        val lineGraph = dataSource.getLineGraphByGraphStatId(graphOrStat.id) ?: return true
        //If the feature was deleted then it should have been deleted via a cascade rule in the db
        // so the any statement should not strictly be necessary.
        return lineGraph.features.isEmpty() || lineGraph.features.any {
            dataSource.tryGetFeatureByIdSync(it.featureId) == null
        }
    }

    override suspend fun duplicate(
        dataSource: TrackAndGraphDatabaseDao,
        oldGraphId: Long,
        newGraphId: Long
    ) {
        val lineGraph = dataSource.getLineGraphByGraphStatId(oldGraphId)
        lineGraph?.let {
            val copy = it.toLineGraph().copy(id = 0, graphStatId = newGraphId)
            val newLineGraphId = dataSource.insertLineGraph(copy)
            val newFeatures = lineGraph.features.map { f ->
                f.copy(id = 0, lineGraphId = newLineGraphId)
            }
            dataSource.insertLineGraphFeatures(newFeatures)
        }
    }
}