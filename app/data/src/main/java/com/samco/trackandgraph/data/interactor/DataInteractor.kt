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

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DisplayNote
import com.samco.trackandgraph.data.database.dto.Feature
import com.samco.trackandgraph.data.database.dto.GlobalNote
import com.samco.trackandgraph.data.database.dto.GroupChildOrderData
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.MoveComponentRequest
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.SharedFlow
import org.threeten.bp.OffsetDateTime

interface DataInteractor : TrackerHelper, FunctionHelper, ReminderHelper, GroupHelper, GraphHelper {
    suspend fun getGroupGraphSync(rootGroupId: Long? = null): GroupGraph

    suspend fun updateGroupChildOrder(groupId: Long, children: List<GroupChildOrderData>)

    suspend fun getFeaturesForGroupSync(groupId: Long): List<Feature>

    suspend fun getFeatureById(featureId: Long): Feature?

    suspend fun deleteDataPoint(dataPoint: DataPoint)

    suspend fun insertDataPoint(dataPoint: DataPoint): Long

    suspend fun insertDataPoints(dataPoints: List<DataPoint>)

    /**
     * Emits an event every time currently displayed data may have changed.
     *
     * @see [DataUpdateType]
     */
    fun getDataUpdateEvents(): SharedFlow<DataUpdateType>

    fun getAllDisplayNotes(): Flow<List<DisplayNote>>

    suspend fun removeNote(timestamp: OffsetDateTime, trackerId: Long)

    suspend fun deleteGlobalNote(note: GlobalNote)

    suspend fun insertGlobalNote(note: GlobalNote): Long

    suspend fun getGlobalNoteByTimeSync(timestamp: OffsetDateTime?): GlobalNote?

    suspend fun getAllGlobalNotesSync(): List<GlobalNote>

    fun onImportedExternalData()

    suspend fun getAllFeaturesSync(): List<Feature>

    suspend fun hasAnyFeatures(): Boolean

    /**
     * Gets all feature IDs that depend on a given feature, either directly or transitively.
     * This includes the feature itself and is used for cycle detection to prevent circular dependencies.
     */
    suspend fun getFeatureIdsDependingOn(featureId: Long): Set<Long>

    /**
     * Gets all feature IDs that a given feature depends on, either directly or transitively.
     * This includes the feature itself. Used for determining which features could affect
     * the output of a given feature.
     */
    suspend fun getDependencyFeatureIdsOf(featureId: Long): Set<Long>

    /**
     * Moves a feature from one group to another.
     *
     * @param request The move request containing the feature type, ID, and source/destination groups.
     */
    suspend fun moveComponent(request: MoveComponentRequest)
}
