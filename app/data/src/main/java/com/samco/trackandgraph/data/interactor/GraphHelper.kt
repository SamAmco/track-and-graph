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

package com.samco.trackandgraph.data.interactor

import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStat
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatCreateRequest
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatUpdateRequest
import com.samco.trackandgraph.data.database.dto.BarChart
import com.samco.trackandgraph.data.database.dto.BarChartCreateRequest
import com.samco.trackandgraph.data.database.dto.BarChartUpdateRequest
import com.samco.trackandgraph.data.database.dto.GraphDeleteRequest
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.LastValueStat
import com.samco.trackandgraph.data.database.dto.LastValueStatCreateRequest
import com.samco.trackandgraph.data.database.dto.LastValueStatUpdateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.LuaGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LuaGraphUpdateRequest
import com.samco.trackandgraph.data.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.PieChart
import com.samco.trackandgraph.data.database.dto.PieChartCreateRequest
import com.samco.trackandgraph.data.database.dto.PieChartUpdateRequest
import com.samco.trackandgraph.data.database.dto.TimeHistogram
import com.samco.trackandgraph.data.database.dto.TimeHistogramCreateRequest
import com.samco.trackandgraph.data.database.dto.TimeHistogramUpdateRequest

/**
 * An interface for managing graphs and stats. Do not use this interface directly, it is implemented
 * by the DataInteractor interface.
 *
 * The implementation of GraphHelper will manage the complete lifecycle of graphs and stats.
 * It will perform all changes inside a transaction and throw an exception if anything goes wrong.
 */
interface GraphHelper {

    // =========================================================================
    // Create methods
    // =========================================================================

    /**
     * Creates a new line graph and returns the graphStatId.
     */
    suspend fun createLineGraph(request: LineGraphCreateRequest): Long

    /**
     * Creates a new pie chart and returns the graphStatId.
     */
    suspend fun createPieChart(request: PieChartCreateRequest): Long

    /**
     * Creates a new average time between stat and returns the graphStatId.
     */
    suspend fun createAverageTimeBetweenStat(request: AverageTimeBetweenStatCreateRequest): Long

    /**
     * Creates a new time histogram and returns the graphStatId.
     */
    suspend fun createTimeHistogram(request: TimeHistogramCreateRequest): Long

    /**
     * Creates a new last value stat and returns the graphStatId.
     */
    suspend fun createLastValueStat(request: LastValueStatCreateRequest): Long

    /**
     * Creates a new bar chart and returns the graphStatId.
     */
    suspend fun createBarChart(request: BarChartCreateRequest): Long

    /**
     * Creates a new Lua graph and returns the graphStatId.
     */
    suspend fun createLuaGraph(request: LuaGraphCreateRequest): Long

    // =========================================================================
    // Update methods
    // =========================================================================

    /**
     * Updates an existing line graph.
     * Only non-null fields in the request will be changed.
     */
    suspend fun updateLineGraph(request: LineGraphUpdateRequest)

    /**
     * Updates an existing pie chart.
     * Only non-null fields in the request will be changed.
     */
    suspend fun updatePieChart(request: PieChartUpdateRequest)

    /**
     * Updates an existing average time between stat.
     * Only non-null fields in the request will be changed.
     */
    suspend fun updateAverageTimeBetweenStat(request: AverageTimeBetweenStatUpdateRequest)

    /**
     * Updates an existing time histogram.
     * Only non-null fields in the request will be changed.
     */
    suspend fun updateTimeHistogram(request: TimeHistogramUpdateRequest)

    /**
     * Updates an existing last value stat.
     * Only non-null fields in the request will be changed.
     */
    suspend fun updateLastValueStat(request: LastValueStatUpdateRequest)

    /**
     * Updates an existing bar chart.
     * Only non-null fields in the request will be changed.
     */
    suspend fun updateBarChart(request: BarChartUpdateRequest)

    /**
     * Updates an existing Lua graph.
     * Only non-null fields in the request will be changed.
     */
    suspend fun updateLuaGraph(request: LuaGraphUpdateRequest)

    // =========================================================================
    // Delete method
    // =========================================================================

    /**
     * Deletes a graph or stat.
     */
    suspend fun deleteGraph(request: GraphDeleteRequest)

    // =========================================================================
    // Duplicate methods
    // =========================================================================

    /**
     * Duplicates an existing line graph and returns the new graphStatId.
     */
    suspend fun duplicateLineGraph(graphStatId: Long): Long?

    /**
     * Duplicates an existing pie chart and returns the new graphStatId.
     */
    suspend fun duplicatePieChart(graphStatId: Long): Long?

    /**
     * Duplicates an existing average time between stat and returns the new graphStatId.
     */
    suspend fun duplicateAverageTimeBetweenStat(graphStatId: Long): Long?

    /**
     * Duplicates an existing time histogram and returns the new graphStatId.
     */
    suspend fun duplicateTimeHistogram(graphStatId: Long): Long?

    /**
     * Duplicates an existing last value stat and returns the new graphStatId.
     */
    suspend fun duplicateLastValueStat(graphStatId: Long): Long?

    /**
     * Duplicates an existing bar chart and returns the new graphStatId.
     */
    suspend fun duplicateBarChart(graphStatId: Long): Long?

    /**
     * Duplicates an existing Lua graph and returns the new graphStatId.
     */
    suspend fun duplicateLuaGraph(graphStatId: Long): Long?

    // =========================================================================
    // Get methods
    // =========================================================================

    suspend fun getGraphStatById(graphStatId: Long): GraphOrStat

    suspend fun tryGetGraphStatById(graphStatId: Long): GraphOrStat?

    suspend fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures?

    suspend fun getPieChartByGraphStatId(graphStatId: Long): PieChart?

    suspend fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    suspend fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram?

    suspend fun getLastValueStatByGraphStatId(graphStatId: Long): LastValueStat?

    suspend fun getBarChartByGraphStatId(graphStatId: Long): BarChart?

    suspend fun getLuaGraphByGraphStatId(graphStatId: Long): LuaGraphWithFeatures?

    suspend fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat>

    suspend fun getAllGraphStatsSync(): List<GraphOrStat>

    // =========================================================================
    // Utility methods
    // =========================================================================

    suspend fun hasAnyGraphs(): Boolean

    suspend fun hasAnyLuaGraphs(): Boolean
}
