/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.fixtures

import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import org.threeten.bp.OffsetDateTime

fun testTracker(
    id: Long,
    name: String = "tracker $id",
    featureId: Long = id,
    description: String = "",
) = Tracker(
    id = id,
    name = name,
    featureId = featureId,
    description = description,
    dataType = DataType.CONTINUOUS,
    hasDefaultValue = false,
    defaultValue = 0.0,
    defaultLabel = "",
    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
    suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
)

fun testDisplayTracker(
    id: Long,
    name: String = "tracker $id",
    featureId: Long = id,
    description: String = "",
    timestamp: OffsetDateTime = OffsetDateTime.now(),
) = DisplayTracker(
    id = id,
    featureId = featureId,
    name = name,
    dataType = DataType.CONTINUOUS,
    hasDefaultValue = false,
    defaultValue = 0.0,
    defaultLabel = "",
    timestamp = timestamp,
    description = description,
    timerStartInstant = null,
    unique = true,
)

fun testGraphOrStat(
    id: Long,
    name: String = "graph $id",
    type: GraphStatType = GraphStatType.LINE_GRAPH,
) = GraphOrStat(
    id = id,
    name = name,
    type = type,
    unique = true,
)

fun emptyFunctionGraph(): FunctionGraph {
    val outputNode = FunctionGraphNode.OutputNode(
        x = 0f,
        y = 0f,
        id = 1,
        dependencies = emptyList(),
    )
    return FunctionGraph(
        nodes = listOf(outputNode),
        outputNode = outputNode,
        isDuration = false,
    )
}

fun testFunction(
    id: Long,
    name: String = "function $id",
    featureId: Long = id,
    description: String = "",
    functionGraph: FunctionGraph = emptyFunctionGraph(),
) = Function(
    id = id,
    featureId = featureId,
    name = name,
    description = description,
    functionGraph = functionGraph,
    inputFeatureIds = emptyList(),
    unique = true,
)

data class TestGraphViewData(
    override val graphOrStat: GraphOrStat,
    override val state: IGraphStatViewData.State,
    val label: String = graphOrStat.name,
    override val error: Throwable? = null,
) : IGraphStatViewData

fun testGraphViewData(
    graphOrStat: GraphOrStat,
    state: IGraphStatViewData.State = IGraphStatViewData.State.READY,
    label: String = graphOrStat.name,
    error: Throwable? = null,
) = TestGraphViewData(
    graphOrStat = graphOrStat,
    state = state,
    label = label,
    error = error,
)
