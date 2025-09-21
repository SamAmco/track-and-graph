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

package com.samco.trackandgraph.data.sampling

import com.samco.trackandgraph.data.database.TrackAndGraphDatabaseDao
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.sampling.functions.FunctionGraphDataSample
import javax.inject.Inject

interface DataSampler {
    suspend fun getRawDataSampleForFeatureId(featureId: Long): RawDataSample?

    suspend fun getDataSampleForFeatureId(featureId: Long): DataSample

    suspend fun getLabelsForFeatureId(featureId: Long): List<String>

    suspend fun getDataSamplePropertiesForFeatureId(featureId: Long): DataSampleProperties?
}

internal class DataSamplerImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val dao: TrackAndGraphDatabaseDao,
    private val luaEngine: LuaEngine
) : DataSampler {

    override suspend fun getRawDataSampleForFeatureId(featureId: Long): RawDataSample? {
        val tracker = dao.getTrackerByFeatureId(featureId)
        if (tracker != null) {
            val cursorSequence = DataPointCursorSequence(dao.getDataPointsCursor(featureId))
            return RawDataSample.fromSequence(
                data = cursorSequence.asRawDataPointSequence(),
                getRawDataPoints = cursorSequence::getRawDataPoints,
                onDispose = cursorSequence::dispose
            )
        }
        val function = dataInteractor.getFunctionByFeatureId(featureId)
        if (function != null) {
            return FunctionGraphDataSample.create(function, this, luaEngine)
        }
        return null
    }

    override suspend fun getDataSampleForFeatureId(featureId: Long): DataSample {
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
        val function = dataInteractor.getFunctionByFeatureId(featureId)
        if (function != null) {
            val properties = getDataSamplePropertiesForFeatureId(featureId)
            return FunctionGraphDataSample.create(function, this, luaEngine)
                .asDataSample(properties)
        }
        return DataSample.fromSequence(emptySequence(), onDispose = {})
    }

    override suspend fun getDataSamplePropertiesForFeatureId(featureId: Long): DataSampleProperties? {
        val tracker = dao.getTrackerByFeatureId(featureId)
        if (tracker != null) {
            return DataSampleProperties(isDuration = tracker.dataType == DataType.DURATION)
        }
        val function = dataInteractor.getFunctionByFeatureId(featureId)
        if (function != null) {
            return DataSampleProperties(isDuration = function.functionGraph.isDuration)
        }
        return null
    }

    override suspend fun getLabelsForFeatureId(featureId: Long): List<String> {
        // Fast case, pull directly using sql if it's a tracker
        val tracker = dao.getTrackerByFeatureId(featureId)
        if (tracker != null) {
            return dao.getLabelsForTracker(tracker.id)
        }
        // Slow case, fall back to iterating all data and getting labels
        // if it's a function
        val rawDataSample = getRawDataSampleForFeatureId(featureId)
        if (rawDataSample != null) {
            return rawDataSample.iterator()
                .asSequence()
                .map { it.label }
                .distinct()
                .toList()
        }
        return emptyList()
    }
}