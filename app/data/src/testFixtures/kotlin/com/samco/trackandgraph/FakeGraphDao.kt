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

package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.GraphDao
import com.samco.trackandgraph.data.database.dto.GraphEndDate
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
 * A fake in-memory implementation of [GraphDao] for testing purposes.
 * This allows tests to run without a real database and without mocking frameworks.
 */
internal class FakeGraphDao : GraphDao {

    private var nextGraphStatId = 1L
    private var nextLineGraphId = 1L
    private var nextPieChartId = 1L
    private var nextAverageTimeBetweenStatId = 1L
    private var nextTimeHistogramId = 1L
    private var nextLastValueStatId = 1L
    private var nextBarChartId = 1L
    private var nextLuaGraphId = 1L
    private var nextLineGraphFeatureId = 1L
    private var nextLuaGraphFeatureId = 1L

    private val graphOrStats = mutableMapOf<Long, GraphOrStat>()
    private val lineGraphs = mutableMapOf<Long, LineGraph>()
    private val lineGraphFeatures = mutableMapOf<Long, MutableList<LineGraphFeature>>()
    private val pieCharts = mutableMapOf<Long, PieChart>()
    private val averageTimeBetweenStats = mutableMapOf<Long, AverageTimeBetweenStat>()
    private val timeHistograms = mutableMapOf<Long, TimeHistogram>()
    private val lastValueStats = mutableMapOf<Long, LastValueStat>()
    private val barCharts = mutableMapOf<Long, BarChart>()
    private val luaGraphs = mutableMapOf<Long, LuaGraph>()
    private val luaGraphFeatures = mutableMapOf<Long, MutableList<LuaGraphFeature>>()

    // =========================================================================
    // GraphOrStat operations
    // =========================================================================

    override fun insertGraphOrStat(graphOrStat: GraphOrStat): Long {
        val id = if (graphOrStat.id == 0L) nextGraphStatId++ else graphOrStat.id
        graphOrStats[id] = graphOrStat.copy(id = id)
        return id
    }

    override fun updateGraphOrStat(graphOrStat: GraphOrStat) {
        graphOrStats[graphOrStat.id] = graphOrStat
    }

    override fun deleteGraphOrStat(id: Long) {
        graphOrStats.remove(id)
        // Cascade delete related entities
        lineGraphs.entries.removeIf { it.value.graphStatId == id }
        pieCharts.entries.removeIf { it.value.graphStatId == id }
        averageTimeBetweenStats.entries.removeIf { it.value.graphStatId == id }
        timeHistograms.entries.removeIf { it.value.graphStatId == id }
        lastValueStats.entries.removeIf { it.value.graphStatId == id }
        barCharts.entries.removeIf { it.value.graphStatId == id }
        luaGraphs.entries.removeIf { it.value.graphStatId == id }
    }

    override fun getGraphStatById(graphStatId: Long): GraphOrStat {
        return graphOrStats[graphStatId]
            ?: throw NoSuchElementException("GraphOrStat not found: $graphStatId")
    }

    override fun tryGetGraphStatById(graphStatId: Long): GraphOrStat? {
        return graphOrStats[graphStatId]
    }

    override fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat> {
        return graphOrStats.values
            .filter { it.groupId == groupId }
            .sortedWith(compareBy({ it.displayIndex }, { -it.id }))
    }

    override fun getAllGraphStatsSync(): List<GraphOrStat> {
        return graphOrStats.values
            .sortedWith(compareBy({ it.displayIndex }, { -it.id }))
    }

    override fun hasAnyGraphs(): Boolean = graphOrStats.isNotEmpty()

    override fun hasAnyLuaGraphs(): Boolean = luaGraphs.isNotEmpty()

    // =========================================================================
    // LineGraph operations
    // =========================================================================

    override fun insertLineGraph(lineGraph: LineGraph): Long {
        val id = if (lineGraph.id == 0L) nextLineGraphId++ else lineGraph.id
        lineGraphs[id] = lineGraph.copy(id = id)
        lineGraphFeatures.getOrPut(id) { mutableListOf() }
        return id
    }

    override fun updateLineGraph(lineGraph: LineGraph) {
        lineGraphs[lineGraph.id] = lineGraph
    }

    override fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures? {
        val lineGraph = lineGraphs.values.find { it.graphStatId == graphStatId } ?: return null
        val features = lineGraphFeatures[lineGraph.id] ?: emptyList()
        return LineGraphWithFeatures(
            id = lineGraph.id,
            graphStatId = lineGraph.graphStatId,
            sampleSize = lineGraph.sampleSize,
            yRangeType = lineGraph.yRangeType,
            yFrom = lineGraph.yFrom,
            yTo = lineGraph.yTo,
            endDate = lineGraph.endDate ?: GraphEndDate.Latest,
            features = features
        )
    }

    override fun insertLineGraphFeatures(lineGraphFeatures: List<LineGraphFeature>) {
        for (feature in lineGraphFeatures) {
            val id = if (feature.id == 0L) nextLineGraphFeatureId++ else feature.id
            val featureWithId = feature.copy(id = id)
            this.lineGraphFeatures.getOrPut(feature.lineGraphId) { mutableListOf() }.add(featureWithId)
        }
    }

    override fun deleteFeaturesForLineGraph(lineGraphId: Long) {
        lineGraphFeatures[lineGraphId]?.clear()
    }

