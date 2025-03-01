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
package com.samco.trackandgraph.graphstatview.factories.helpers

import com.androidplot.util.SeriesUtils
import com.androidplot.xy.RectRegion
import com.androidplot.xy.SimpleXYSeries
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.GraphStatType
import com.samco.trackandgraph.graphstatview.factories.viewdto.IBarChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.TimeBarSegmentSeries
import com.samco.trackandgraph.lua.dto.ColorSpec
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TimeBar
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAmount
import javax.inject.Inject

class TimeBarchartLuaHelper @Inject constructor(
    private val dataDisplayIntervalHelper: DataDisplayIntervalHelper,
) {
    operator fun invoke(
        lineGraphData: LuaGraphResultData.TimeBarChartData,
        graphOrStat: GraphOrStat,
    ): ILuaGraphViewData {
        val bars = lineGraphData.bars

        val endTime = lineGraphData.endTime

        val barViewData = try {
            getBarViewData(bars)
        } catch (t: Throwable) {
            return errorProcessingBarViewData(t, graphOrStat)
        }

        val xDates = getXDates(endTime, lineGraphData.barDuration, bars.size)

        val yAxisParameters = dataDisplayIntervalHelper.getYParameters(
            yMin = 0.0,
            yMax = lineGraphData.yMax ?: lineGraphData.bars.maxOf { bar -> bar.segments.sumOf { it.value } },
            isDurationBasedRange = lineGraphData.durationBasedRange,
            fixedBounds = lineGraphData.yMax != null
        )

        val xRegion = SeriesUtils.minMax(listOf(-0.5, (lineGraphData.bars.size - 1) + 0.5))
        val yRegion = SeriesUtils.minMax(listOf(yAxisParameters.boundsMin, yAxisParameters.boundsMax))
        val bounds = RectRegion(xRegion.min, xRegion.max, yRegion.min, lineGraphData.yMax ?: yRegion.max)

        return object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData = object : IBarChartViewData {
                override val durationBasedRange: Boolean = lineGraphData.durationBasedRange
                override val xDates: List<ZonedDateTime> = xDates
                override val bars: List<TimeBarSegmentSeries> = barViewData
                override val endTime: ZonedDateTime = endTime
                override val bounds: RectRegion = bounds
                override val yAxisSubdivides: Int = yAxisParameters.subdivides
                override val barPeriod: TemporalAmount = lineGraphData.barDuration
                override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
                override val graphOrStat: GraphOrStat = graphOrStat.copy(type = GraphStatType.BAR_CHART)
            }
            override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat = graphOrStat
        }
    }

    private fun errorProcessingBarViewData(throwable: Throwable, graphOrStat: GraphOrStat) = object : ILuaGraphViewData {
        override val wrapped: IGraphStatViewData? = null
        override val state: IGraphStatViewData.State = IGraphStatViewData.State.ERROR
        override val error: Throwable = throwable
        override val graphOrStat: GraphOrStat = graphOrStat
    }

    private fun getXDates(
        endDate: ZonedDateTime,
        temporalAmount: TemporalAmount,
        barCount: Int,
    ): List<ZonedDateTime> {
        var current = endDate
        return List(barCount) {
            current.also { current = current.minus(temporalAmount) }
        }.asReversed()
    }

    private data class ColorLabel(val color: ColorSpec?, val label: String?)

    private fun blankXYSeries(size: Int, label: String) = SimpleXYSeries(
        List(size) { 0.0 },
        SimpleXYSeries.ArrayFormat.Y_VALS_ONLY,
        label
    )

    /**
     * The list of TimeBars is sorted from latest to oldest. Each time bar is a list of bar segments.
     * What we want is to get a list of series. Each series is a list with the same length as the number
     * of bars. The x value of the series is the 0 based index of the bar in the list. The y value is the
     * value of the segment for that bar. We need one list of segment values for each distinct combination
     * of color and label. The segments will be rendered layered on top of each other in order so we want to
     * preserve the order of the segments in the bars. If the segments in each bar have different order, then
     * the behaviour is un-defined.
     */
    private fun getBarViewData(bars: List<TimeBar>): List<TimeBarSegmentSeries> {
        val colorLabelsToXYSeries = mutableMapOf<ColorLabel, SimpleXYSeries>()

        val timeAscendingBars = bars.asReversed()
        for (idx in timeAscendingBars.indices) {
            val bar = timeAscendingBars[idx]
            for (segment in bar.segments) {
                val colorLabel = ColorLabel(segment.color, segment.label)
                var xySeries = colorLabelsToXYSeries[colorLabel]
                if (xySeries == null) {
                    xySeries = blankXYSeries(bars.size, segment.label ?: "")
                    colorLabelsToXYSeries[colorLabel] = xySeries
                }
                xySeries.setY(segment.value, idx)
            }
        }

        return colorLabelsToXYSeries.entries.mapIndexed { idx, (colorLabel, series) ->
            TimeBarSegmentSeries(
                segmentSeries = series,
                color = (colorLabel.color ?: indexColorSpec(idx)).toColorSpec(),
            )
        }
    }
}
