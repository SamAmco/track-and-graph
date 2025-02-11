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
package com.samco.trackandgraph.graphstatview.factories

import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.times
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.samco.trackandgraph.assetreader.AssetReader
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.base.database.dto.LuaGraphFeature
import com.samco.trackandgraph.base.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.base.database.sampling.RawDataSample
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.lua.DaggerLuaDataFactoryTestComponent
import com.samco.trackandgraph.lua.LuaEngine
import com.samco.trackandgraph.lua.dto.LuaGraphResult
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.luaj.vm2.cli.lua
import org.mockito.Mock
import org.threeten.bp.OffsetDateTime
import javax.inject.Provider

@OptIn(ExperimentalCoroutinesApi::class)
class LuaGraphDataFactoryTest {

    private val luaEngine: LuaEngine = mock()
    private val dataInteractor: DataInteractor = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val onDataSampled: (List<DataPoint>) -> Unit = mock()

    private fun uut() = LuaGraphDataFactory(
        luaEngine = { luaEngine },
        dataInteractor = dataInteractor,
        ioDispatcher = ioDispatcher
    )

    private val rawDataSamples = mutableMapOf<Long, RawDataSample>()

    @Before
    fun setup() {
        rawDataSamples.clear()
        whenever(dataInteractor.getRawDataSampleForFeatureId(any())).thenAnswer {
            val featureId = it.getArgument<Long>(0)
            rawDataSamples[featureId] = mock()
            rawDataSamples[featureId]
        }
    }

    private suspend fun callGetViewData(features: Map<String, Long> = emptyMap()): ILuaGraphViewData {
        val graphOrStat = GraphOrStat(
            id = 1,
            groupId = 1,
            name = "name",
            type = GraphStatType.LUA_SCRIPT,
            displayIndex = 1
        )
        val luaGraph = luaGraph(
            features = features.map { (name, id) ->
                luaGraphFeature(name = name, featureId = id)
            }
        )
        return uut().getViewData(graphOrStat, luaGraph, onDataSampled)
    }

    @Test
    fun `test return error`() = runTest {
        whenever(luaEngine.runLuaGraphScript(any(), any()))
            .thenReturn(LuaGraphResult(error = Exception("error")))

        val result = callGetViewData()

        assertEquals(null, result.wrapped)
        assertEquals("error", result.error?.message)
    }

    @Test
    fun `test return null data point`() = runTest {
        whenever(luaEngine.runLuaGraphScript(any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.DataPointData(
                    dataPoint = null,
                    isDuration = false
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is ILastValueViewData)
        val lastValue = result.wrapped as ILastValueViewData
        assertEquals(null, lastValue.lastDataPoint)
        assertEquals(false, lastValue.isDuration)
        assertEquals(null, result.error)
    }

    @Test
    fun `test return data point isDuration false`() = runTest {
        val dataPoint = DataPoint(
            timestamp = OffsetDateTime.now(),
            featureId = 1,
            value = 1.0,
            label = "label",
            note = "note"
        )

        whenever(luaEngine.runLuaGraphScript(any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.DataPointData(
                    dataPoint = dataPoint,
                    isDuration = false
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is ILastValueViewData)
        val lastValue = result.wrapped as ILastValueViewData
        assertEquals(dataPoint, lastValue.lastDataPoint)
        assertEquals(false, lastValue.isDuration)
        assertEquals(null, result.error)
    }

    @Test
    fun `test return data point isDuration true`() = runTest {
        val dataPoint = DataPoint(
            timestamp = OffsetDateTime.now(),
            featureId = 1,
            value = 1.0,
            label = "label",
            note = "note"
        )

        whenever(luaEngine.runLuaGraphScript(any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.DataPointData(
                    dataPoint = dataPoint,
                    isDuration = true
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is ILastValueViewData)
        val lastValue = result.wrapped as ILastValueViewData
        assertEquals(dataPoint, lastValue.lastDataPoint)
        assertEquals(true, lastValue.isDuration)
        assertEquals(null, result.error)
    }

    @Test
    fun `closes data sample after script`() = runTest {
        whenever(luaEngine.runLuaGraphScript(any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.DataPointData(
                    dataPoint = null,
                    isDuration = false
                )
            )
        )

        callGetViewData(
            features = mapOf(
                "feature1" to 1,
                "feature2" to 2
            )
        )

        rawDataSamples.values.forEach {
            verify(it).dispose()
        }
    }

    @Test
    fun `closes data sample after script error`() = runTest {
        whenever(luaEngine.runLuaGraphScript(any(), any()))
            .thenThrow(RuntimeException("error"))

        callGetViewData(
            features = mapOf(
                "feature1" to 1,
                "feature2" to 2
            )
        )

        rawDataSamples.values.forEach {
            verify(it).dispose()
        }
    }

    private fun luaGraph(
        script: String = "",
        features: List<LuaGraphFeature> = emptyList(),
    ) = LuaGraphWithFeatures(
        id = 1,
        graphStatId = 1,
        script = script,
        features = features
    )

    private fun luaGraphFeature(
        id: Long = 1,
        luaGraphId: Long = 1,
        featureId: Long = 1,
        name: String = "name"
    ) = LuaGraphFeature(
        id = id,
        luaGraphId = luaGraphId,
        featureId = featureId,
        name = name
    )
}