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
import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.*
import com.samco.trackandgraph.data.interactor.TrackerHelper.DurationNumericConversionMode
import com.samco.trackandgraph.data.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import timber.log.Timber
import javax.inject.Inject
import com.samco.trackandgraph.data.database.entity.DataPoint as DataPointEntity

internal class TrackerHelperImpl @Inject constructor(
    private val transactionHelper: DatabaseTransactionHelper,
    private val dao: TrackAndGraphDatabaseDao,
    private val dataPointUpdateHelper: DataPointUpdateHelper,
    @IODispatcher private val io: CoroutineDispatcher
) : TrackerHelper {

    override suspend fun updateDataPoints(
        trackerId: Long,
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?
    ) {
        withContext(io) {
            transactionHelper.withTransaction {
                runDataPointUpdate(
                    trackerId = trackerId,
                    whereValue = whereValue,
                    whereLabel = whereLabel,
                    toValue = toValue,
                    toLabel = toLabel
                ).let {
                    if (it.isFailure) Timber.e(it.exceptionOrNull())
                }
            }
        }
    }

    private fun runDataPointUpdate(
        trackerId: Long,
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?
    ): Result<Unit> {
        val tracker by lazy { dao.getTrackerById(trackerId) }
        return runCatching {
            dataPointUpdateHelper.performUpdate(
                whereValue = whereValue,
                whereLabel = whereLabel,
                toValue = toValue,
                toLabel = toLabel,
                isDuration = { tracker?.dataType == DataType.DURATION },
                getNumDataPoints = {
                    tracker?.let { dao.getDataPointCount(it.featureId) } ?: 0
                },
                getDataPoints = { limit, offset ->
                    tracker?.let {
                        dao.getDataPoints(
                            featureId = it.featureId,
                            limit = limit,
                            offset = offset
                        )
                    } ?: emptyList()
                },
                performUpdate = { dao.updateDataPoints(it) }
            )
        }
    }

    override suspend fun updateTracker(
        oldTracker: Tracker,
        durationNumericConversionMode: DurationNumericConversionMode?,
        newName: String?,
        newType: DataType?,
        hasDefaultValue: Boolean?,
        defaultValue: Double?,
        defaultLabel: String?,
        featureDescription: String?,
        suggestionType: TrackerSuggestionType?,
        suggestionOrder: TrackerSuggestionOrder?
    ) = withContext(io) {
        transactionHelper.withTransaction {
            val newDataType = newType ?: oldTracker.dataType

            updateAllExistingDataPointsForTransformation(
                oldTracker,
                newDataType,
                durationNumericConversionMode
            )

            val newFeature = com.samco.trackandgraph.data.database.entity.Feature(
                id = oldTracker.featureId,
                name = newName ?: oldTracker.name,
                groupId = oldTracker.groupId,
                displayIndex = oldTracker.displayIndex,
                description = featureDescription ?: oldTracker.description
            )
            val newTracker = com.samco.trackandgraph.data.database.entity.Tracker(
                id = oldTracker.id,
                featureId = oldTracker.featureId,
                dataType = newDataType,
                hasDefaultValue = hasDefaultValue ?: oldTracker.hasDefaultValue,
                defaultValue = defaultValue ?: oldTracker.defaultValue,
                defaultLabel = defaultLabel ?: oldTracker.defaultLabel,
                suggestionType = suggestionType?.toEntity() ?: oldTracker.suggestionType.toEntity(),
                suggestionOrder = suggestionOrder?.toEntity()
                    ?: oldTracker.suggestionOrder.toEntity()
            )
            dao.updateFeature(newFeature)
            dao.updateTracker(newTracker)
        }
    }

    private fun updateAllExistingDataPointsForTransformation(
        oldTracker: Tracker,
        newType: DataType,
        durationNumericConversionMode: DurationNumericConversionMode?
    ) {
        when (oldTracker.dataType) {
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
                epochMilli = it.epochMilli,
                featureId = it.featureId,
                utcOffsetSec = it.utcOffsetSec,
                value = newValue,
                label = it.label,
                note = it.note
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
                epochMilli = it.epochMilli,
                featureId = it.featureId,
                utcOffsetSec = it.utcOffsetSec,
                value = newValue,
                label = it.label,
                note = it.note
            )
        }
        dao.updateDataPoints(newDataPoints)
    }

    private fun getAllDataPoints(featureId: Long) = dao.getDataPointsForFeatureSync(featureId)

    override suspend fun getTrackersByIdsSync(trackerIds: List<Long>): List<Tracker> =
        withContext(io) { trackerIds.mapNotNull { getTrackerById(it) } }

    override suspend fun getTrackerById(trackerId: Long): Tracker? = withContext(io) {
        dao.getTrackerById(trackerId)?.let { Tracker.fromTrackerWithFeature(it) }
    }

    override suspend fun playTimerForTracker(trackerId: Long): Long? = withContext(io) {
        dao.getTrackerById(trackerId)?.featureId?.also { featureId ->
            dao.deleteFeatureTimer(featureId)
            val timer = com.samco.trackandgraph.data.database.entity.FeatureTimer(
                0L,
                featureId,
                Instant.now()
            )
            dao.insertFeatureTimer(timer)
        }
    }

    override suspend fun stopTimerForTracker(trackerId: Long): Duration? = withContext(io) {
        dao.getTrackerById(trackerId)?.featureId?.let { featureId ->
            val timer = dao.getFeatureTimer(featureId)
            dao.deleteFeatureTimer(featureId)
            timer?.let { Duration.between(it.startInstant, Instant.now()) }
        }
    }

    override suspend fun getAllActiveTimerTrackers(): List<DisplayTracker> = withContext(io) {
        dao.getAllActiveTimerTrackers().map { it.toDto() }
    }

    override suspend fun getTrackersForGroupSync(groupId: Long): List<Tracker> =
        withContext(io) {
            dao.getTrackersForGroupSync(groupId).map { Tracker.fromTrackerWithFeature(it) }
        }

    override suspend fun getTrackerByFeatureId(featureId: Long): Tracker? = withContext(io) {
        dao.getTrackerByFeatureId(featureId)?.let { Tracker.fromTrackerWithFeature(it) }
    }

    override suspend fun hasAtLeastOneTracker(): Boolean = withContext(io) { dao.numTrackers() > 0 }

    override suspend fun hasAtLeastOneDataPoint() = withContext(io) {
        return@withContext dao.hasAtLeastOneDataPoint()
    }

    override suspend fun insertTracker(tracker: Tracker): Long = withContext(io) {
        return@withContext transactionHelper.withTransaction {
            val featureId = dao.insertFeature(tracker.toFeatureEntity())
            dao.insertTracker(tracker.toEntity().copy(featureId = featureId))
        }
    }

    override suspend fun updateTracker(tracker: Tracker) = withContext(io) {
        transactionHelper.withTransaction {
            dao.updateFeature(tracker.toFeatureEntity())
            dao.updateTracker(tracker.toEntity())
        }
    }

    override suspend fun getAllTrackersSync(): List<Tracker> = withContext(io) {
        dao.getAllTrackersSync().map { Tracker.fromTrackerWithFeature(it) }
    }

    override suspend fun getDisplayTrackersForGroupSync(groupId: Long): List<DisplayTracker> =
        withContext(io) {
            dao.getDisplayTrackersForGroupSync(groupId).map { it.toDto() }
        }

    override suspend fun tryGetDisplayTrackerByFeatureIdSync(featureId: Long): DisplayTracker? =
        withContext(io) {
            dao.getDisplayTrackerByFeatureIdSync(featureId)?.toDto()
        }

    override suspend fun getDataPointByTimestampAndTrackerSync(
        trackerId: Long,
        timestamp: OffsetDateTime
    ): DataPoint? = withContext(io) {
        return@withContext dao.getTrackerById(trackerId)?.featureId?.let {
            dao.getDataPointByTimestampAndFeatureSync(
                featureId = it,
                epochMilli = timestamp.toInstant().toEpochMilli()
            )?.toDto()
        }
    }
}