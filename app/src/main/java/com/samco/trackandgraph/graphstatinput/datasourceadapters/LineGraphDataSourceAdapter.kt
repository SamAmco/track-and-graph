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
import com.samco.trackandgraph.base.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.base.model.DataInteractor
import javax.inject.Inject

class LineGraphDataSourceAdapter @Inject constructor(
    dataInteractor: DataInteractor
) : GraphStatDataSourceAdapter<LineGraphWithFeatures>(dataInteractor) {
    override suspend fun writeConfigToDatabase(
        graphOrStat: GraphOrStat,
        config: LineGraphWithFeatures,
        updateMode: Boolean
    ) {
        if (updateMode) dataInteractor.updateLineGraph(graphOrStat, config)
        else dataInteractor.insertLineGraph(graphOrStat, config)
    }

    override suspend fun getConfigDataFromDatabase(graphOrStatId: Long): Pair<Long, LineGraphWithFeatures>? {
        val lineGraph = dataInteractor.getLineGraphByGraphStatId(graphOrStatId) ?: return null
        return Pair(lineGraph.id, lineGraph)
    }

    override suspend fun shouldPreen(graphOrStat: GraphOrStat): Boolean {
        val lineGraph = dataInteractor.getLineGraphByGraphStatId(graphOrStat.id) ?: return true
        //If the feature was deleted then it should have been deleted via a cascade rule in the db
        // so the any statement should not strictly be necessary.
        return lineGraph.features.isEmpty() || lineGraph.features.any {
            dataInteractor.tryGetFeatureByIdSync(it.featureId) == null
        }
    }

    override suspend fun duplicateGraphOrStat(graphOrStat: GraphOrStat) {
        dataInteractor.duplicateLineGraph(graphOrStat)
    }
}