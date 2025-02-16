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

import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.toArgb
import com.nhaarman.mockitokotlin2.any
import com.nhaarman.mockitokotlin2.mock
import com.nhaarman.mockitokotlin2.verify
import com.nhaarman.mockitokotlin2.whenever
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.base.database.dto.LuaGraphFeature
import com.samco.trackandgraph.base.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.base.database.sampling.RawDataSample
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.graphstatview.factories.helpers.DataPointLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.ErrorLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.PieChartLuaHelper
import com.samco.trackandgraph.graphstatview.factories.helpers.TextLuaHelper
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITextViewData
import com.samco.trackandgraph.lua.LuaEngine
import com.samco.trackandgraph.lua.dto.ColorSpec
import com.samco.trackandgraph.lua.dto.LuaGraphResult
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.PieChartSegment
import com.samco.trackandgraph.lua.dto.TextAlignment
import com.samco.trackandgraph.lua.dto.TextSize
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.threeten.bp.OffsetDateTime
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec as ViewColorSpec

@OptIn(ExperimentalCoroutinesApi::class)
class LuaGraphDataFactoryTest {

    private val luaEngine: LuaEngine = mock()
    private val dataInteractor: DataInteractor = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val onDataSampled: (List<DataPoint>) -> Unit = mock()

    private fun uut() = LuaGraphDataFactory(
        luaEngine = { luaEngine },
        pieChartLuaHelper = PieChartLuaHelper(),
        textLuaHelper = TextLuaHelper(),
        dataPointLuaHelper = DataPointLuaHelper(),
        errorLuaHelper = ErrorLuaHelper(),
        dataInteractor = dataInteractor,
        ioDispatcher = ioDispatcher
    )

    private val rawDataSamples = mutableMapOf<Long, RawDataSample>()

    @Before
    fun setup() {
        rawDataSamples.clear()
        whenever(dataInteractor.getRawDataSampleForFeatureId(any())).thenAnswer {
            val featureId = it.getArgument<Long>(0)
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
    fun `test return text`() = runTest {
        whenever(luaEngine.runLuaGraphScript(any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.TextData(
                    text = "text",
                    size = TextSize.MEDIUM,
                    alignment = TextAlignment.START,
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is ITextViewData)
        val text = result.wrapped as ITextViewData
        assertEquals("text", text.text)
        assertEquals(ITextViewData.TextSize.MEDIUM, text.textSize)
        assertEquals(ITextViewData.TextAlignment.START, text.textAlignment)
        assertEquals(null, result.error)
    }

    @Test
    fun `pie chart returns pie chart`() = runTest {
        whenever(luaEngine.runLuaGraphScript(any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.PieChartData(
                    segments = listOf(
                        PieChartSegment(100.0, "A", null),
                        PieChartSegment(150.0, "B", ColorSpec.HexColor("#00FF00")),
                        PieChartSegment(250.0, "C", ColorSpec.ColorIndex(8)),
                        PieChartSegment(100.0, "D", ColorSpec.HexColor("#FFFF00")),
                        PieChartSegment(150.0, "E", ColorSpec.HexColor("#0000FF")),
                        PieChartSegment(250.0, "F", ColorSpec.HexColor("#FF00FF")),
                    )
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is IPieChartViewData)
        val pieChart = result.wrapped as IPieChartViewData
        assert(pieChart.graphOrStat.type == GraphStatType.PIE_CHART)

        assertEquals(
            listOf(
                IPieChartViewData.Segment(10.0, "A", null),
                IPieChartViewData.Segment(15.0, "B", ViewColorSpec.ColorValue(Color.Green.toArgb())),
                IPieChartViewData.Segment(25.0, "C", ViewColorSpec.ColorIndex(8)),
                IPieChartViewData.Segment(10.0, "D", ViewColorSpec.ColorValue(Color.Yellow.toArgb())),
                IPieChartViewData.Segment(15.0, "E", ViewColorSpec.ColorValue(Color.Blue.toArgb())),
                IPieChartViewData.Segment(25.0, "F", ViewColorSpec.ColorValue(Color.Magenta.toArgb())),
            ),
            pieChart.segments
        )
        assertEquals(null, result.error)
    }

    @Test
    fun `calls onDataSampled with all sampled data points after generating graph`() = runTest {
        val dataPoints = listOf(
            DataPoint(
                timestamp = OffsetDateTime.now(),
                featureId = 1,
                value = 1.0,
                label = "label",
                note = "note"
            ),
            DataPoint(
                timestamp = OffsetDateTime.now(),
                featureId = 2,
                value = 2.0,
                label = "label",
                note = "note"
            )
        )

        rawDataSamples[1] = RawDataSample.fromSequence(
            data = dataPoints.asSequence(),
            getRawDataPoints = { dataPoints },
            onDispose = { }
        )
        rawDataSamples[2] = RawDataSample.fromSequence(
            data = dataPoints.asSequence(),
            getRawDataPoints = { dataPoints },
            onDispose = { }
        )
        whenever(luaEngine.runLuaGraphScript(any(), any())).thenAnswer {
            LuaGraphResult(
                data = LuaGraphResultData.DataPointData(
                    dataPoint = null,
                    isDuration = false
                )
            )
        }

        callGetViewData(
            features = mapOf(
                "feature1" to 1,
                "feature2" to 2
            )
        )

        verify(onDataSampled).invoke(dataPoints + dataPoints)
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