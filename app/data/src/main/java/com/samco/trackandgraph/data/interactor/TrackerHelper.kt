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
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.dto.TrackerCreateRequest
import com.samco.trackandgraph.data.database.dto.TrackerDeleteRequest
import com.samco.trackandgraph.data.database.dto.TrackerUpdateRequest
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

/**
 * An interface for updating features. Do not use this interface directly, it is implemented by
 * the DataInteractor interface.
 *
 * The implementation of FeatureUpdater will update the existing data that has been tracked for
 * a feature as well as the feature its self. It will perform all changes inside a transaction and
 * throw an exception if anything goes wrong.
 */
interface TrackerHelper {

    enum class DurationNumericConversionMode { HOURS, MINUTES, SECONDS }

    suspend fun updateDataPoints(
        trackerId: Long,
        whereValue: Double? = null,
        whereLabel: String? = null,
        toValue: Double? = null,
        toLabel: String? = null
    )

    /**
     * Creates a new tracker and returns the tracker ID.
     */
    suspend fun createTracker(request: TrackerCreateRequest): Long

    /**
     * Updates an existing tracker. Only non-null fields in the request will be changed.
     *
     * If [TrackerUpdateRequest.dataType] is being changed to/from DURATION, the
     * [TrackerUpdateRequest.durationNumericConversionMode] should be specified to indicate
     * how to convert existing data points.
     */
    suspend fun updateTracker(request: TrackerUpdateRequest)

    /**
     * Deletes a tracker from a specific group.
     */
    suspend fun deleteTracker(request: TrackerDeleteRequest)

    suspend fun getTrackersByIdsSync(trackerIds: List<Long>): List<Tracker>

    suspend fun getTrackerById(trackerId: Long): Tracker?

    suspend fun getAllTrackersSync(): List<Tracker>

    suspend fun getDisplayTrackersForGroupSync(groupId: Long): List<DisplayTracker>

    suspend fun tryGetTrackerByFeatureId(featureId: Long): DisplayTracker?

    suspend fun getDataPointByTimestampAndTrackerSync(
        trackerId: Long,
        timestamp: OffsetDateTime
    ): DataPoint?

    /**
     * Returns the feature id of the started timer tracker
     */
    suspend fun playTimerForTracker(trackerId: Long): Long?

    suspend fun stopTimerForTracker(trackerId: Long): Duration?

    suspend fun getAllActiveTimerTrackers(): List<DisplayTracker>

    suspend fun getTrackersForGroupSync(groupId: Long): List<Tracker>

    suspend fun getTrackerByFeatureId(featureId: Long): Tracker?

    suspend fun hasAtLeastOneTracker(): Boolean

    suspend fun hasAtLeastOneDataPoint(): Boolean
}