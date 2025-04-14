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

package com.samco.trackandgraph.base.model

import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.database.sampling.DataSampler
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.threeten.bp.OffsetDateTime
import java.io.InputStream
import java.io.OutputStream

interface DataInteractor : TrackerHelper, DataSampler {
    suspend fun insertGroup(group: Group): Long

    suspend fun deleteGroup(id: Long)

    suspend fun updateGroup(group: Group)

    suspend fun getAllRemindersSync(): List<Reminder>

    suspend fun getAllGroupsSync(): List<Group>

    suspend fun updateReminders(reminders: List<Reminder>)

    suspend fun getGroupById(id: Long): Group

    suspend fun updateGroupChildOrder(groupId: Long, children: List<GroupChild>)

    suspend fun getFeaturesForGroupSync(groupId: Long): List<Feature>

    suspend fun getFeatureById(featureId: Long): Feature?

    suspend fun deleteDataPoint(dataPoint: DataPoint)

    suspend fun deleteGraphOrStat(id: Long)

    suspend fun deleteGraphOrStat(graphOrStat: GraphOrStat)

    suspend fun deleteFeature(featureId: Long)

    suspend fun insertDataPoint(dataPoint: DataPoint): Long

    suspend fun insertDataPoints(dataPoints: List<DataPoint>)

    /**
     * Emits an event every time currently displayed data may have changed.
     *
     * @see [DataUpdateType]
     */
    fun getDataUpdateEvents(): SharedFlow<DataUpdateType>

    suspend fun getGraphStatById(graphStatId: Long): GraphOrStat

    suspend fun tryGetGraphStatById(graphStatId: Long): GraphOrStat?

    suspend fun getLineGraphByGraphStatId(graphStatId: Long): LineGraphWithFeatures?

    suspend fun getPieChartByGraphStatId(graphStatId: Long): PieChart?

    suspend fun getAverageTimeBetweenStatByGraphStatId(graphStatId: Long): AverageTimeBetweenStat?

    suspend fun getTimeHistogramByGraphStatId(graphStatId: Long): TimeHistogram?

    suspend fun getLastValueStatByGraphStatId(graphOrStatId: Long): LastValueStat?

    suspend fun getBarChartByGraphStatId(graphStatId: Long): BarChart?

    suspend fun getGraphsAndStatsByGroupIdSync(groupId: Long): List<GraphOrStat>

    suspend fun getAllGraphStatsSync(): List<GraphOrStat>

    fun getAllDisplayNotes(): Flow<List<DisplayNote>>

    suspend fun removeNote(timestamp: OffsetDateTime, trackerId: Long)

    suspend fun deleteGlobalNote(note: GlobalNote)

    suspend fun insertGlobalNote(note: GlobalNote): Long

    suspend fun getGlobalNoteByTimeSync(timestamp: OffsetDateTime?): GlobalNote?

    suspend fun getAllGlobalNotesSync(): List<GlobalNote>

    suspend fun duplicateLineGraph(graphOrStat: GraphOrStat): Long?

    suspend fun duplicatePieChart(graphOrStat: GraphOrStat): Long?

    suspend fun duplicateAverageTimeBetweenStat(graphOrStat: GraphOrStat): Long?

    suspend fun duplicateTimeHistogram(graphOrStat: GraphOrStat): Long?

    suspend fun duplicateLastValueStat(graphOrStat: GraphOrStat): Long?

    suspend fun duplicateBarChart(graphOrStat: GraphOrStat): Long?

    suspend fun insertLineGraph(graphOrStat: GraphOrStat, lineGraph: LineGraphWithFeatures): Long

    suspend fun insertPieChart(graphOrStat: GraphOrStat, pieChart: PieChart): Long

    suspend fun insertAverageTimeBetweenStat(
        graphOrStat: GraphOrStat,
        averageTimeBetweenStat: AverageTimeBetweenStat
    ): Long

    suspend fun insertTimeHistogram(graphOrStat: GraphOrStat, timeHistogram: TimeHistogram): Long

    suspend fun insertLastValueStat(graphOrStat: GraphOrStat, config: LastValueStat): Long

    suspend fun insertBarChart(graphOrStat: GraphOrStat, barChart: BarChart): Long

    suspend fun updatePieChart(graphOrStat: GraphOrStat, pieChart: PieChart)

    suspend fun updateAverageTimeBetweenStat(
        graphOrStat: GraphOrStat,
        averageTimeBetweenStat: AverageTimeBetweenStat
    )

    suspend fun updateLineGraph(graphOrStat: GraphOrStat, lineGraph: LineGraphWithFeatures)

    suspend fun updateLastValueStat(graphOrStat: GraphOrStat, config: LastValueStat)

    suspend fun updateBarChart(graphOrStat: GraphOrStat, barChart: BarChart)

    suspend fun updateGraphOrStat(graphOrStat: GraphOrStat)

    suspend fun updateTimeHistogram(graphOrStat: GraphOrStat, timeHistogram: TimeHistogram)

    suspend fun getGroupsForGroupSync(id: Long): List<Group>

    suspend fun writeFeaturesToCSV(outStream: OutputStream, featureIds: List<Long>)

    suspend fun readFeaturesFromCSV(inputStream: InputStream, trackGroupId: Long)

    suspend fun getAllFeaturesSync(): List<Feature>

    suspend fun getLuaGraphByGraphStatId(graphStatId: Long): LuaGraphWithFeatures?

    suspend fun duplicateLuaGraph(graphOrStat: GraphOrStat): Long?

    suspend fun insertLuaGraph(graphOrStat: GraphOrStat, luaGraph: LuaGraphWithFeatures): Long

    suspend fun updateLuaGraph(graphOrStat: GraphOrStat, luaGraph: LuaGraphWithFeatures)

    suspend fun hasAnyLuaGraphs(): Boolean

    suspend fun hasAnyGraphs(): Boolean

    suspend fun hasAnyFeatures(): Boolean

    suspend fun hasAnyGroups(): Boolean

    suspend fun hasAnyReminders(): Boolean
}