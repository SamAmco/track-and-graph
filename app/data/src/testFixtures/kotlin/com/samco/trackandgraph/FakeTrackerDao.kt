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

import com.samco.trackandgraph.data.database.TrackerDao
import com.samco.trackandgraph.data.database.entity.DataPoint
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.FeatureTimer
import com.samco.trackandgraph.data.database.entity.Tracker
import com.samco.trackandgraph.data.database.entity.queryresponse.DisplayTracker
import com.samco.trackandgraph.data.database.entity.queryresponse.TrackerWithFeature

/**
 * A fake in-memory implementation of [TrackerDao] for testing purposes.
 * This allows tests to run without a real database and without mocking frameworks.
 */
internal class FakeTrackerDao : TrackerDao {

    private var nextFeatureId = 1L
    private var nextTrackerId = 1L
    private val features = mutableMapOf<Long, Feature>()
    private val trackers = mutableMapOf<Long, Tracker>()
    private val dataPoints = mutableMapOf<Long, DataPoint>()
    private val timers = mutableMapOf<Long, FeatureTimer>()

    // =========================================================================
    // Test helper methods
    // =========================================================================

    fun clear() {
        features.clear()
        trackers.clear()
        dataPoints.clear()
        timers.clear()
        nextFeatureId = 1L
        nextTrackerId = 1L
    }

    // =========================================================================
    // Feature operations
    // =========================================================================

    override fun insertFeature(feature: Feature): Long {
        val id = if (feature.id == 0L) nextFeatureId++ else feature.id
        features[id] = feature.copy(id = id)
        return id
    }

    override fun updateFeature(feature: Feature) {
        features[feature.id] = feature
    }

    override fun deleteFeature(id: Long) {
        features.remove(id)
        trackers.entries.removeIf { it.value.featureId == id }
        dataPoints.entries.removeIf { it.value.featureId == id }
        timers.remove(id)
    }

    // =========================================================================
    // Tracker CRUD operations
    // =========================================================================

    override fun insertTracker(tracker: Tracker): Long {
        val id = if (tracker.id == 0L) nextTrackerId++ else tracker.id
        trackers[id] = tracker.copy(id = id)
        return id
    }

    override fun updateTracker(tracker: Tracker) {
        trackers[tracker.id] = tracker
    }

    override fun getTrackerById(trackerId: Long): TrackerWithFeature? {
        val tracker = trackers[trackerId] ?: return null
        val feature = features[tracker.featureId] ?: return null
        return TrackerWithFeature(
            id = tracker.id,
            featureId = tracker.featureId,
            name = feature.name,
            description = feature.description,
            dataType = tracker.dataType,
            hasDefaultValue = tracker.hasDefaultValue,
            defaultValue = tracker.defaultValue,
            defaultLabel = tracker.defaultLabel,
            suggestionType = tracker.suggestionType,
            suggestionOrder = tracker.suggestionOrder
        )
    }

    override fun getTrackerByFeatureId(featureId: Long): TrackerWithFeature? {
        val tracker = trackers.values.firstOrNull { it.featureId == featureId } ?: return null
        val feature = features[featureId] ?: return null
        return TrackerWithFeature(
            id = tracker.id,
            featureId = tracker.featureId,
            name = feature.name,
            description = feature.description,
            dataType = tracker.dataType,
            hasDefaultValue = tracker.hasDefaultValue,
            defaultValue = tracker.defaultValue,
            defaultLabel = tracker.defaultLabel,
            suggestionType = tracker.suggestionType,
            suggestionOrder = tracker.suggestionOrder
        )
    }

    override fun getAllTrackersSync(): List<TrackerWithFeature> {
        return trackers.values.mapNotNull { getTrackerById(it.id) }
    }

    override fun getTrackersForGroupSync(groupId: Long): List<TrackerWithFeature> {
        return getAllTrackersSync()
    }

    override fun numTrackers(): Int = trackers.size

    // =========================================================================
    // Display tracker operations
    // =========================================================================

    override fun getAllActiveTimerTrackers(): List<DisplayTracker> = emptyList()

    override fun getDisplayTrackerByTrackerIdsSync(ids: Set<Long>): List<DisplayTracker> =
        emptyList()

    override fun getDisplayTrackerByFeatureIdSync(featureId: Long): DisplayTracker? = null

    // =========================================================================
    // Timer operations
    // =========================================================================

    override fun insertFeatureTimer(featureTimer: FeatureTimer) {
        timers[featureTimer.featureId] = featureTimer
    }

    override fun deleteFeatureTimer(featureId: Long) {
        timers.remove(featureId)
    }

    override fun getFeatureTimer(featureId: Long): FeatureTimer? = timers[featureId]

    // =========================================================================
    // Data point operations
    // =========================================================================

    override fun hasAtLeastOneDataPoint(): Boolean = dataPoints.isNotEmpty()

    override fun getDataPointsForFeatureSync(featureId: Long): List<DataPoint> {
        return dataPoints.values.filter { it.featureId == featureId }
    }

    override fun getDataPointByTimestampAndFeatureSync(
        featureId: Long,
        epochMilli: Long
    ): DataPoint? {
        return dataPoints.values.firstOrNull {
            it.featureId == featureId && it.epochMilli == epochMilli
        }
    }

    override fun getDataPointCount(featureId: Long): Int {
        return dataPoints.values.count { it.featureId == featureId }
    }

    override fun getDataPoints(featureId: Long, limit: Int, offset: Int): List<DataPoint> {
        return dataPoints.values
            .filter { it.featureId == featureId }
            .drop(offset)
            .take(limit)
    }

    override fun updateDataPoints(dataPoint: List<DataPoint>) {
        dataPoint.forEach { dataPoints[it.epochMilli] = it }
    }
}
