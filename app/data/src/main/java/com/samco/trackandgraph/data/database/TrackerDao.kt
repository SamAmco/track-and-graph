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

import com.samco.trackandgraph.data.database.entity.DataPoint
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.FeatureTimer
import com.samco.trackandgraph.data.database.entity.Tracker
import com.samco.trackandgraph.data.database.entity.queryresponse.DisplayTracker
import com.samco.trackandgraph.data.database.entity.queryresponse.TrackerWithFeature

/**
 * Data access interface for tracker-related operations.
 * This interface abstracts the database operations needed by TrackerHelper,
 * allowing for different implementations (Room, fake for testing, etc.)
 */
internal interface TrackerDao {

    // =========================================================================
    // Feature operations
    // =========================================================================

    fun insertFeature(feature: Feature): Long

    fun updateFeature(feature: Feature)

    fun deleteFeature(id: Long)

    // =========================================================================
    // Tracker CRUD operations
    // =========================================================================

    fun insertTracker(tracker: Tracker): Long

    fun updateTracker(tracker: Tracker)

    fun getTrackerById(trackerId: Long): TrackerWithFeature?

    fun getTrackerByFeatureId(featureId: Long): TrackerWithFeature?

    fun getAllTrackersSync(): List<TrackerWithFeature>

    fun getTrackersForGroupSync(groupId: Long): List<TrackerWithFeature>

    fun numTrackers(): Int

    // =========================================================================
    // Display tracker operations
    // =========================================================================

    fun getAllActiveTimerTrackers(): List<DisplayTracker>

    fun getDisplayTrackerByTrackerIdsSync(ids: Set<Long>): List<DisplayTracker>

    fun getDisplayTrackerByFeatureIdSync(featureId: Long): DisplayTracker?

    // =========================================================================
    // Timer operations
    // =========================================================================

    fun insertFeatureTimer(featureTimer: FeatureTimer)

    fun deleteFeatureTimer(featureId: Long)

    fun getFeatureTimer(featureId: Long): FeatureTimer?

    // =========================================================================
    // Data point operations
    // =========================================================================

    fun hasAtLeastOneDataPoint(): Boolean

    fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint>

    fun getDataPointByTimestampAndFeatureSync(featureId: Long, epochMilli: Long): DataPoint?

    fun getDataPointCount(featureId: Long): Int

    fun getDataPoints(featureId: Long, limit: Int, offset: Int): List<DataPoint>

    fun updateDataPoints(dataPoint: List<DataPoint>)

    fun insertDataPoints(dataPoints: List<DataPoint>)

    fun deleteDataPoints(dataPoints: List<DataPoint>)
}
