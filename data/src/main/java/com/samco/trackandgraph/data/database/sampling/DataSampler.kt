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

package com.samco.trackandgraph.data.database.sampling

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DataType
import javax.inject.Inject

interface DataSampler {
    fun getRawDataSampleForFeatureId(featureId: Long): RawDataSample?

    fun getDataSampleForFeatureId(featureId: Long): DataSample

    suspend fun getLabelsForFeatureId(featureId: Long): List<String>

    fun getDataSamplePropertiesForFeatureId(featureId: Long): DataSampleProperties?
}

internal class DataSamplerImpl @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao
) : DataSampler {

    override fun getRawDataSampleForFeatureId(featureId: Long): RawDataSample? {
        val tracker = dao.getTrackerByFeatureId(featureId)
        if (tracker != null) {
            val cursorSequence = DataPointCursorSequence(dao.getDataPointsCursor(featureId))
            return RawDataSample.fromSequence(
                data = cursorSequence.asRawDataPointSequence(),
                getRawDataPoints = cursorSequence::getRawDataPoints,
                onDispose = cursorSequence::dispose
            )
        }
        val function = dao.getFunctionByFeatureId(featureId)
        if (function != null) {
            return FunctionTreeDataSample(function, dao)
        }
        return null
    }

    override fun getDataSampleForFeatureId(featureId: Long): DataSample {
        val tracker = dao.getTrackerByFeatureId(featureId)
        if (tracker != null) {
            val cursorSequence = DataPointCursorSequence(dao.getDataPointsCursor(featureId))
            return DataSample.fromSequence(
                data = cursorSequence.asIDataPointSequence(),
                dataSampleProperties = DataSampleProperties(isDuration = tracker.dataType == DataType.DURATION),
                getRawDataPoints = cursorSequence::getRawDataPoints,
                onDispose = cursorSequence::dispose
            )
        }
        val function = dao.getFunctionByFeatureId(featureId)
        if (function != null) {
            val properties = getDataSamplePropertiesForFeatureId(featureId)
            return FunctionTreeDataSample(function, dao).asDataSample(properties)
        }
        return DataSample.fromSequence(emptySequence(), onDispose = {})
    }

    //TODO implement getDataSamplePropertiesForFeatureId for functions
    override fun getDataSamplePropertiesForFeatureId(featureId: Long) =
        dao.getTrackerByFeatureId(featureId)?.let {
            DataSampleProperties(isDuration = it.dataType == DataType.DURATION)
        }

    //TODO implement getLabelsForFeatureId for functions
    override suspend fun getLabelsForFeatureId(featureId: Long): List<String> {
        val tracker = dao.getTrackerByFeatureId(featureId)
        return tracker?.let {
            dao.getLabelsForTracker(tracker.id)
        } ?: emptyList()
    }
}