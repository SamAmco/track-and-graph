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

package com.samco.trackandgraph.data.database.dto

import org.threeten.bp.temporal.TemporalAmount

// =============================================================================
// Feature configuration types (without implementation detail ids)
// =============================================================================

/**
 * Configuration for a feature within a line graph.
 * Does not include id or lineGraphId as these are implementation details.
 */
data class LineGraphFeatureConfig(
    val featureId: Long,
    val name: String,
    val colorIndex: Int,
    val averagingMode: LineGraphAveragingModes,
    val plottingMode: LineGraphPlottingModes,
    val pointStyle: LineGraphPointStyle,
    val offset: Double,
    val scale: Double,
    val durationPlottingMode: DurationPlottingMode
)

/**
 * Configuration for a feature within a Lua graph.
 * Does not include id or luaGraphId as these are implementation details.
 */
data class LuaGraphFeatureConfig(
    val featureId: Long,
    val name: String
)

// =============================================================================
// Graph configuration types (without implementation detail ids)
// =============================================================================

/**
 * Configuration for a line graph.
 * Does not include id or graphStatId as these are implementation details.
 */
data class LineGraphConfig(
    val features: List<LineGraphFeatureConfig>,
    val sampleSize: TemporalAmount?,
    val yRangeType: YRangeType,
    val yFrom: Double,
    val yTo: Double,
    val endDate: GraphEndDate
)

/**
 * Configuration for a pie chart.
 * Does not include id or graphStatId as these are implementation details.
 */
data class PieChartConfig(
    val featureId: Long,
    val sampleSize: TemporalAmount?,
    val endDate: GraphEndDate,
    val sumByCount: Boolean
)

/**
 * Configuration for an average time between stat.
 * Does not include id or graphStatId as these are implementation details.
 */
data class AverageTimeBetweenStatConfig(
    val featureId: Long,
    val fromValue: Double,
    val toValue: Double,
    val sampleSize: TemporalAmount?,
    val labels: List<String>,
    val endDate: GraphEndDate,
    val filterByRange: Boolean,
    val filterByLabels: Boolean
)

/**
 * Configuration for a time histogram.
 * Does not include id or graphStatId as these are implementation details.
 */
data class TimeHistogramConfig(
    val featureId: Long,
    val sampleSize: TemporalAmount?,
    val window: TimeHistogramWindow,
    val sumByCount: Boolean,
    val endDate: GraphEndDate
)

/**
 * Configuration for a last value stat.
 * Does not include id or graphStatId as these are implementation details.
 */
data class LastValueStatConfig(
    val featureId: Long,
    val endDate: GraphEndDate,
    val fromValue: Double,
    val toValue: Double,
    val labels: List<String>,
    val filterByRange: Boolean,
    val filterByLabels: Boolean
)

/**
 * Configuration for a bar chart.
 * Does not include id or graphStatId as these are implementation details.
 */
data class BarChartConfig(
    val featureId: Long,
    val endDate: GraphEndDate,
    val sampleSize: TemporalAmount?,
    val yRangeType: YRangeType,
    val yTo: Double,
    val scale: Double,
    val barPeriod: BarChartBarPeriod,
    val sumByCount: Boolean
)

/**
 * Configuration for a Lua graph.
 * Does not include id or graphStatId as these are implementation details.
 */
data class LuaGraphConfig(
    val features: List<LuaGraphFeatureConfig>,
    val script: String
)

// =============================================================================
// Create Request types
// =============================================================================

/**
 * Request object for creating a new line graph.
 * Note: id, graphStatId, and displayIndex are handled by the data layer.
 */
data class LineGraphCreateRequest(
    val name: String,
    val groupId: Long,
    val config: LineGraphConfig
)

/**
 * Request object for creating a new pie chart.
 * Note: id, graphStatId, and displayIndex are handled by the data layer.
 */
data class PieChartCreateRequest(
    val name: String,
    val groupId: Long,
    val config: PieChartConfig
)

/**
 * Request object for creating a new average time between stat.
 * Note: id, graphStatId, and displayIndex are handled by the data layer.
 */
