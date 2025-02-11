package com.samco.trackandgraph.lua

import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.whenever
import com.samco.trackandgraph.assetreader.AssetReader
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.sampling.RawDataSample
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.lua.dto.LuaGraphResult
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
        return DaggerLuaDataFactoryTestComponent.builder()
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
        dataSources: Map<String, Sequence<TestDP>>,
        script: String,
        assertionBlock: AssertionScope.() -> Unit
    ) {
        val data = dataSources.mapValues { (_, data) ->
            object : RawDataSample() {
                private val iterator = data.iterator()
                private val sampled = mutableListOf<DataPoint>()
                override fun getRawDataPoints() = sampled
                override fun iterator() = object : Iterator<DataPoint> {
                    override fun hasNext() = iterator.hasNext()
                    override fun next() = iterator.next().toDataPoint().also { sampled.add(it) }
                }

                override fun dispose() = Unit
            }
        }

        val iterators = data.mapValues { (_, sample) -> sample.iterator() }

        val uut = uut()

        val result = uut.runLuaGraphScript(
            script,
            next = { source, count ->
                val iterator = iterators[source] ?: throw IllegalArgumentException("No data sample found for $source")
                val batchSample = mutableListOf<DataPoint>()
                while (batchSample.size < count && iterator.hasNext()) {
                    batchSample.add(iterator.next())
                }
                batchSample
            }
        )

        AssertionScope(
            result = result,
            sampledData = data.mapValues { (_, sample) -> sample.getRawDataPoints() }
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