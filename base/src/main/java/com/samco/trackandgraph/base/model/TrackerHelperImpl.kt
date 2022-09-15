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

import androidx.room.withTransaction
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.*
import com.samco.trackandgraph.base.model.TrackerHelper.DurationNumericConversionMode
import com.samco.trackandgraph.base.model.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject
import com.samco.trackandgraph.base.database.entity.DataPoint as DataPointEntity

internal class TrackerHelperImpl @Inject constructor(
    private val database: TrackAndGraphDatabase,
    private val dao: TrackAndGraphDatabaseDao,
    @IODispatcher private val io: CoroutineDispatcher
) : TrackerHelper {
    override suspend fun updateTracker(
        oldTracker: Tracker,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>,
        durationNumericConversionMode: DurationNumericConversionMode?,
        newName: String?,
        newType: DataType?,
        newDiscreteValues: List<DiscreteValue>?,
        hasDefaultValue: Boolean?,
        defaultValue: Double?,
        featureDescription: String?
    ) = withContext(io) {
        database.withTransaction {
            updateFeatureImpl(
                oldTracker,
                discreteValueMap,
                durationNumericConversionMode,
                newName,
                newType,
                newDiscreteValues,
                hasDefaultValue,
                defaultValue,
                featureDescription
            )
        }
    }

    private fun updateFeatureImpl(
        oldTracker: Tracker,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>,
        durationNumericConversionMode: DurationNumericConversionMode?,
        newName: String?,
        newDataType: DataType?,
        newDiscreteValues: List<DiscreteValue>?,
        hasDefaultValue: Boolean?,
        defaultValue: Double?,
        featureDescription: String?
    ) {
        val newType = newDataType ?: oldTracker.dataType

        updateAllExistingDataPointsForTransformation(
            oldTracker,
            newType,
            discreteValueMap,
            durationNumericConversionMode
        )

        val newFeature = com.samco.trackandgraph.base.database.entity.Feature(
            id = oldTracker.featureId,
            name = newName ?: oldTracker.name,
            groupId = oldTracker.groupId,
            displayIndex = oldTracker.displayIndex,
            description = featureDescription ?: oldTracker.description
        )
        val newTracker = com.samco.trackandgraph.base.database.entity.Tracker(
            id = oldTracker.id,
            featureId = oldTracker.featureId,
            dataType = newType,
            discreteValues = newDiscreteValues ?: oldTracker.discreteValues,
            hasDefaultValue = hasDefaultValue ?: oldTracker.hasDefaultValue,
            defaultValue = defaultValue ?: oldTracker.defaultValue
        )
        dao.updateFeature(newFeature)
        dao.updateTracker(newTracker)
    }

    private fun updateAllExistingDataPointsForTransformation(
        oldTracker: Tracker,
        newType: DataType,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>,
        durationNumericConversionMode: DurationNumericConversionMode?
    ) {
        when (oldTracker.dataType) {
            DataType.DISCRETE -> when (newType) {
                DataType.CONTINUOUS -> stripDataPointsToValue(oldTracker)
                DataType.DISCRETE -> updateDiscreteValueDataPoints(oldTracker, discreteValueMap)
                else -> {}
            }
            DataType.CONTINUOUS -> when (newType) {
                DataType.DURATION -> {
                    if (durationNumericConversionMode == null) noConversionModeError()
                    updateContinuousDataPointsToDurations(
                        oldTracker,
                        durationNumericConversionMode
                    )
                }
                else -> {}
            }
            DataType.DURATION -> when (newType) {
                DataType.CONTINUOUS -> {
                    if (durationNumericConversionMode == null) noConversionModeError()
                    updateDurationDataPointsToContinuous(
                        oldTracker,
                        durationNumericConversionMode
                    )
                }
                else -> {}
            }
        }
    }

    private fun noConversionModeError(): Nothing =
        throw Exception("You must provide durationNumericConversionMode if you convert from CONTINUOUS TO DURATION")

    private fun updateDurationDataPointsToContinuous(
        oldTracker: Tracker,
        durationNumericConversionMode: DurationNumericConversionMode
    ) {
        val oldDataPoints = getAllDataPoints(oldTracker.featureId)
        val divisor = when (durationNumericConversionMode) {
            DurationNumericConversionMode.HOURS -> 3600.0
            DurationNumericConversionMode.MINUTES -> 60.0
            DurationNumericConversionMode.SECONDS -> 1.0
        }
        val newDataPoints = oldDataPoints.map {
            val newValue = it.value / divisor
            DataPointEntity(
                it.timestamp,
                it.featureId,
                newValue,
                "",
                it.note
            )
        }
        dao.updateDataPoints(newDataPoints)
    }

    //TODO this would probably be faster in SQL
    private fun updateContinuousDataPointsToDurations(
        oldTracker: Tracker,
        durationNumericConversionMode: DurationNumericConversionMode
    ) {
        val oldDataPoints = getAllDataPoints(oldTracker.featureId)
        val multiplier = when (durationNumericConversionMode) {
            DurationNumericConversionMode.HOURS -> 3600.0
            DurationNumericConversionMode.MINUTES -> 60.0
            DurationNumericConversionMode.SECONDS -> 1.0
        }
        val newDataPoints = oldDataPoints.map {
            val newValue = it.value * multiplier
            DataPointEntity(
                it.timestamp,
                it.featureId,
                newValue,
                "",
                it.note
            )
        }
        dao.updateDataPoints(newDataPoints)
    }

    //TODO this would probably be faster in SQL
    private fun stripDataPointsToValue(oldTracker: Tracker) {
        val oldDataPoints = getAllDataPoints(oldTracker.featureId)
        val newDataPoints = oldDataPoints.map {
            DataPointEntity(
                it.timestamp,
                it.featureId,
                it.value,
                "",
                it.note
            )
        }
        dao.updateDataPoints(newDataPoints)
    }

    private fun getAllDataPoints(featureId: Long) = dao.getDataPointsForFeatureSync(featureId)

    private fun updateDiscreteValueDataPoints(
        oldTracker: Tracker,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>
    ) {
        //Remove any discrete values that have no mapping
        oldTracker.discreteValues
            .filter { dv -> !discreteValueMap.keys.contains(dv) }
            .forEach { i ->
                removeExistingDataPointsForDiscreteValue(
                    oldTracker.featureId,
                    i.index
                )
            }

        //Update existing data points to their new discrete values
        if (discreteValueMap.any { it.key != it.value }) {
            updateExistingDataPointsForDiscreteValue(oldTracker.featureId, discreteValueMap)
        }
    }

    //TODO this would probably be faster in SQL
    private fun updateExistingDataPointsForDiscreteValue(
        featureId: Long,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>
    ) {
        val oldValues = getAllDataPoints(featureId)
        val newValues = oldValues.map { v ->
            val newDisc = discreteValueMap[DiscreteValue(v.value.toInt(), v.label)]!!
            DataPointEntity(
                v.timestamp,
                v.featureId,
                newDisc.index.toDouble(),
                newDisc.label,
                v.note
            )
        }
        dao.updateDataPoints(newValues)
    }

    private fun removeExistingDataPointsForDiscreteValue(featureId: Long, index: Int) {
        dao.deleteAllDataPointsForDiscreteValue(featureId, index.toDouble())
    }

    override suspend fun getTrackersByIdsSync(trackerIds: List<Long>): List<Tracker> =
        withContext(io) { trackerIds.mapNotNull { getTrackerById(it) } }

    override suspend fun getTrackerById(trackerId: Long): Tracker? = withContext(io) {
        dao.getTrackerById(trackerId)?.let {
            val feature = dao.getFeatureById(it.featureId) ?: return@let null
            Tracker.fromEntities(it, feature)
        }
    }

    override suspend fun playTimerForTracker(trackerId: Long): Long? {
        return dao.getTrackerById(trackerId)?.featureId?.also { featureId ->
            dao.deleteFeatureTimer(featureId)
            val timer = com.samco.trackandgraph.base.database.entity.FeatureTimer(
                0L,
                featureId,
                Instant.now()
            )
            dao.insertFeatureTimer(timer)
        }
    }

    override suspend fun stopTimerForTracker(trackerId: Long): Duration? =
        dao.getTrackerById(trackerId)?.featureId?.let { featureId ->
            val timer = dao.getFeatureTimer(featureId)
            dao.deleteFeatureTimer(featureId)
            timer?.let { Duration.between(it.startInstant, Instant.now()) }
        }

    override suspend fun getAllActiveTimerTrackers(): List<DisplayTracker> = withContext(io) {
        dao.getAllActiveTimerTrackers().map { it.toDto() }
    }

    override suspend fun getTrackersForGroupSync(groupId: Long): List<Tracker> {
        return dao.getFeaturesForGroupSync(groupId)
            .mapNotNull { feature ->
                dao.getTrackerByFeatureId(feature.id)?.let { tracker ->
                    Pair(tracker, feature)
                }
            }
            .map { Tracker.fromEntities(it.first, it.second) }
    }

    override suspend fun getTrackerByFeatureId(featureId: Long): Tracker? {
        return dao.getTrackerByFeatureId(featureId)?.let { tracker ->
            dao.getFeatureById(tracker.featureId)?.let { feature ->
                Tracker.fromEntities(tracker, feature)
            }
        }
    }

    override fun hasAtLeastOneTracker(): Flow<Boolean> {
        return dao.numTrackers().map { it > 0 }
    }

    override suspend fun insertTracker(tracker: Tracker): Long = withContext(io) {
        return@withContext database.withTransaction {
            val featureId = dao.insertFeature(tracker.toFeatureEntity())
            dao.insertTracker(tracker.toEntity().copy(featureId = featureId))
        }
    }

    override suspend fun updateTracker(tracker: Tracker) = withContext(io) {
        database.withTransaction {
            dao.updateFeature(tracker.toFeatureEntity())
            dao.updateTracker(tracker.toEntity())
        }
    }

    override suspend fun getAllTrackersSync(): List<Tracker> = withContext(io) {
        dao.getAllTrackersSync().map {
            val feature = dao.getFeatureById(it.featureId) ?: return@map null
            Tracker.fromEntities(it, feature)
        }.filterNotNull()
    }

    override suspend fun getDisplayTrackersForGroupSync(groupId: Long): List<DisplayTracker> =
        withContext(io) {
            dao.getDisplayTrackersForGroupSync(groupId).map { it.toDto() }
        }

    override suspend fun tryGetDisplayTrackerByIdSync(trackerId: Long): DisplayTracker? =
        withContext(io) {
            dao.getDisplayTrackerByIdSync(trackerId)?.toDto()
        }

    override suspend fun getDataPointByTimestampAndTrackerSync(
        trackerId: Long,
        timestamp: OffsetDateTime
    ): DataPoint? = withContext(io) {
        return@withContext dao.getTrackerById(trackerId)?.featureId?.let {
            dao.getDataPointByTimestampAndFeatureSync(it, timestamp).toDto()
        }
    }

}