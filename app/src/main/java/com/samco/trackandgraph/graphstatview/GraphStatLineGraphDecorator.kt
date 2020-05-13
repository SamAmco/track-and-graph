/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.graphstatview

import android.content.Context
import android.view.View
import androidx.core.content.ContextCompat
import com.androidplot.Region
import com.androidplot.util.SeriesUtils
import com.androidplot.xy.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.WeekFields
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import java.util.*
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

class GraphStatLineGraphDecorator(
    private val graphOrStat: GraphOrStat,
    private val lineGraph: LineGraph,
    private val listViewMode: Boolean
) : IGraphStatViewDecorator {

    private val lineGraphHourMinuteSecondFromat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    private val lineGraphHoursDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    private val lineGraphDaysDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM")
        .withZone(ZoneId.systemDefault())
    private val lineGraphMonthsDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MM/yy")
        .withZone(ZoneId.systemDefault())

    private val currentXYRegions = mutableListOf<RectRegion>()
    private val creationTime = OffsetDateTime.now()

    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var graphStatView: IDecoratableGraphStatView? = null

    override suspend fun decorate(view: IDecoratableGraphStatView) {
        graphStatView = view
        binding = view.getBinding()
        context = view.getContext()

        binding!!.lineGraph.visibility = View.INVISIBLE
        initHeader(binding, graphOrStat)
        initFromLineGraphBody()
    }

    private suspend fun initFromLineGraphBody() {
        binding!!.progressBar.visibility = View.VISIBLE
        if (tryDrawLineGraphFeaturesAndCacheTimeRange()) {
            setUpLineGraphXAxis()
            setLineGraphBounds()
            binding!!.lineGraph.redraw()
            binding!!.lineGraph.visibility = View.VISIBLE
            binding!!.progressBar.visibility = View.GONE
        } else {
            throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_graph)
        }
    }

    private fun setLineGraphBounds() {
        val bounds = RectRegion()
        currentXYRegions.forEach { r -> bounds.union(r) }
        if (lineGraph.yRangeType == YRangeType.FIXED) {
            bounds.minY = lineGraph.yFrom
            bounds.maxY = lineGraph.yTo
            binding!!.lineGraph.setRangeBoundaries(bounds.minY, bounds.maxY, BoundaryMode.FIXED)
        }
        binding!!.lineGraph.bounds.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
        binding!!.lineGraph.outerLimits.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
        setLineGraphPaddingFromBounds(bounds)
    }

    private fun setLineGraphPaddingFromBounds(bounds: RectRegion) {
        val minY = bounds.minY.toDouble()
        val maxY = bounds.maxY.toDouble()
        val maxBound = max(abs(minY), abs(maxY))
        val numDigits = log10(maxBound).toFloat() + 3
        binding!!.lineGraph.graph.paddingLeft =
            (numDigits - 1) * (context!!.resources.displayMetrics.scaledDensity) * 3.5f
        binding!!.lineGraph.graph.refreshLayout()
    }

    private fun setUpLineGraphXAxis() {
        binding!!.lineGraph.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format =
            object : Format() {
                override fun format(
                    obj: Any,
                    toAppendTo: StringBuffer,
                    pos: FieldPosition
                ): StringBuffer {
                    val millis = (obj as Number).toLong()
                    val duration = Duration.ofMillis(millis)
                    val timeStamp = creationTime.plus(duration)
                    val formatter = getDateTimeFormatForDuration(
                        binding!!.lineGraph.bounds.minX,
                        binding!!.lineGraph.bounds.maxX
                    )
                    return toAppendTo.append(formatter.format(timeStamp))
                }

                override fun parseObject(source: String, pos: ParsePosition) = null
            }
    }

    private fun getDateTimeFormatForDuration(minX: Number?, maxX: Number?): DateTimeFormatter {
        if (minX == null || maxX == null) return lineGraphDaysDateFormat
        val duration = Duration.ofMillis(abs(maxX.toLong() - minX.toLong()))
        return when {
            duration.toMinutes() < 5L -> lineGraphHourMinuteSecondFromat
            duration.toDays() >= 304 -> lineGraphMonthsDateFormat
            duration.toDays() >= 1 -> lineGraphDaysDateFormat
            else -> lineGraphHoursDateFormat
        }
    }

    private suspend fun tryDrawLineGraphFeaturesAndCacheTimeRange(): Boolean {
        val bools = lineGraph.features.map {
            yield()
            drawLineGraphFeature(it)
        }
        return bools.any { b -> b }
    }

    private suspend fun drawLineGraphFeature(lineGraphFeature: LineGraphFeature): Boolean {
        inflateGraphLegendItem(
            binding!!, context!!,
            lineGraphFeature.colorIndex, lineGraphFeature.name
        )
        val movingAvDuration = movingAverageDurations[lineGraphFeature.averagingMode]
        val plottingPeriod = plottingModePeriods[lineGraphFeature.plottingMode]
        val rawDataSample = sampleData(
            graphStatView!!.getDataSource(),
            lineGraphFeature.featureId,
            lineGraph.duration,
            movingAvDuration,
            plottingPeriod
        )
        val plottingData = withContext(Dispatchers.IO) {
            when (lineGraphFeature.plottingMode) {
                LineGraphPlottingModes.WHEN_TRACKED -> rawDataSample
                else -> calculateDurationAccumulatedValues(rawDataSample, plottingPeriod!!)
            }
        }
        return if (dataPlottable(plottingData, 2)) {
            createAndAddSeries(plottingData, lineGraphFeature)
            true
        } else false
    }

    private suspend fun createAndAddSeries(
        rawData: RawDataSample,
        lineGraphFeature: LineGraphFeature
    ) {
        val series = getXYSeriesFromRawDataSample(rawData, lineGraphFeature)
        addSeries(series, lineGraphFeature)
    }

    private suspend fun getXYSeriesFromRawDataSample(
        rawData: RawDataSample,
        lineGraphFeature: LineGraphFeature
    ) = withContext(Dispatchers.IO) {

        val yValues = when (lineGraphFeature.averagingMode) {
            LineGraphAveraginModes.NO_AVERAGING -> rawData.dataPoints.drop(rawData.plotFrom)
                .map { dp -> dp.value.toDouble() }
            else -> calculateMovingAverage(
                rawData,
                movingAverageDurations[lineGraphFeature.averagingMode]!!
            )
        }.map { v -> (v * lineGraphFeature.scale) + lineGraphFeature.offset }

        val xValues = rawData.dataPoints.drop(rawData.plotFrom).map { dp ->
            Duration.between(creationTime, dp.timestamp).toMillis()
        }

        var yRegion = SeriesUtils.minMax(yValues)
        if (abs(yRegion.min.toDouble() - yRegion.max.toDouble()) < 0.1)
            yRegion = Region(yRegion.min, yRegion.min.toDouble() + 0.1)
        val xRegion = SeriesUtils.minMax(xValues)
        val rectRegion = RectRegion(xRegion.min, xRegion.max, yRegion.min, yRegion.max)

        return@withContext object : FastXYSeries {
            override fun minMax() = rectRegion
            override fun getX(index: Int): Number = xValues[index]
            override fun getY(index: Int): Number = yValues[index]
            override fun getTitle() = lineGraphFeature.name
            override fun size() = rawData.dataPoints.size - rawData.plotFrom
        }
    }

    private suspend fun calculateMovingAverage(
        rawData: RawDataSample,
        movingAvDuration: Duration
    ): List<Double> {
        return rawData.dataPoints
            .drop(rawData.plotFrom)
            .mapIndexed { index, dataPoint ->
                val inRange = mutableListOf(dataPoint)
                var i = rawData.plotFrom + index - 1
                while (i > 0 && Duration.between(
                        rawData.dataPoints[i].timestamp,
                        dataPoint.timestamp
                    ) <= movingAvDuration
                ) {
                    inRange.add(rawData.dataPoints[i])
                    i--
                }
                yield()
                inRange.sumByDouble { dp -> dp.value.toDouble() } / inRange.size.toDouble()
            }
    }

    private suspend fun calculateDurationAccumulatedValues(
        rawData: RawDataSample,
        period: Period
    ): RawDataSample {
        if (rawData.dataPoints.isEmpty()) return rawData
        var plotFrom = 0
        var foundPlotFrom = false
        val featureId = rawData.dataPoints[0].featureId
        val newData = mutableListOf<DataPoint>()
        var currentTimeStamp = findBeginningOfPeriod(rawData.dataPoints[0].timestamp, period)
        val latest = getNowOrLatest(rawData)
        var index = 0
        while (currentTimeStamp.isBefore(latest)) {
            currentTimeStamp = currentTimeStamp.with { ld -> ld.plus(period) }
            val points = rawData.dataPoints.drop(index)
                .takeWhile { dp -> dp.timestamp.isBefore(currentTimeStamp) }
            val total = points.sumByDouble { dp -> dp.value }
            index += points.size
            if (index > rawData.plotFrom && !foundPlotFrom) {
                plotFrom = newData.size
                foundPlotFrom = true
            }
            newData.add(DataPoint(currentTimeStamp, featureId, total, ""))
            yield()
        }
        return RawDataSample(
            newData,
            plotFrom
        )
    }

    private fun getNowOrLatest(rawData: RawDataSample): OffsetDateTime {
        val now = OffsetDateTime.now()
        if (rawData.dataPoints.isEmpty()) return now
        val latest = rawData.dataPoints.last().timestamp
        return if (latest > now) latest else now
    }

    private fun findBeginningOfPeriod(
        startDateTime: OffsetDateTime,
        period: Period
    ): OffsetDateTime {
        var dt = startDateTime
        val minusAWeek = period.minus(Period.ofWeeks(1))
        val minusAMonth = period.minus(Period.ofMonths(1))
        val minusAYear = period.minus(Period.ofYears(1))
        if (minusAYear.days >= 0 && !minusAYear.isNegative) {
            dt = startDateTime.withDayOfYear(1)
        } else if (minusAMonth.days >= 0 && !minusAMonth.isNegative) {
            dt = startDateTime
                .withDayOfMonth(1)
        } else if (minusAWeek.days >= 0 && !minusAWeek.isNegative) {
            dt = startDateTime.with(
                TemporalAdjusters.previousOrSame(
                    WeekFields.of(Locale.getDefault()).firstDayOfWeek
                )
            )
        }
        return dt.withHour(0).withMinute(0).withSecond(0).minusSeconds(1)
    }

    private fun addSeries(series: FastXYSeries, lineGraphFeature: LineGraphFeature) {
        val seriesFormat =
            if (listViewMode) {
                val sf = FastLineAndPointRenderer.Formatter(
                    ContextCompat.getColor(
                        context!!,
                        dataVisColorList[lineGraphFeature.colorIndex]
                    ),
                    null,
                    null
                )
                sf.linePaint.isAntiAlias = false

                sf.linePaint.strokeWidth = 2f * context!!.resources.displayMetrics.density
                sf
            } else {
                val sf = LineAndPointFormatter(context, R.xml.line_point_formatter)
                sf.linePaint.color =
                    ContextCompat.getColor(
                        context!!,
                        dataVisColorList[lineGraphFeature.colorIndex]
                    )
                sf
            }
        currentXYRegions.add(series.minMax())
        binding!!.lineGraph.addSeries(series, seriesFormat)
    }
}