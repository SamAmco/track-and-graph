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

import com.samco.trackandgraph.BuildConfig
import com.samco.trackandgraph.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.database.entity.GraphOrStat
import kotlin.random.Random

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
        return lineGraph.features.any { dataSource.tryGetFeatureByIdSync(it.featureId) == null }
    }

    override suspend fun duplicate(
        dataSource: TrackAndGraphDatabaseDao,
        oldGraphId: Long,
        newGraphId: Long
    ) {
        if (BuildConfig.DEBUG) return duplicateRandomOffsetAndScale(dataSource, oldGraphId, newGraphId)

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

    suspend fun duplicateRandomOffsetAndScale(
            dataSource: TrackAndGraphDatabaseDao,
            oldGraphId: Long,
            newGraphId: Long
    ) {
        val offset = Random.nextInt(15,5000).toDouble() / 10.0
        val scale = 1 + Random.nextDouble()*500

        val lineGraph = dataSource.getLineGraphByGraphStatId(oldGraphId)
        lineGraph?.let {
            val copy = it.toLineGraph().copy(id = 0, graphStatId = newGraphId)
            val newLineGraphId = dataSource.insertLineGraph(copy)
            val newFeatures = lineGraph.features.map { f ->
                f.copy(id = 0, lineGraphId = newLineGraphId, offset = offset, scale = scale )
            }
            dataSource.insertLineGraphFeatures(newFeatures)
        }
    }
}