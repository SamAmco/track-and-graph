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
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.LuaGraphFeature
import com.samco.trackandgraph.data.database.dto.LuaGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSampler
import com.samco.trackandgraph.data.sampling.RawDataSample
import com.samco.trackandgraph.graphstatview.factories.viewdto.IBarChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILineGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITextViewData
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.dto.ColorSpec
import com.samco.trackandgraph.data.lua.dto.Line
import com.samco.trackandgraph.data.lua.dto.LinePoint
import com.samco.trackandgraph.data.lua.dto.LinePointStyle
import com.samco.trackandgraph.data.lua.dto.LuaGraphResult
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.PieChartSegment
import com.samco.trackandgraph.data.lua.dto.TextAlignment
import com.samco.trackandgraph.data.lua.dto.TextSize
import com.samco.trackandgraph.data.lua.dto.TimeBar
import com.samco.trackandgraph.data.lua.dto.TimeBarSegment
import junit.framework.TestCase.assertEquals
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.any
import org.mockito.kotlin.mock
import org.mockito.kotlin.verify
import org.mockito.kotlin.whenever
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneOffset
import org.threeten.bp.ZonedDateTime
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec as ViewColorSpec

@OptIn(ExperimentalCoroutinesApi::class)
class LuaGraphDataFactoryTest {

    private val luaEngine: LuaEngine = mock()
    private val dataInteractor: DataInteractor = mock()
    private val dataSampler: DataSampler = mock()
    private val ioDispatcher: CoroutineDispatcher = UnconfinedTestDispatcher()

    private val onDataSampled: (List<DataPoint>) -> Unit = mock()

    private fun uut() = DaggerLuaDataFactoryTestComponent.builder()
        .dataInteractor(dataInteractor)
        .dataSampler(dataSampler)
        .ioDispatcher(ioDispatcher)
        .luaEngine(luaEngine)
        .build()
        .provideLuaDataFactory()

    private val rawDataSamples = mutableMapOf<Long, RawDataSample>()

