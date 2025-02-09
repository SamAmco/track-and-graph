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

package com.samco.trackandgraph.base.database.sampling

import com.samco.trackandgraph.base.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.base.database.dto.DataType
import javax.inject.Inject

interface DataSampler {
    fun getRawDataSampleForFeatureId(featureId: Long): RawDataSample?

    fun getDataSampleForFeatureId(featureId: Long): DataSample

    suspend fun getLabelsForFeatureId(featureId: Long): List<String>

    suspend fun getDataSamplePropertiesForFeatureId(featureId: Long): DataSampleProperties?
}

internal class DataSamplerImpl @Inject constructor(
    private val dao: TrackAndGraphDatabaseDao
) : DataSampler {

    override fun getRawDataSampleForFeatureId(featureId: Long): RawDataSample? {
        val tracker = dao.getTrackerByFeatureId(featureId)
        return tracker?.let {
            val cursorSequence = DataPointCursorSequence(dao.getDataPointsCursor(featureId))
            RawDataSample.fromSequence(
                data = cursorSequence.asRawDataPointSequence(),
                dataSampleProperties = DataSampleProperties(isDuration = tracker.dataType == DataType.DURATION),
                getRawDataPoints = cursorSequence::getRawDataPoints,
                onDispose = cursorSequence::dispose
            )
        }
    }

    override fun getDataSampleForFeatureId(featureId: Long): DataSample {
        val tracker = dao.getTrackerByFeatureId(featureId)
        return tracker?.let {
            val cursorSequence = DataPointCursorSequence(dao.getDataPointsCursor(featureId))
            DataSample.fromSequence(
                data = cursorSequence.asIDataPointSequence(),
                dataSampleProperties = DataSampleProperties(isDuration = tracker.dataType == DataType.DURATION),
                getRawDataPoints = cursorSequence::getRawDataPoints,
                onDispose = cursorSequence::dispose
            )
        } ?: DataSample.fromSequence(emptySequence(), onDispose = {})
    }

    override suspend fun getDataSamplePropertiesForFeatureId(featureId: Long) =
        dao.getTrackerByFeatureId(featureId)?.let {
            DataSampleProperties(isDuration = it.dataType == DataType.DURATION)
        }

    override suspend fun getLabelsForFeatureId(featureId: Long): List<String> {
        val tracker = dao.getTrackerByFeatureId(featureId)
        return tracker?.let {
            dao.getLabelsForTracker(tracker.id)
        } ?: emptyList()
    }
}