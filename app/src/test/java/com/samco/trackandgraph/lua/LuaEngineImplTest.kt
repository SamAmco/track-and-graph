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
package com.samco.trackandgraph.lua

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.samco.trackandgraph.assetreader.AssetReader
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.lua.dto.LuaGraphResult
import com.samco.trackandgraph.util.rawDataSampleFromSequence
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.threeten.bp.OffsetDateTime

@OptIn(ExperimentalCoroutinesApi::class)
abstract class LuaEngineImplTest {

    protected val dataInteractor: DataInteractor = mock()
    protected val assetReader: AssetReader = mock()
    protected val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    protected val luaTng = javaClass.getClassLoader()
        ?.getResourceAsStream("generated/lua/tng.lua")
        .use { it?.bufferedReader()?.readText() }

    protected fun uut(): LuaEngineImpl {
        whenever(assetReader.readAssetToString("generated/lua/tng.lua"))
            .thenReturn(luaTng)
        return DaggerLuaEngineTestComponent.builder()
            .dataInteractor(dataInteractor)
            .assetReader(assetReader)
            .ioDispatcher(ioDispatcher)
            .build()
            .provideLuaEngine()
    }

    protected data class AssertionScope(
        val result: LuaGraphResult,
        val sampledData: Map<String, List<DataPoint>>,
    )

    protected fun testLuaEngine(
        script: String,
        assertionBlock: AssertionScope.() -> Unit
    ) = testLuaEngine(emptyMap(), script, assertionBlock)

    protected fun testLuaEngine(
        dataSources: Map<String, Sequence<TestDP>>,
        script: String,
        assertionBlock: AssertionScope.() -> Unit
    ) {
        val uut = uut()

        val sources = dataSources.mapValues { (_, source) ->
            val asDataPoints = source.map { it.toDataPoint() }
            rawDataSampleFromSequence(asDataPoints) {}
        }

        val result = uut.runLuaGraphScript(
            script,
            LuaEngine.LuaGraphEngineParams(sources)
        )

        AssertionScope(
            result = result,
            sampledData = sources.mapValues { (_, sample) -> sample.getRawDataPoints() }
        ).assertionBlock()
    }

    protected data class TestDP(
        val timestamp: OffsetDateTime = OffsetDateTime.now(),
        val featureId: Long = 1,
        val value: Double = 1.0,
        val label: String = "",
        val note: String = ""
    ) {
        fun toDataPoint() = DataPoint(
            timestamp = timestamp,
            featureId = featureId,
            value = value,
            label = label,
            note = note
        )
    }
}