    @Before
    fun setup() {
        rawDataSamples.clear()
        runBlocking {
            whenever(dataSampler.getRawDataSampleForFeatureId(any())).thenAnswer {
                val featureId = it.getArgument<Long>(0)
                rawDataSamples[featureId]
            }
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
        whenever(luaEngine.runLuaGraph(any(), any(), any()))
            .thenReturn(LuaGraphResult(error = Exception("error")))

        val result = callGetViewData()

        assertEquals(null, result.wrapped)
        assertEquals("error", result.error?.message)
    }

    @Test
    fun `test return null data point`() = runTest {
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
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

        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
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

        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
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
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
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
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
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
    fun `line graph returns line graph`() = runTest {
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.LineGraphData(
                    lines = listOf(
                        Line(
                            label = "line1",
                            lineColor = ColorSpec.HexColor("#FF0000"),
                            pointStyle = null,
                            linePoints = listOf(
                                LinePoint(OffsetDateTime.now(), 1.0),
                                LinePoint(OffsetDateTime.now(), 2.0),
                                LinePoint(OffsetDateTime.now(), 3.0),
                            )
                        ),
                        Line(
                            label = "line2",
                            lineColor = ColorSpec.ColorIndex(2),
                            pointStyle = LinePointStyle.CIRCLE_VALUE,
                            linePoints = listOf(
                                LinePoint(OffsetDateTime.now(), 4.0),
                                LinePoint(OffsetDateTime.now(), 5.0),
                                LinePoint(OffsetDateTime.now(), 6.0),
                            )
                        ),
                    ),
                    yMin = 1.0,
                    yMax = 10.0,
                    durationBasedRange = false
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is ILineGraphViewData)
        val lineGraph = result.wrapped as ILineGraphViewData
        assert(lineGraph.graphOrStat.type == GraphStatType.LINE_GRAPH)

        assertEquals("line1", lineGraph.lines[0].name)
        assertEquals(ViewColorSpec.ColorValue(Color.Red.toArgb()), lineGraph.lines[0].color)
        assertEquals(LineGraphPointStyle.NONE, lineGraph.lines[0].pointStyle)

        assertEquals("line2", lineGraph.lines[1].name)
        assertEquals(ViewColorSpec.ColorIndex(2), lineGraph.lines[1].color)
        assertEquals(LineGraphPointStyle.CIRCLES_AND_NUMBERS, lineGraph.lines[1].pointStyle)

        assertEquals(3.0, lineGraph.lines[0].line?.getY(0))
        assertEquals(2.0, lineGraph.lines[0].line?.getY(1))
        assertEquals(1.0, lineGraph.lines[0].line?.getY(2))

        assertEquals(6.0, lineGraph.lines[1].line?.getY(0))
        assertEquals(5.0, lineGraph.lines[1].line?.getY(1))
        assertEquals(4.0, lineGraph.lines[1].line?.getY(2))

        assertEquals(false, lineGraph.durationBasedRange)
        assertEquals(1.0, lineGraph.bounds.minY.toDouble())
        assertEquals(10.0, lineGraph.bounds.maxY.toDouble())
        assertEquals(YRangeType.FIXED, lineGraph.yRangeType)
    }

    @Test
    fun `bar chart returns bar chart`() = runTest {
        val endTime = ZonedDateTime.of(2021, 1, 1, 0, 0, 0, 0, ZoneOffset.UTC)

        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.TimeBarChartData(
                    barDuration = Period.ofDays(2),
                    endTime = endTime,
                    durationBasedRange = false,
                    bars = listOf(
                        TimeBar(listOf(TimeBarSegment(1.0))),
                        TimeBar(
                            listOf(TimeBarSegment(2.0, "A", ColorSpec.HexColor("#FF0000"))),
                        ),
                    ),
                    yMax = 10.0
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is IBarChartViewData)
        val barChart = result.wrapped as IBarChartViewData
        assert(barChart.graphOrStat.type == GraphStatType.BAR_CHART)

        assertEquals(endTime, barChart.endTime)
        assertEquals(2, barChart.bars.size)
        assertEquals(2, barChart.bars[0].segmentSeries.size())
        assertEquals("A", barChart.bars[0].segmentSeries.title)
        assertEquals(2.0, barChart.bars[0].segmentSeries.getY(0))
        assertEquals(0.0, barChart.bars[0].segmentSeries.getY(1))
        assertEquals(ViewColorSpec.ColorValue(Color.Red.toArgb()), barChart.bars[0].color)
        assertEquals(2, barChart.bars[1].segmentSeries.size())
        assertEquals("", barChart.bars[1].segmentSeries.title)
        assertEquals(0.0, barChart.bars[1].segmentSeries.getY(0))
        assertEquals(1.0, barChart.bars[1].segmentSeries.getY(1))
        assertEquals(ViewColorSpec.ColorIndex(7), barChart.bars[1].color)
        assertEquals(2, barChart.xDates.size)
        assertEquals(-0.5, barChart.bounds.minX.toDouble())
        assertEquals(1.5, barChart.bounds.maxX.toDouble())
        assertEquals(0.0, barChart.bounds.minY.toDouble())
        assertEquals(10.0, barChart.bounds.maxY.toDouble())
        assertEquals(6, barChart.yAxisSubdivides)
        assertEquals(Period.ofDays(2), barChart.barPeriod)
        assertEquals(false, barChart.durationBasedRange)
    }

    @Test
    fun `bar chart with non-unique labels`() = runTest {
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.TimeBarChartData(
                    barDuration = Period.ofDays(1),
                    endTime = ZonedDateTime.now(),
                    durationBasedRange = false,
                    bars = listOf(
                        TimeBar(listOf(TimeBarSegment(1.0))),
                        TimeBar(
                            listOf(TimeBarSegment(2.0, "A")),
                        ),
                        TimeBar(
                            listOf(TimeBarSegment(3.0, "B", ColorSpec.ColorIndex(1))),
                        ),
                        TimeBar(
                            listOf(TimeBarSegment(4.0, "C", ColorSpec.HexColor("#00FF00"))),
                        ),
                        TimeBar(
                            listOf(TimeBarSegment(5.0, "C", ColorSpec.HexColor("#00FFF0"))),
                        ),
                    ),
                    yMax = 10.0
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is IBarChartViewData)
        val barChart = result.wrapped as IBarChartViewData
        assert(barChart.graphOrStat.type == GraphStatType.BAR_CHART)

        assertEquals(5, barChart.bars.size)
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0), barChart.bars[0].segmentSeries.getxVals().map { it.toDouble() })
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0), barChart.bars[1].segmentSeries.getxVals().map { it.toDouble() })
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0), barChart.bars[2].segmentSeries.getxVals().map { it.toDouble() })
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0), barChart.bars[3].segmentSeries.getxVals().map { it.toDouble() })
        assertEquals(listOf(0.0, 1.0, 2.0, 3.0, 4.0), barChart.bars[4].segmentSeries.getxVals().map { it.toDouble() })

        assertEquals(listOf(5.0, 0.0, 0.0, 0.0, 0.0), barChart.bars[0].segmentSeries.getyVals().map { it.toDouble() })
        assertEquals(listOf(0.0, 4.0, 0.0, 0.0, 0.0), barChart.bars[1].segmentSeries.getyVals().map { it.toDouble() })
        assertEquals(listOf(0.0, 0.0, 3.0, 0.0, 0.0), barChart.bars[2].segmentSeries.getyVals().map { it.toDouble() })
        assertEquals(listOf(0.0, 0.0, 0.0, 2.0, 0.0), barChart.bars[3].segmentSeries.getyVals().map { it.toDouble() })
        assertEquals(listOf(0.0, 0.0, 0.0, 0.0, 1.0), barChart.bars[4].segmentSeries.getyVals().map { it.toDouble() })

        assertEquals(listOf("C", "C", "B", "A", ""), barChart.bars.map { it.segmentSeries.title })

        assertEquals(
            listOf(
                ViewColorSpec.ColorValue(Color(0, 255, 240).toArgb()),
                ViewColorSpec.ColorValue(Color.Green.toArgb()),
                ViewColorSpec.ColorIndex(1),
                ViewColorSpec.ColorIndex(9),
                ViewColorSpec.ColorIndex(4),
            ),
            barChart.bars.map { it.color }
        )
    }

    @Test
    fun `bar chart preserves first seen segment order`() = runTest {
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
            LuaGraphResult(
                data = LuaGraphResultData.TimeBarChartData(
                    barDuration = Period.ofDays(1),
                    endTime = ZonedDateTime.now(),
                    durationBasedRange = false,
                    bars = listOf(
                        TimeBar(
                            listOf(
                                TimeBarSegment(1.0, "A"),
                                TimeBarSegment(1.0, "B")
                            ),
                        ),
                        TimeBar(
                            listOf(
                                TimeBarSegment(1.0, "B"),
                                TimeBarSegment(1.0, "A")
                            ),
                        ),
                    ),
                    yMax = 10.0
                )
            )
        )

        val result = callGetViewData()

        assert(result.wrapped is IBarChartViewData)
        val barChart = result.wrapped as IBarChartViewData

        // The order chosen is the first order seen in the data when the bars are drawn from left to right.
        // Given that the bars arrive reversed, the order should be the order of the last segments in the list.
        // The decision to use the order of the first seen segments is arbitrary, however it should be predictable.
        // The really important thing here is that if all segments are always passed in the same order, that order should
        // be respected. If you jumble the orders behaviour is un-defined but deterministic.
        assertEquals(
            listOf("B", "A"),
            listOf(barChart.bars[0].segmentSeries.title, barChart.bars[1].segmentSeries.title),
        )
    }

    @Test
    fun `null data and error shows no data`() = runTest {
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
            LuaGraphResult(
                data = null,
                error = null,
            )
        )

        val result = callGetViewData()

        assertEquals(null, result.wrapped)
        assertEquals(null, result.error)
        assertEquals(false, result.hasData)
        assertEquals(IGraphStatViewData.State.READY, result.state)
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
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenAnswer {
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
        whenever(luaEngine.runLuaGraph(any(), any(), any())).thenReturn(
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
        whenever(luaEngine.runLuaGraph(any(), any(), any()))
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