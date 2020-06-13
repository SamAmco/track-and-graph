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
import android.graphics.Paint
import android.view.View
import androidx.core.content.ContextCompat
import com.androidplot.ui.VerticalPosition
import com.androidplot.ui.VerticalPositioning
import com.androidplot.xy.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import com.samco.trackandgraph.util.formatDayMonth
import com.samco.trackandgraph.util.formatMonthYear
import com.samco.trackandgraph.util.getColorFromAttr
import kotlinx.coroutines.*
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import java.text.DecimalFormat
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import kotlin.math.abs
import kotlin.math.log10
import kotlin.math.max

class GraphStatLineGraphDecorator(
    private val graphOrStat: GraphOrStat,
    private val lineGraph: LineGraph,
    private val listViewMode: Boolean,
    private val onSampledDataCallback: SampleDataCallback?
) : IGraphStatViewDecorator {
    private val lineGraphHourMinuteSecondFromat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm:ss")
        .withZone(ZoneId.systemDefault())
    private val lineGraphHoursDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())

    private val currentXYRegions = mutableListOf<RectRegion>()
    private lateinit var endTime: OffsetDateTime

    private var binding: GraphStatViewBinding? = null
    private var context: Context? = null
    private var graphStatView: IDecoratableGraphStatView? = null

    private val allReferencedDataPoints: MutableList<DataPoint> = mutableListOf()

    override suspend fun decorate(view: IDecoratableGraphStatView) {
        graphStatView = view
        binding = view.getBinding()
        context = view.getContext()
        endTime = graphOrStat.endDate ?: OffsetDateTime.now()

        yield()
        binding!!.lineGraph.visibility = View.INVISIBLE
        initHeader(binding, graphOrStat)
        initFromLineGraphBody()
        onSampledDataCallback?.invoke(allReferencedDataPoints)
    }

    override fun setTimeMarker(time: OffsetDateTime) {
        binding!!.lineGraph.removeMarkers()
        val markerPaint = getMarkerPaint()
        val millis = Duration.between(endTime, time).toMillis()
        binding!!.lineGraph.addMarker(
            XValueMarker(
                millis,
                null,
                VerticalPosition(0f, VerticalPositioning.ABSOLUTE_FROM_TOP),
                markerPaint,
                null
            )
        )
        binding!!.lineGraph.redraw()
    }

    private suspend fun initFromLineGraphBody() {
        yield()
        binding!!.progressBar.visibility = View.VISIBLE
        if (tryDrawLineGraphFeatures()) {
            setUpLineGraphXAxis()
            setLineGraphBounds()
            yield()
            binding!!.lineGraph.redraw()
            binding!!.lineGraph.visibility = View.VISIBLE
            binding!!.progressBar.visibility = View.GONE
        } else {
            throw GraphStatInitException(R.string.graph_stat_view_not_enough_data_graph)
        }
    }

    private suspend fun setLineGraphBounds() {
        val bounds = RectRegion()
        currentXYRegions.forEach { r -> bounds.union(r) }
        yield()
        if (lineGraph.yRangeType == YRangeType.FIXED) {
            bounds.minY = lineGraph.yFrom
            bounds.maxY = lineGraph.yTo
            binding!!.lineGraph.setRangeBoundaries(bounds.minY, bounds.maxY, BoundaryMode.FIXED)
        }
        binding!!.lineGraph.bounds.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
        binding!!.lineGraph.outerLimits.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
        setLineGraphPaddingFromBounds(bounds)
    }

    private suspend fun setLineGraphPaddingFromBounds(bounds: RectRegion) {
        val minY = bounds.minY.toDouble()
        val maxY = bounds.maxY.toDouble()
        val maxBound = max(abs(minY), abs(maxY))
        val numDigits = log10(maxBound).toFloat() + 3
        yield()
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
                    val timeStamp = endTime.plus(duration)
                    val formattedTimestamp = getDateTimeFormattedForDuration(
                        binding!!.lineGraph.bounds.minX,
                        binding!!.lineGraph.bounds.maxX,
                        timeStamp
                    )
                    return toAppendTo.append(formattedTimestamp)
                }

                override fun parseObject(source: String, pos: ParsePosition) = null
            }
    }

    private fun getDateTimeFormattedForDuration(
        minX: Number?,
        maxX: Number?,
        timestamp: OffsetDateTime
    ): String {
        if (minX == null || maxX == null) return formatDayMonth(context!!, timestamp)
        val duration = Duration.ofMillis(abs(maxX.toLong() - minX.toLong()))
        return when {
            duration.toMinutes() < 5L -> lineGraphHourMinuteSecondFromat.format(timestamp)
            duration.toDays() >= 304 -> formatMonthYear(context!!, timestamp)
            duration.toDays() >= 1 -> formatDayMonth(context!!, timestamp)
            else -> lineGraphHoursDateFormat.format(timestamp)
        }
    }

    private suspend fun tryDrawLineGraphFeatures(): Boolean {
        val bools = lineGraph.features.map {
            yield()
            inflateGraphLegendItem(binding!!, context!!, it.colorIndex, it.name)
            val plottingData = withContext(Dispatchers.IO) { tryGetPlottingData(it) }
            return@map if (plottingData != null) {
                createAndAddSeries(plottingData, it)
                true
            } else false
        }
        return bools.any { b -> b }
    }

    private suspend fun tryGetPlottingData(lineGraphFeature: LineGraphFeature): DataSample? {
        val movingAvDuration = movingAverageDurations[lineGraphFeature.averagingMode]
        val plottingPeriod = plottingModePeriods[lineGraphFeature.plottingMode]
        val rawDataSample = sampleData(
            graphStatView!!.getDataSource(),
            lineGraphFeature.featureId,
            lineGraph.duration,
            graphOrStat.endDate,
            movingAvDuration,
            plottingPeriod
        )
        val visibleSection = clipDataSample(rawDataSample, graphOrStat.endDate, lineGraph.duration)
        allReferencedDataPoints.addAll(visibleSection.dataPoints)
        val plotTotalData = when (lineGraphFeature.plottingMode) {
            LineGraphPlottingModes.WHEN_TRACKED -> rawDataSample
            else -> calculateDurationAccumulatedValues(
                rawDataSample,
                lineGraphFeature.featureId,
                lineGraph.duration,
                graphOrStat.endDate,
                plottingPeriod!!
            )
        }
        val averagedData = when (lineGraphFeature.averagingMode) {
            LineGraphAveraginModes.NO_AVERAGING -> plotTotalData
            else -> calculateMovingAverages(
                plotTotalData,
                movingAvDuration!!
            )
        }
        val plottingData = clipDataSample(averagedData, graphOrStat.endDate, lineGraph.duration)

        return if (plottingData.dataPoints.size >= 2) plottingData else null
    }

    private suspend fun createAndAddSeries(
        rawData: DataSample,
        lineGraphFeature: LineGraphFeature
    ) {
        val series = withContext(Dispatchers.IO) {
            getXYSeriesFromDataSample(rawData, endTime, lineGraphFeature)
        }
        addSeries(series, lineGraphFeature)
    }

    private suspend fun addSeries(series: FastXYSeries, lineGraphFeature: LineGraphFeature) {
        val seriesFormat =
            if (listViewMode && lineGraphFeature.pointStyle != LineGraphPointStyle.CIRCLES_AND_NUMBERS)
                getFastLineAndPointFormatter(lineGraphFeature)
            else getLineAndPointFormatter(lineGraphFeature)
        currentXYRegions.add(series.minMax())
        yield()
        binding!!.lineGraph.addSeries(series, seriesFormat)
    }

    private fun getLineAndPointFormatter(lineGraphFeature: LineGraphFeature): LineAndPointFormatter {
        val formatter = LineAndPointFormatter()
        formatter.linePaint.apply {
            color = getLinePaintColor(lineGraphFeature)
            strokeWidth = getLinePaintWidth()
        }
        getVertexPaintColor(lineGraphFeature)?.let {
            formatter.vertexPaint.color = it
            formatter.vertexPaint.strokeWidth = getVertexPaintWidth()
        } ?: run {
            formatter.vertexPaint = null
        }
        getPointLabelFormatter(lineGraphFeature)?.let {
            formatter.pointLabelFormatter = it
            formatter.setPointLabeler { series, index ->
                DecimalFormat("#.#").format(series.getY(index))
            }
        } ?: run {
            formatter.pointLabelFormatter = null
        }
        formatter.fillPaint = null
        return formatter
    }

    private fun getFastLineAndPointFormatter(lineGraphFeature: LineGraphFeature): LineAndPointFormatter {
        val formatter = FastLineAndPointRenderer.Formatter(
            getLinePaintColor(lineGraphFeature),
            getVertexPaintColor(lineGraphFeature),
            getPointLabelFormatter(lineGraphFeature)
        )
        formatter.linePaint?.apply { isAntiAlias = false }
        formatter.linePaint?.apply { strokeWidth = getLinePaintWidth() }
        formatter.vertexPaint?.apply { strokeWidth = getVertexPaintWidth() }
        return formatter
    }

    private fun getLinePaintWidth() =
        context!!.resources.getDimension(R.dimen.line_graph_line_thickness)

    private fun getVertexPaintWidth() =
        context!!.resources.getDimension(R.dimen.line_graph_vertex_thickness)

    private fun getLinePaintColor(lineGraphFeature: LineGraphFeature): Int {
        return getPaintColor(lineGraphFeature)
    }

    private fun getVertexPaintColor(lineGraphFeature: LineGraphFeature): Int? {
        return if (lineGraphFeature.pointStyle == LineGraphPointStyle.NONE) null
        else getPaintColor(lineGraphFeature)
    }

    private fun getPointLabelFormatter(lineGraphFeature: LineGraphFeature): PointLabelFormatter? {
        if (lineGraphFeature.pointStyle != LineGraphPointStyle.CIRCLES_AND_NUMBERS) return null
        val color = context!!.getColorFromAttr(android.R.attr.textColorPrimary)
        val pointLabelFormatter = PointLabelFormatter(
            color,
            context!!.resources.getDimension(R.dimen.line_graph_point_label_h_offset),
            context!!.resources.getDimension(R.dimen.line_graph_point_label_v_offset)
        )
        pointLabelFormatter.textPaint.textAlign = Paint.Align.RIGHT
        return pointLabelFormatter
    }

    private fun getPaintColor(lineGraphFeature: LineGraphFeature) =
        ContextCompat.getColor(context!!, dataVisColorList[lineGraphFeature.colorIndex])

    private fun getMarkerPaint(): Paint {
        val color = context!!.getColorFromAttr(R.attr.errorTextColor)
        val paint = Paint()
        paint.color = color
        paint.strokeWidth = 2f
        return paint
    }
}