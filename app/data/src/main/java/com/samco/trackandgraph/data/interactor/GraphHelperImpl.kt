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

import com.samco.trackandgraph.data.database.DatabaseTransactionHelper
import com.samco.trackandgraph.data.database.GraphDao
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStat
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatCreateRequest
import com.samco.trackandgraph.data.database.dto.AverageTimeBetweenStatUpdateRequest
import com.samco.trackandgraph.data.database.dto.BarChart
import com.samco.trackandgraph.data.database.dto.BarChartCreateRequest
import com.samco.trackandgraph.data.database.dto.BarChartUpdateRequest
import com.samco.trackandgraph.data.database.dto.GraphDeleteRequest
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
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
import com.samco.trackandgraph.data.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
internal class GraphHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val graphDao: GraphDao,
    @IODispatcher private val io: CoroutineDispatcher
) : GraphHelper {

    // =========================================================================
    // Create methods
    // =========================================================================

    override suspend fun createLineGraph(request: LineGraphCreateRequest): Long = withContext(io) {
        transactionHelper.withTransaction {
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.LINE_GRAPH)
            val lineGraphId = graphDao.insertLineGraph(
                com.samco.trackandgraph.data.database.entity.LineGraph(
                    id = 0L,
                    graphStatId = graphStatId,
                    sampleSize = request.config.sampleSize,
                    yRangeType = request.config.yRangeType,
                    yFrom = request.config.yFrom,
                    yTo = request.config.yTo,
                    endDate = request.config.endDate
                )
            )
            val features = request.config.features.map { config ->
                com.samco.trackandgraph.data.database.entity.LineGraphFeature(
                    id = 0L,
                    lineGraphId = lineGraphId,
                    featureId = config.featureId,
                    name = config.name,
                    colorIndex = config.colorIndex,
                    averagingMode = config.averagingMode,
                    plottingMode = config.plottingMode,
                    pointStyle = config.pointStyle,
                    offset = config.offset,
                    scale = config.scale,
                    durationPlottingMode = config.durationPlottingMode
                )
            }
            graphDao.insertLineGraphFeatures(features)
            graphStatId
        }
    }

    override suspend fun createPieChart(request: PieChartCreateRequest): Long = withContext(io) {
        transactionHelper.withTransaction {
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.PIE_CHART)
            graphDao.insertPieChart(
                com.samco.trackandgraph.data.database.entity.PieChart(
                    id = 0L,
                    graphStatId = graphStatId,
                    featureId = request.config.featureId,
                    sampleSize = request.config.sampleSize,
                    endDate = request.config.endDate,
                    sumByCount = request.config.sumByCount
                )
            )
            graphStatId
        }
    }

    override suspend fun createAverageTimeBetweenStat(request: AverageTimeBetweenStatCreateRequest): Long =
        withContext(io) {
            transactionHelper.withTransaction {
                val graphStatId = insertGraphOrStat(
                    request.name,
                    request.groupId,
                    GraphStatType.AVERAGE_TIME_BETWEEN
                )
                graphDao.insertAverageTimeBetweenStat(
                    com.samco.trackandgraph.data.database.entity.AverageTimeBetweenStat(
                        id = 0L,
                        graphStatId = graphStatId,
                        featureId = request.config.featureId,
                        fromValue = request.config.fromValue,
                        toValue = request.config.toValue,
                        sampleSize = request.config.sampleSize,
                        labels = request.config.labels,
                        endDate = request.config.endDate,
                        filterByRange = request.config.filterByRange,
                        filterByLabels = request.config.filterByLabels
                    )
                )
                graphStatId
            }
        }

    override suspend fun createTimeHistogram(request: TimeHistogramCreateRequest): Long =
        withContext(io) {
            transactionHelper.withTransaction {
                val graphStatId = insertGraphOrStat(
                    request.name,
                    request.groupId,
                    GraphStatType.TIME_HISTOGRAM
                )
                graphDao.insertTimeHistogram(
                    com.samco.trackandgraph.data.database.entity.TimeHistogram(
                        id = 0L,
                        graphStatId = graphStatId,
                        featureId = request.config.featureId,
                        sampleSize = request.config.sampleSize,
                        window = request.config.window,
                        sumByCount = request.config.sumByCount,
                        endDate = request.config.endDate
                    )
                )
                graphStatId
            }
        }

    override suspend fun createLastValueStat(request: LastValueStatCreateRequest): Long =
        withContext(io) {
            transactionHelper.withTransaction {
                val graphStatId = insertGraphOrStat(
                    request.name,
                    request.groupId,
                    GraphStatType.LAST_VALUE
                )
                graphDao.insertLastValueStat(
                    com.samco.trackandgraph.data.database.entity.LastValueStat(
                        id = 0L,
                        graphStatId = graphStatId,
                        featureId = request.config.featureId,
                        endDate = request.config.endDate,
                        fromValue = request.config.fromValue,
                        toValue = request.config.toValue,
                        labels = request.config.labels,
                        filterByRange = request.config.filterByRange,
                        filterByLabels = request.config.filterByLabels
                    )
                )
                graphStatId
            }
        }

    override suspend fun createBarChart(request: BarChartCreateRequest): Long = withContext(io) {
        transactionHelper.withTransaction {
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.BAR_CHART)
            graphDao.insertBarChart(
                com.samco.trackandgraph.data.database.entity.BarChart(
                    id = 0L,
                    graphStatId = graphStatId,
                    featureId = request.config.featureId,
                    endDate = request.config.endDate,
                    sampleSize = request.config.sampleSize,
                    yRangeType = request.config.yRangeType,
                    yTo = request.config.yTo,
                    scale = request.config.scale,
                    barPeriod = request.config.barPeriod,
                    sumByCount = request.config.sumByCount
                )
            )
            graphStatId
        }
    }

    override suspend fun createLuaGraph(request: LuaGraphCreateRequest): Long = withContext(io) {
        transactionHelper.withTransaction {
            val graphStatId = insertGraphOrStat(request.name, request.groupId, GraphStatType.LUA_SCRIPT)
            val luaGraphId = graphDao.insertLuaGraph(
                com.samco.trackandgraph.data.database.entity.LuaGraph(
                    id = 0L,
                    graphStatId = graphStatId,
                    script = request.config.script
                )
            )
            val features = request.config.features.map { config ->
                com.samco.trackandgraph.data.database.entity.LuaGraphFeature(
                    id = 0L,
                    luaGraphId = luaGraphId,
                    featureId = config.featureId,
                    name = config.name
                )
            }
            graphDao.insertLuaGraphFeatures(features)
            graphStatId
        }
    }

    // =========================================================================
    // Update methods
    // =========================================================================

    override suspend fun updateLineGraph(request: LineGraphUpdateRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val existing = graphDao.getGraphStatById(request.graphStatId)
            val existingLineGraph = graphDao.getLineGraphByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Line graph not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                graphDao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                graphDao.updateLineGraph(
                    existingLineGraph.toLineGraph().copy(
                        sampleSize = request.config.sampleSize,
                        yRangeType = request.config.yRangeType,
                        yFrom = request.config.yFrom,
                        yTo = request.config.yTo,
                        endDate = request.config.endDate
                    )
                )
                graphDao.deleteFeaturesForLineGraph(existingLineGraph.id)
                val features = request.config.features.map { config ->
                    com.samco.trackandgraph.data.database.entity.LineGraphFeature(
                        id = 0L,
                        lineGraphId = existingLineGraph.id,
                        featureId = config.featureId,
                        name = config.name,
                        colorIndex = config.colorIndex,
                        averagingMode = config.averagingMode,
                        plottingMode = config.plottingMode,
                        pointStyle = config.pointStyle,
                        offset = config.offset,
                        scale = config.scale,
                        durationPlottingMode = config.durationPlottingMode
                    )
                }
                graphDao.insertLineGraphFeatures(features)
            }
        }
    }

    override suspend fun updatePieChart(request: PieChartUpdateRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val existing = graphDao.getGraphStatById(request.graphStatId)
            val existingPieChart = graphDao.getPieChartByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Pie chart not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                graphDao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                graphDao.updatePieChart(
                    existingPieChart.copy(
                        featureId = request.config.featureId,
                        sampleSize = request.config.sampleSize,
                        endDate = request.config.endDate,
                        sumByCount = request.config.sumByCount
                    )
                )
            }
        }
    }

    override suspend fun updateAverageTimeBetweenStat(request: AverageTimeBetweenStatUpdateRequest) =
        withContext(io) {
            transactionHelper.withTransaction {
                val existing = graphDao.getGraphStatById(request.graphStatId)
                val existingStat = graphDao.getAverageTimeBetweenStatByGraphStatId(request.graphStatId)
                    ?: throw IllegalArgumentException("Average time between stat not found for graphStatId: ${request.graphStatId}")

                if (request.name != null) {
                    graphDao.updateGraphOrStat(existing.copy(name = request.name))
                }

                if (request.config != null) {
                    graphDao.updateAverageTimeBetweenStat(
                        existingStat.copy(
                            featureId = request.config.featureId,
                            fromValue = request.config.fromValue,
                            toValue = request.config.toValue,
                            sampleSize = request.config.sampleSize,
                            labels = request.config.labels,
                            endDate = request.config.endDate,
                            filterByRange = request.config.filterByRange,
                            filterByLabels = request.config.filterByLabels
                        )
                    )
                }
            }
        }

    override suspend fun updateTimeHistogram(request: TimeHistogramUpdateRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val existing = graphDao.getGraphStatById(request.graphStatId)
            val existingStat = graphDao.getTimeHistogramByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Time histogram not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                graphDao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                graphDao.updateTimeHistogram(
                    existingStat.copy(
                        featureId = request.config.featureId,
                        sampleSize = request.config.sampleSize,
                        window = request.config.window,
                        sumByCount = request.config.sumByCount,
                        endDate = request.config.endDate
                    )
                )
            }
        }
    }

    override suspend fun updateLastValueStat(request: LastValueStatUpdateRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val existing = graphDao.getGraphStatById(request.graphStatId)
            val existingStat = graphDao.getLastValueStatByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Last value stat not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                graphDao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                graphDao.updateLastValueStat(
                    existingStat.copy(
                        featureId = request.config.featureId,
                        endDate = request.config.endDate,
                        fromValue = request.config.fromValue,
                        toValue = request.config.toValue,
                        labels = request.config.labels,
                        filterByRange = request.config.filterByRange,
                        filterByLabels = request.config.filterByLabels
                    )
                )
            }
        }
    }

    override suspend fun updateBarChart(request: BarChartUpdateRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val existing = graphDao.getGraphStatById(request.graphStatId)
            val existingStat = graphDao.getBarChartByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Bar chart not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                graphDao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                graphDao.updateBarChart(
                    existingStat.copy(
                        featureId = request.config.featureId,
                        endDate = request.config.endDate,
                        sampleSize = request.config.sampleSize,
                        yRangeType = request.config.yRangeType,
                        yTo = request.config.yTo,
                        scale = request.config.scale,
                        barPeriod = request.config.barPeriod,
                        sumByCount = request.config.sumByCount
                    )
                )
            }
        }
    }

    override suspend fun updateLuaGraph(request: LuaGraphUpdateRequest) = withContext(io) {
        transactionHelper.withTransaction {
            val existing = graphDao.getGraphStatById(request.graphStatId)
            val existingLuaGraph = graphDao.getLuaGraphByGraphStatId(request.graphStatId)
                ?: throw IllegalArgumentException("Lua graph not found for graphStatId: ${request.graphStatId}")

            if (request.name != null) {
                graphDao.updateGraphOrStat(existing.copy(name = request.name))
            }

            if (request.config != null) {
                graphDao.updateLuaGraph(
                    existingLuaGraph.toLuaGraph().copy(
                        script = request.config.script
                    )
                )
                graphDao.deleteFeaturesForLuaGraph(existingLuaGraph.id)
                val features = request.config.features.map { config ->
                    com.samco.trackandgraph.data.database.entity.LuaGraphFeature(
                        id = 0L,
                        luaGraphId = existingLuaGraph.id,
                        featureId = config.featureId,
                        name = config.name
                    )
                }
                graphDao.insertLuaGraphFeatures(features)
            }
        }
    }

    // =========================================================================
    // Delete method
    // =========================================================================

    override suspend fun deleteGraph(request: GraphDeleteRequest) = withContext(io) {
        graphDao.deleteGraphOrStat(request.graphStatId)
    }

    // =========================================================================
    // Duplicate methods
    // =========================================================================

    override suspend fun duplicateLineGraph(graphStatId: Long, groupId: Long): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val graphOrStat = graphDao.tryGetGraphStatById(graphStatId)?.toDto()
                ?: return@withTransaction null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat, groupId)
            graphDao.getLineGraphByGraphStatId(graphStatId)?.let {
                val copy = graphDao.insertLineGraph(
                    it.toLineGraph().copy(id = 0L, graphStatId = newGraphStatId)
                )
                graphDao.insertLineGraphFeatures(it.features.map { f ->
                    f.copy(id = 0L, lineGraphId = copy)
                })
            }
            newGraphStatId
        }
    }

    override suspend fun duplicatePieChart(graphStatId: Long, groupId: Long): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val graphOrStat = graphDao.tryGetGraphStatById(graphStatId)?.toDto()
                ?: return@withTransaction null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat, groupId)
            graphDao.getPieChartByGraphStatId(graphStatId)?.let {
                graphDao.insertPieChart(it.copy(id = 0L, graphStatId = newGraphStatId))
            }
            newGraphStatId
        }
    }

    override suspend fun duplicateAverageTimeBetweenStat(graphStatId: Long, groupId: Long): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val graphOrStat = graphDao.tryGetGraphStatById(graphStatId)?.toDto()
                ?: return@withTransaction null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat, groupId)
            graphDao.getAverageTimeBetweenStatByGraphStatId(graphStatId)?.let {
                graphDao.insertAverageTimeBetweenStat(it.copy(id = 0L, graphStatId = newGraphStatId))
            }
            newGraphStatId
        }
    }

    override suspend fun duplicateTimeHistogram(graphStatId: Long, groupId: Long): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val graphOrStat = graphDao.tryGetGraphStatById(graphStatId)?.toDto()
                ?: return@withTransaction null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat, groupId)
            graphDao.getTimeHistogramByGraphStatId(graphStatId)?.let {
                graphDao.insertTimeHistogram(it.copy(id = 0L, graphStatId = newGraphStatId))
            }
            newGraphStatId
        }
    }

    override suspend fun duplicateLastValueStat(graphStatId: Long, groupId: Long): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val graphOrStat = graphDao.tryGetGraphStatById(graphStatId)?.toDto()
                ?: return@withTransaction null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat, groupId)
            graphDao.getLastValueStatByGraphStatId(graphStatId)?.let {
                graphDao.insertLastValueStat(it.copy(id = 0L, graphStatId = newGraphStatId))
            }
            newGraphStatId
        }
    }

    override suspend fun duplicateBarChart(graphStatId: Long, groupId: Long): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val graphOrStat = graphDao.tryGetGraphStatById(graphStatId)?.toDto()
                ?: return@withTransaction null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat, groupId)
            graphDao.getBarChartByGraphStatId(graphStatId)?.let {
                graphDao.insertBarChart(it.copy(id = 0L, graphStatId = newGraphStatId))
            }
            newGraphStatId
        }
    }

    override suspend fun duplicateLuaGraph(graphStatId: Long, groupId: Long): Long? = withContext(io) {
        transactionHelper.withTransaction {
            val graphOrStat = graphDao.tryGetGraphStatById(graphStatId)?.toDto()
                ?: return@withTransaction null
            val newGraphStatId = duplicateGraphOrStat(graphOrStat, groupId)
            graphDao.getLuaGraphByGraphStatId(graphStatId)?.let {
                val copy = graphDao.insertLuaGraph(
                    it.toLuaGraph().copy(id = 0L, graphStatId = newGraphStatId)
                )
                graphDao.insertLuaGraphFeatures(it.features.map { f ->
                    f.copy(id = 0L, luaGraphId = copy)
                })
            }
            newGraphStatId
        }
    }

    // =========================================================================
    // Get methods
    // =========================================================================

    override suspend fun getGraphStatById(graphStatId: Long): GraphOrStat = withContext(io) {
        graphDao.getGraphStatById(graphStatId).toDto()
    }

    override suspend fun tryGetGraphStatById(graphStatId: Long): GraphOrStat? = withContext(io) {
        graphDao.tryGetGraphStatById(graphStatId)?.toDto()
    }

    override suspend fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures? =
        withContext(io) {
            graphDao.getLineGraphByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getPieChartByGraphStatId(graphStatId: Long): PieChart? = withContext(io) {
        graphDao.getPieChartByGraphStatId(graphStatId)?.toDto()
    }

    override suspend fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat? =
        withContext(io) {
            graphDao.getAverageTimeBetweenStatByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram? =
        withContext(io) {
            graphDao.getTimeHistogramByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getLastValueStatByGraphStatId(graphStatId: Long): LastValueStat? =
        withContext(io) {
            graphDao.getLastValueStatByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getBarChartByGraphStatId(graphStatId: Long): BarChart? = withContext(io) {
        graphDao.getBarChartByGraphStatId(graphStatId)?.toDto()
    }

    override suspend fun getLuaGraphByGraphStatId(graphStatId: Long): LuaGraphWithFeatures? =
        withContext(io) {
            graphDao.getLuaGraphByGraphStatId(graphStatId)?.toDto()
        }

    override suspend fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat> =
        withContext(io) {
            graphDao.getGraphsAndStatsByGroupIdSync(groupId).map { it.toDto() }
        }

    override suspend fun getAllGraphStatsSync(): List<GraphOrStat> = withContext(io) {
        graphDao.getAllGraphStatsSync().map { it.toDto() }
    }

    override suspend fun hasAnyGraphs(): Boolean = withContext(io) { graphDao.hasAnyGraphs() }

    override suspend fun hasAnyLuaGraphs(): Boolean = withContext(io) { graphDao.hasAnyLuaGraphs() }

    // =========================================================================
    // Private helper methods
    // =========================================================================

    private fun insertGraphOrStat(name: String, groupId: Long, type: GraphStatType): Long =
        graphDao.insertGraphOrStat(
            com.samco.trackandgraph.data.database.entity.GraphOrStat(
                id = 0L,
                groupId = groupId,
                name = name,
                type = type,
                displayIndex = 0
            )
        )

    private fun duplicateGraphOrStat(graphOrStat: GraphOrStat, groupId: Long) =
        graphDao.insertGraphOrStat(graphOrStat.copy(id = 0L).toEntity(groupId))
}