data class AverageTimeBetweenStatCreateRequest(
    val name: String,
    val groupId: Long,
    val config: AverageTimeBetweenStatConfig
)

/**
 * Request object for creating a new time histogram.
 * Note: id, graphStatId, and displayIndex are handled by the data layer.
 */
data class TimeHistogramCreateRequest(
    val name: String,
    val groupId: Long,
    val config: TimeHistogramConfig
)

/**
 * Request object for creating a new last value stat.
 * Note: id, graphStatId, and displayIndex are handled by the data layer.
 */
data class LastValueStatCreateRequest(
    val name: String,
    val groupId: Long,
    val config: LastValueStatConfig
)

/**
 * Request object for creating a new bar chart.
 * Note: id, graphStatId, and displayIndex are handled by the data layer.
 */
data class BarChartCreateRequest(
    val name: String,
    val groupId: Long,
    val config: BarChartConfig
)

/**
 * Request object for creating a new Lua graph.
 * Note: id, graphStatId, and displayIndex are handled by the data layer.
 */
data class LuaGraphCreateRequest(
    val name: String,
    val groupId: Long,
    val config: LuaGraphConfig
)

// =============================================================================
// Update Request types
// =============================================================================

/**
 * Request object for updating an existing line graph.
 * All fields except [graphStatId] are optional. A null value means "don't change this field".
 * Note: To move a graph between groups, use [MoveComponentRequest] instead.
 */
data class LineGraphUpdateRequest(
    val graphStatId: Long,
    val name: String? = null,
    val config: LineGraphConfig? = null
)

/**
 * Request object for updating an existing pie chart.
 * All fields except [graphStatId] are optional. A null value means "don't change this field".
 * Note: To move a graph between groups, use [MoveComponentRequest] instead.
 */
data class PieChartUpdateRequest(
    val graphStatId: Long,
    val name: String? = null,
    val config: PieChartConfig? = null
)

/**
 * Request object for updating an existing average time between stat.
 * All fields except [graphStatId] are optional. A null value means "don't change this field".
 * Note: To move a graph between groups, use [MoveComponentRequest] instead.
 */
data class AverageTimeBetweenStatUpdateRequest(
    val graphStatId: Long,
    val name: String? = null,
    val config: AverageTimeBetweenStatConfig? = null
)

/**
 * Request object for updating an existing time histogram.
 * All fields except [graphStatId] are optional. A null value means "don't change this field".
 * Note: To move a graph between groups, use [MoveComponentRequest] instead.
 */
data class TimeHistogramUpdateRequest(
    val graphStatId: Long,
    val name: String? = null,
    val config: TimeHistogramConfig? = null
)

/**
 * Request object for updating an existing last value stat.
 * All fields except [graphStatId] are optional. A null value means "don't change this field".
 * Note: To move a graph between groups, use [MoveComponentRequest] instead.
 */
data class LastValueStatUpdateRequest(
    val graphStatId: Long,
    val name: String? = null,
    val config: LastValueStatConfig? = null
)

/**
 * Request object for updating an existing bar chart.
 * All fields except [graphStatId] are optional. A null value means "don't change this field".
 * Note: To move a graph between groups, use [MoveComponentRequest] instead.
 */
data class BarChartUpdateRequest(
    val graphStatId: Long,
    val name: String? = null,
    val config: BarChartConfig? = null
)

/**
 * Request object for updating an existing Lua graph.
 * All fields except [graphStatId] are optional. A null value means "don't change this field".
 * Note: To move a graph between groups, use [MoveComponentRequest] instead.
 */
data class LuaGraphUpdateRequest(
    val graphStatId: Long,
    val name: String? = null,
    val config: LuaGraphConfig? = null
)

// =============================================================================
// Delete Request type
// =============================================================================

/**
 * Request object for deleting a graph or stat.
 *
 * @param graphStatId The ID of the graph/stat to delete.
 */
data class GraphDeleteRequest(
    val graphStatId: Long
)