    // =========================================================================
    // PieChart operations
    // =========================================================================

    override fun insertPieChart(pieChart: PieChart): Long {
        val id = if (pieChart.id == 0L) nextPieChartId++ else pieChart.id
        pieCharts[id] = pieChart.copy(id = id)
        return id
    }

    override fun updatePieChart(pieChart: PieChart) {
        pieCharts[pieChart.id] = pieChart
    }

    override fun getPieChartByGraphStatId(graphStatId: Long): PieChart? {
        return pieCharts.values.find { it.graphStatId == graphStatId }
    }

    // =========================================================================
    // AverageTimeBetweenStat operations
    // =========================================================================

    override fun insertAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat): Long {
        val id = if (averageTimeBetweenStat.id == 0L) nextAverageTimeBetweenStatId++ else averageTimeBetweenStat.id
        averageTimeBetweenStats[id] = averageTimeBetweenStat.copy(id = id)
        return id
    }

    override fun updateAverageTimeBetweenStat(averageTimeBetweenStat: AverageTimeBetweenStat) {
        averageTimeBetweenStats[averageTimeBetweenStat.id] = averageTimeBetweenStat
    }

    override fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat? {
        return averageTimeBetweenStats.values.find { it.graphStatId == graphStatId }
    }

    // =========================================================================
    // TimeHistogram operations
    // =========================================================================

    override fun insertTimeHistogram(timeHistogram: TimeHistogram): Long {
        val id = if (timeHistogram.id == 0L) nextTimeHistogramId++ else timeHistogram.id
        timeHistograms[id] = timeHistogram.copy(id = id)
        return id
    }

    override fun updateTimeHistogram(timeHistogram: TimeHistogram) {
        timeHistograms[timeHistogram.id] = timeHistogram
    }

    override fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram? {
        return timeHistograms.values.find { it.graphStatId == graphStatId }
    }

    // =========================================================================
    // LastValueStat operations
    // =========================================================================

    override fun insertLastValueStat(lastValueStat: LastValueStat): Long {
        val id = if (lastValueStat.id == 0L) nextLastValueStatId++ else lastValueStat.id
        lastValueStats[id] = lastValueStat.copy(id = id)
        return id
    }

    override fun updateLastValueStat(lastValueStat: LastValueStat) {
        lastValueStats[lastValueStat.id] = lastValueStat
    }

    override fun getLastValueStatByGraphStatId(graphStatId: Long): LastValueStat? {
        return lastValueStats.values.find { it.graphStatId == graphStatId }
    }

    // =========================================================================
    // BarChart operations
    // =========================================================================

    override fun insertBarChart(barChart: BarChart): Long {
        val id = if (barChart.id == 0L) nextBarChartId++ else barChart.id
        barCharts[id] = barChart.copy(id = id)
        return id
    }

    override fun updateBarChart(barChart: BarChart) {
        barCharts[barChart.id] = barChart
    }

    override fun getBarChartByGraphStatId(graphStatId: Long): BarChart? {
        return barCharts.values.find { it.graphStatId == graphStatId }
    }

    // =========================================================================
    // LuaGraph operations
    // =========================================================================

    override fun insertLuaGraph(luaGraph: LuaGraph): Long {
        val id = if (luaGraph.id == 0L) nextLuaGraphId++ else luaGraph.id
        luaGraphs[id] = luaGraph.copy(id = id)
        luaGraphFeatures.getOrPut(id) { mutableListOf() }
        return id
    }

    override fun updateLuaGraph(luaGraph: LuaGraph) {
        luaGraphs[luaGraph.id] = luaGraph
    }

    override fun getLuaGraphByGraphStatId(graphStatId: Long): LuaGraphWithFeatures? {
        val luaGraph = luaGraphs.values.find { it.graphStatId == graphStatId } ?: return null
        val features = luaGraphFeatures[luaGraph.id] ?: emptyList()
        return LuaGraphWithFeatures(
            id = luaGraph.id,
            graphStatId = luaGraph.graphStatId,
            script = luaGraph.script,
            features = features
        )
    }

    override fun insertLuaGraphFeatures(luaGraphFeatures: List<LuaGraphFeature>) {
        for (feature in luaGraphFeatures) {
            val id = if (feature.id == 0L) nextLuaGraphFeatureId++ else feature.id
            val featureWithId = feature.copy(id = id)
            this.luaGraphFeatures.getOrPut(feature.luaGraphId) { mutableListOf() }.add(featureWithId)
        }
    }

    override fun deleteFeaturesForLuaGraph(luaGraphId: Long) {
        luaGraphFeatures[luaGraphId]?.clear()
    }

    // =========================================================================
    // Test helper methods
    // =========================================================================

    fun clear() {
        graphOrStats.clear()
        lineGraphs.clear()
        lineGraphFeatures.clear()
        pieCharts.clear()
        averageTimeBetweenStats.clear()
        timeHistograms.clear()
        lastValueStats.clear()
        barCharts.clear()
        luaGraphs.clear()
        luaGraphFeatures.clear()

        nextGraphStatId = 1L
        nextLineGraphId = 1L
        nextPieChartId = 1L
        nextAverageTimeBetweenStatId = 1L
        nextTimeHistogramId = 1L
        nextLastValueStatId = 1L
        nextBarChartId = 1L
        nextLuaGraphId = 1L
        nextLineGraphFeatureId = 1L
        nextLuaGraphFeatureId = 1L
    }
}
