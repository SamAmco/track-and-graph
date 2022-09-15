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
import kotlinx.coroutines.flow.Flow
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

    suspend fun updateTracker(
        oldTracker: Tracker,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>,
        durationNumericConversionMode: DurationNumericConversionMode? = null,
        newName: String? = null,
        newType: DataType? = null,
        newDiscreteValues: List<DiscreteValue>? = null,
        hasDefaultValue: Boolean? = null,
        defaultValue: Double? = null,
        featureDescription: String? = null
    )

    suspend fun getTrackersByIdsSync(trackerIds: List<Long>): List<Tracker>

    suspend fun getTrackerById(trackerId: Long): Tracker?

    suspend fun insertTracker(tracker: Tracker): Long

    suspend fun updateTracker(tracker: Tracker)

    suspend fun getAllTrackersSync(): List<Tracker>

    suspend fun getDisplayTrackersForGroupSync(groupId: Long): List<DisplayTracker>

    suspend fun tryGetDisplayTrackerByIdSync(trackerId: Long): DisplayTracker?

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

    fun hasAtLeastOneTracker(): Flow<Boolean>
}