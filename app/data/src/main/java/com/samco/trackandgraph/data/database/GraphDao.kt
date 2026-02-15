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

package com.samco.trackandgraph.data.database

import com.samco.trackandgraph.data.database.entity.AverageTimeBetweenStat
import com.samco.trackandgraph.data.database.entity.BarChart
import com.samco.trackandgraph.data.database.entity.GraphOrStat
import com.samco.trackandgraph.data.database.entity.LastValueStat
import com.samco.trackandgraph.data.database.entity.LineGraph
import com.samco.trackandgraph.data.database.entity.LineGraphFeature
import com.samco.trackandgraph.data.database.entity.LuaGraph
import com.samco.trackandgraph.data.database.entity.LuaGraphFeature
import com.samco.trackandgraph.data.database.entity.PieChart
import com.samco.trackandgraph.data.database.entity.TimeHistogram
import com.samco.trackandgraph.data.database.entity.queryresponse.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.entity.queryresponse.LuaGraphWithFeatures

/**
 * Data access interface for graph-related operations.
 * This interface abstracts the database operations needed by GraphHelper,
 * allowing for different implementations (Room, fake for testing, etc.)
 */
internal interface GraphDao {

    // =========================================================================
    // GraphOrStat operations
    // =========================================================================

    fun insertGraphOrStat(graphOrStat: GraphOrStat): Long

    fun updateGraphOrStat(graphOrStat: GraphOrStat)

    fun deleteGraphOrStat(id: Long)

    fun getGraphStatById(graphStatId: Long): GraphOrStat

    fun tryGetGraphStatById(graphStatId: Long): GraphOrStat?

    fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat>

    fun getAllGraphStatsSync(): List<GraphOrStat>

    fun hasAnyGraphs(): Boolean

    fun hasAnyLuaGraphs(): Boolean

    // =========================================================================
    // LineGraph operations
    // =========================================================================

    fun insertLineGraph(lineGraph: LineGraph): Long

    fun updateLineGraph(lineGraph: LineGraph)

    fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures?

    fun insertLineGraphFeatures(lineGraphFeatures: List<LineGraphFeature>)

    fun deleteFeaturesForLineGraph(lineGraphId: Long)

    // =========================================================================
    // PieChart operations
    // =========================================================================

    fun insertPieChart(pieChart: PieChart): Long

    fun updatePieChart(pieChart: PieChart)

    fun getPieChartByGraphStatId(graphStatId: Long): PieChart?

    // =========================================================================
    // AverageTimeBetweenStat operations
    // =========================================================================

    fun insertAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat): Long

    fun updateAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat)

    fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    // =========================================================================
    // TimeHistogram operations
    // =========================================================================

    fun insertTimeHistogram(timeHistogram: TimeHistogram): Long

    fun updateTimeHistogram(timeHistogram: TimeHistogram)

    fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram?

    // =========================================================================
    // LastValueStat operations
    // =========================================================================

    fun insertLastValueStat(lastValueStat: LastValueStat): Long

    fun updateLastValueStat(lastValueStat: LastValueStat)

    fun getLastValueStatByGraphStatId(graphStatId: Long): LastValueStat?

    // =========================================================================
    // BarChart operations
    // =========================================================================

    fun insertBarChart(barChart: BarChart): Long

    fun updateBarChart(barChart: BarChart)

    fun getBarChartByGraphStatId(graphStatId: Long): BarChart?

    // =========================================================================
    // LuaGraph operations
    // =========================================================================

    fun insertLuaGraph(luaGraph: LuaGraph): Long

    fun updateLuaGraph(luaGraph: LuaGraph)

    fun getLuaGraphByGraphStatId(graphStatId: Long): LuaGraphWithFeatures?

    fun insertLuaGraphFeatures(luaGraphFeatures: List<LuaGraphFeature>)

    fun deleteFeaturesForLuaGraph(luaGraphId: Long)
}
