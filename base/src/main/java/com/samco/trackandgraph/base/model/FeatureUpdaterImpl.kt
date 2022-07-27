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
import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.model.FeatureUpdater.DurationNumericConversionMode
import com.samco.trackandgraph.base.model.di.IODispatcher
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import javax.inject.Inject
import com.samco.trackandgraph.base.database.entity.DataPoint as DataPointEntity

internal class FeatureUpdaterImpl @Inject constructor(
    private val database: TrackAndGraphDatabase,
    private val dao: TrackAndGraphDatabaseDao,
    @IODispatcher private val io: CoroutineDispatcher
) : FeatureUpdater {
    override suspend fun updateFeature(
        oldFeature: Feature,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>,
        durationNumericConversionMode: DurationNumericConversionMode?,
        newName: String?,
        newFeatureType: DataType?,
        newDiscreteValues: List<DiscreteValue>?,
        hasDefaultValue: Boolean?,
        defaultValue: Double?,
        featureDescription: String?
    ) = withContext(io) {
        database.withTransaction {
            updateFeatureImpl(
                oldFeature,
                discreteValueMap,
                durationNumericConversionMode,
                newName,
                newFeatureType,
                newDiscreteValues,
                hasDefaultValue,
                defaultValue,
                featureDescription
            )
        }
    }

    private fun updateFeatureImpl(
        oldFeature: Feature,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>,
        durationNumericConversionMode: DurationNumericConversionMode? = null,
        newName: String? = null,
        newFeatureType: DataType? = null,
        newDiscreteValues: List<DiscreteValue>? = null,
        hasDefaultValue: Boolean? = null,
        defaultValue: Double? = null,
        featureDescription: String? = null
    ) {
        val newType = newFeatureType ?: oldFeature.featureType

        updateAllExistingDataPointsForTransformation(
            oldFeature,
            newType,
            discreteValueMap,
            durationNumericConversionMode
        )

        val feature = Feature(
            oldFeature.id,
            newName ?: oldFeature.name,
            oldFeature.groupId,
            newType,
            newDiscreteValues ?: oldFeature.discreteValues,
            oldFeature.displayIndex,
            hasDefaultValue ?: oldFeature.hasDefaultValue,
            defaultValue ?: oldFeature.defaultValue,
            featureDescription ?: oldFeature.description
        )
        dao.updateFeature(feature.toEntity())
    }

    private fun updateAllExistingDataPointsForTransformation(
        oldFeature: Feature,
        newFeatureType: DataType,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>,
        durationNumericConversionMode: DurationNumericConversionMode?
    ) {
        when (oldFeature.featureType) {
            DataType.DISCRETE -> when (newFeatureType) {
                DataType.CONTINUOUS -> stripDataPointsToValue(oldFeature)
                DataType.DISCRETE -> updateDiscreteValueDataPoints(oldFeature, discreteValueMap)
                else -> {}
            }
            DataType.CONTINUOUS -> when (newFeatureType) {
                DataType.DURATION -> {
                    if (durationNumericConversionMode == null) noConversionModeError()
                    updateContinuousDataPointsToDurations(
                        oldFeature,
                        durationNumericConversionMode
                    )
                }
                else -> {}
            }
            DataType.DURATION -> when (newFeatureType) {
                DataType.CONTINUOUS -> {
                    if (durationNumericConversionMode == null) noConversionModeError()
                    updateDurationDataPointsToContinuous(
                        oldFeature,
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
        oldFeature: Feature,
        durationNumericConversionMode: DurationNumericConversionMode
    ) {
        val oldDataPoints = getAllDataPoints(oldFeature)
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

    private fun updateContinuousDataPointsToDurations(
        oldFeature: Feature,
        durationNumericConversionMode: DurationNumericConversionMode
    ) {
        val oldDataPoints = getAllDataPoints(oldFeature)
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

    private fun stripDataPointsToValue(oldFeature: Feature) {
        val oldDataPoints = getAllDataPoints(oldFeature)
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

    private fun getAllDataPoints(feature: Feature) = dao.getDataPointsForFeatureSync(feature.id)

    private fun updateDiscreteValueDataPoints(
        oldFeature: Feature,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>
    ) {
        //Remove any discrete values that have no mapping
        oldFeature.discreteValues
            .filter { dv -> !discreteValueMap.keys.contains(dv) }
            .forEach { i -> removeExistingDataPointsForDiscreteValue(oldFeature.id, i.index) }

        //Update existing data points to their new discrete values
        if (discreteValueMap.any { it.key != it.value }) {
            updateExistingDataPointsForDiscreteValue(oldFeature, discreteValueMap)
        }
    }

    private fun updateExistingDataPointsForDiscreteValue(
        feature: Feature,
        discreteValueMap: Map<DiscreteValue, DiscreteValue>
    ) {
        val oldValues = getAllDataPoints(feature)
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