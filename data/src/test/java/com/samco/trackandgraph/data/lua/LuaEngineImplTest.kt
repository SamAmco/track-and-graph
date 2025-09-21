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
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.assetreader.AssetReader
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.sampling.RawDataSample
import com.samco.trackandgraph.data.time.TimeProvider
import com.samco.trackandgraph.data.time.TimeProviderImpl
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import org.mockito.ArgumentMatchers.anyString
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

@OptIn(ExperimentalCoroutinesApi::class)
internal abstract class LuaEngineImplTest {

    protected val dataInteractor: DataInteractor = mock()
    protected val assetReader: AssetReader = mock()
    protected val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()
    protected open val timeProvider: TimeProvider = TimeProviderImpl()

    private fun readAssetToString(path: String): String? {
        return javaClass.getClassLoader()
            ?.getResourceAsStream(path)
            .use { it?.bufferedReader()?.readText() }
    }

    protected fun uut(): LuaEngineImpl {
        whenever(assetReader.readAssetToString(anyString())).thenAnswer {
            val path = it.getArgument<String>(0)
            readAssetToString(path)
        }
        return DaggerLuaEngineTestComponent.builder()
            .dataInteractor(dataInteractor)
            .assetReader(assetReader)
            .ioDispatcher(ioDispatcher)
            .timeProvider(timeProvider)
            .build()
            .provideLuaEngine()
    }

    protected data class LuaGraphAssertionScope(
        val result: LuaGraphResult,
        val sampledData: Map<String, List<DataPoint>>,
    )

    protected data class LuaFunctionAssertionScope(
        val result: Sequence<DataPoint>,
        val inputDataSources: List<List<DataPoint>>,
    ) {
        // Convert the result sequence to a list for easier testing
        val resultList: List<DataPoint> by lazy { result.toList() }
    }

    protected fun testLuaGraph(
        script: String,
        assertionBlock: LuaGraphAssertionScope.() -> Unit
    ) = testLuaGraph(emptyMap(), script, assertionBlock)

    protected fun testLuaGraph(
        dataSources: Map<String, Sequence<TestDP>>,
        script: String,
        assertionBlock: LuaGraphAssertionScope.() -> Unit
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

        LuaGraphAssertionScope(
            result = result,
            sampledData = sources.mapValues { (_, sample) -> sample.getRawDataPoints() }
        ).assertionBlock()
    }

    protected fun testLuaFunction(
        script: String,
        assertionBlock: LuaFunctionAssertionScope.() -> Unit
    ) = testLuaFunction(emptyList(), script, assertionBlock)

    protected fun testLuaFunction(
        dataSources: List<Sequence<TestDP>>,
        script: String,
        assertionBlock: LuaFunctionAssertionScope.() -> Unit
    ) {
        val uut = uut()

        val rawDataSources = dataSources.map { source ->
            val asDataPoints = source.map { it.toDataPoint() }
            rawDataSampleFromSequence(asDataPoints) {}
        }

        val result = uut.runLuaFunctionScript(script, rawDataSources)

        LuaFunctionAssertionScope(
            result = result,
            inputDataSources = rawDataSources.map { it.getRawDataPoints() }
        ).assertionBlock()
    }

    protected data class TestDP(
        val timestamp: OffsetDateTime = OffsetDateTime.now(),
        val value: Double = 1.0,
        val label: String = "",
        val note: String = "",
        val featureId: Long = 1,
    ) {
        constructor(
            timestamp: Long,
            value: Double = 1.0,
            label: String = "",
            note: String = "",
            featureId: Long = 1,
        ) : this(
            timestamp = OffsetDateTime.ofInstant(Instant.ofEpochMilli(timestamp), ZoneOffset.UTC),
            value = value,
            label = label,
            note = note,
            featureId = featureId
        )

        fun toDataPoint() = DataPoint(
            timestamp = timestamp,
            featureId = featureId,
            value = value,
            label = label,
            note = note
        )
    }

    private fun rawDataSampleFromSequence(
        data: Sequence<DataPoint>,
        onDispose: () -> Unit
    ) = object : RawDataSample() {
        private val visited = mutableListOf<DataPoint>()
        override fun getRawDataPoints() = visited
        override fun iterator() = data.onEach { visited.add(it) }.iterator()
        override fun dispose() = onDispose()
    }
}