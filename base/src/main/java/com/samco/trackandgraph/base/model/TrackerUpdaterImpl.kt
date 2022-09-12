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
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.DiscreteValue
import com.samco.trackandgraph.base.database.dto.Tracker
import com.samco.trackandgraph.base.model.TrackerUpdater.DurationNumericConversionMode
import com.samco.trackandgraph.base.model.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.samco.trackandgraph.base.database.entity.DataPoint as DataPointEntity

internal class TrackerUpdaterImpl @Inject constructor(
    private val database: TrackAndGraphDatabase,
    private val dao: TrackAndGraphDatabaseDao,
    @IODispatcher private val io: CoroutineDispatcher
) : TrackerUpdater {
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
            .forEach { i -> removeExistingDataPointsForDiscreteValue(oldTracker.featureId, i.index) }

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

}