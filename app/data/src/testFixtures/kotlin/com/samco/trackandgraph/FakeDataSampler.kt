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

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.lua.LuaVMLock
import com.samco.trackandgraph.data.sampling.DataSample
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.sampling.DataSampleProperties
import com.samco.trackandgraph.data.sampling.RawDataSample

/**
 * Fake DataSampler for testing that allows setting data points per feature ID.
 */
class FakeDataSampler : DataSampler {
    private val dataPointsByFeature = mutableMapOf<Long, List<DataPoint>>()

    fun setDataPointsForFeature(featureId: Long, dataPoints: List<DataPoint>) {
        dataPointsByFeature[featureId] = dataPoints
    }

    fun clear() {
        dataPointsByFeature.clear()
    }

    override suspend fun getRawDataSampleForFeatureId(
        featureId: Long,
        vmLock: LuaVMLock?
    ): RawDataSample? {
        val dataPoints = dataPointsByFeature[featureId] ?: return null
        if (dataPoints.isEmpty()) return null
        return RawDataSample.fromSequence(
            data = dataPoints.asSequence(),
            getRawDataPoints = { dataPoints },
            onDispose = { }
        )
    }

    override suspend fun getDataSampleForFeatureId(
        featureId: Long,
        vmLock: LuaVMLock?
    ): DataSample {
        return DataSample.fromSequence(emptySequence(), onDispose = {})
    }

    override suspend fun getLabelsForFeatureId(featureId: Long): List<String> = emptyList()

    override suspend fun getDataSamplePropertiesForFeatureId(featureId: Long): DataSampleProperties? = null
}

/**
 * A no-op DataSampler that always returns null/empty results.
 * Useful for tests that don't need DataSampler functionality.
 */
class NoOpDataSampler : DataSampler {
    override suspend fun getRawDataSampleForFeatureId(
        featureId: Long,
        vmLock: LuaVMLock?
    ): RawDataSample? = null

    override suspend fun getDataSampleForFeatureId(
        featureId: Long,
        vmLock: LuaVMLock?
    ): DataSample = DataSample.fromSequence(emptySequence(), onDispose = {})

    override suspend fun getLabelsForFeatureId(featureId: Long): List<String> = emptyList()

    override suspend fun getDataSamplePropertiesForFeatureId(featureId: Long): DataSampleProperties? = null
}
