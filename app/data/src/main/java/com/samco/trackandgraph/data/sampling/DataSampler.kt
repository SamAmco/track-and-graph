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
import com.samco.trackandgraph.data.lua.LuaVMLock
import com.samco.trackandgraph.data.sampling.functions.FunctionGraphDataSample
import timber.log.Timber
import javax.inject.Inject

/**
 * Interface for sampling data from features (trackers or functions).
 */
interface DataSampler {
    /**
     * Gets a raw data sample for the specified feature ID.
     *
     * @param featureId The ID of the feature to sample data from
     * @param vmLock Optional Lua VM lock. If the data source is derived from a function,
     *               the Lua engine will need to run to provide this data source. If you are
     *               getting one data source, iterating it, and then disposing it, you can
     *               leave this as null and a lock will be acquired for you. However, if you
     *               will get multiple data sources and iterate them all before disposing any
     *               of them, you could get a deadlock from reaching the VM limit. In such cases,
     *               you should acquire one VM lock and pass it to all calls, ensuring you do
     *               not iterate the data samples in parallel on different threads.
     * @return The raw data sample, or null if the feature doesn't exist
     */
    suspend fun getRawDataSampleForFeatureId(
        featureId: Long,
        vmLock: LuaVMLock? = null
    ): RawDataSample?

    /**
     * Gets a data sample for the specified feature ID.
     *
     * @param featureId The ID of the feature to sample data from
     * @param vmLock Optional Lua VM lock. If the data source is derived from a function,
     *               the Lua engine will need to run to provide this data source. If you are
     *               getting one data source, iterating it, and then disposing it, you can
     *               leave this as null and a lock will be acquired for you. However, if you
     *               will get multiple data sources and iterate them all before disposing any
     *               of them, you could get a deadlock from reaching the VM limit. In such cases,
     *               you should acquire one VM lock and pass it to all calls, ensuring you do
     *               not iterate the data samples in parallel on different threads.
     * @return The data sample
     */
    suspend fun getDataSampleForFeatureId(
        featureId: Long,
        vmLock: LuaVMLock? = null
    ): DataSample

    /**
     * Gets all distinct labels for the specified feature ID.
     *
     * @param featureId The ID of the feature to get labels from
     * @return List of distinct labels
     */
    suspend fun getLabelsForFeatureId(featureId: Long): List<String>

    /**
     * Gets the data sample properties for the specified feature ID.
     *
     * @param featureId The ID of the feature to get properties from
     * @return The data sample properties, or null if the feature doesn't exist
     */
    suspend fun getDataSamplePropertiesForFeatureId(featureId: Long): DataSampleProperties?
}

internal class DataSamplerImpl @Inject constructor(
    private val dataInteractor: DataInteractor,
    private val dao: TrackAndGraphDatabaseDao,
    private val luaEngine: LuaEngine
) : DataSampler {

    override suspend fun getRawDataSampleForFeatureId(
        featureId: Long,
        vmLock: LuaVMLock?
    ): RawDataSample? {
        val tracker = dao.getTrackerByFeatureId(featureId)
        if (tracker != null) {
            val cursorSequence = DataPointCursorSequence(dao.getDataPointsCursor(featureId))
            return RawDataSample.fromSequence(
                data = cursorSequence.asRawDataPointSequence(),
                getRawDataPoints = cursorSequence::getRawDataPoints,
                onDispose = cursorSequence::dispose
            )
        }
        val function = dataInteractor.tryGetFunctionByFeatureId(featureId)
        if (function != null) {
            return FunctionGraphDataSample.create(
                vmLock = vmLock,
                function = function,
                dataSampler = this,
                luaEngine = luaEngine
            )
        }
        return null
    }

    override suspend fun getDataSampleForFeatureId(
        featureId: Long,
        vmLock: LuaVMLock?
    ): DataSample {
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
        val function = dataInteractor.tryGetFunctionByFeatureId(featureId)
        if (function != null) {
            val properties = getDataSamplePropertiesForFeatureId(featureId)
            return FunctionGraphDataSample.create(
                vmLock = vmLock,
                function = function,
                dataSampler = this,
                luaEngine = luaEngine
            ).asDataSample(properties)
        }
        return DataSample.fromSequence(emptySequence(), onDispose = {})
    }

    override suspend fun getDataSamplePropertiesForFeatureId(featureId: Long): DataSampleProperties? {
        val tracker = dao.getTrackerByFeatureId(featureId)
        if (tracker != null) {
            return DataSampleProperties(isDuration = tracker.dataType == DataType.DURATION)
        }
        val function = dataInteractor.tryGetFunctionByFeatureId(featureId)
        if (function != null) {
            return DataSampleProperties(isDuration = function.functionGraph.isDuration)
        }
        return null
    }

    override suspend fun getLabelsForFeatureId(featureId: Long): List<String> {
        var dataSample: RawDataSample? = null
        try {
            // Fast case, pull directly using sql if it's a tracker
            val tracker = dao.getTrackerByFeatureId(featureId)
            if (tracker != null) {
                return dao.getLabelsForTracker(tracker.id)
            }
            // Slow case, fall back to iterating all data and getting labels
            // if it's a function
            dataSample = getRawDataSampleForFeatureId(featureId)
            if (dataSample != null) {
                return dataSample.iterator()
                    .asSequence()
                    .map { it.label }
                    .distinct()
                    .toList()
            }
            return emptyList()
        } catch (e: Throwable) {
            Timber.e(e)
            return emptyList()
        } finally {
            dataSample?.dispose()
        }
    }
}