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
* along with Foobar.  If not, see <https://www.gnu.org/licenses/>.
*/
package com.samco.trackandgraph.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getColor
import com.androidplot.Plot
import com.androidplot.Region
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
import com.androidplot.ui.HorizontalPositioning
import com.androidplot.ui.Size
import com.androidplot.ui.VerticalPositioning
import com.androidplot.util.SeriesUtils
import com.androidplot.xy.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import kotlinx.coroutines.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
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

abstract class GraphStatViewBase : FrameLayout {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    private var binding = getBinding()
    private var currJob: Job? = null
    private var viewScope: CoroutineScope? = null
    private val dataSource = TrackAndGraphDatabase.getInstance(context.applicationContext).trackAndGraphDatabaseDao
    private val creationTime = OffsetDateTime.now()
    private var listViewMode = false

    private val currentXYRegions = mutableListOf<RectRegion>()

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

    protected abstract fun getBinding(): GraphStatViewBinding

    init {
        basicLineGraphSetup()
        initInvalid()
    }

    private fun resetJob() {
        currJob?.cancel()
        currJob = Job()
        viewScope = CoroutineScope(Dispatchers.Main + currJob!!)
    }

    private fun basicLineGraphSetup() {
        binding.lineGraph.layoutManager.remove(binding.lineGraph.domainTitle)
        binding.lineGraph.layoutManager.remove(binding.lineGraph.rangeTitle)
        binding.lineGraph.layoutManager.remove(binding.lineGraph.title)
        binding.lineGraph.setBorderStyle(Plot.BorderStyle.NONE, null, null)
        binding.lineGraph.graph.size = Size.FILL
        binding.lineGraph.graph.position(0f, HorizontalPositioning.ABSOLUTE_FROM_LEFT, 0f, VerticalPositioning.ABSOLUTE_FROM_TOP)
        binding.lineGraph.setPlotMargins(0f, 0f, 0f, 0f)
        binding.lineGraph.setPlotPadding(0f, 0f, 0f, 0f)
        binding.lineGraph.graph.setPadding(0f,0f,0f,0f)
        binding.lineGraph.graph.setMargins(0f, 20f, 0f, 50f)
    }

    fun addLineGraphPanAndZoom() {
        PanZoom.attach(binding.lineGraph, PanZoom.Pan.HORIZONTAL, PanZoom.Zoom.STRETCH_HORIZONTAL)
    }

    private fun cleanAllViews() {
        binding.legendFlexboxLayout.removeAllViews()
        currentXYRegions.clear()
        binding.lineGraph.clear()
        binding.lineGraph.graph.paddingLeft = 0f
        binding.lineGraph.setRangeBoundaries(0, 1, BoundaryMode.AUTO)
        binding.lineGraph.setDomainBoundaries(0, 1, BoundaryMode.GROW)
        binding.lineGraph.graph.refreshLayout()
        binding.pieChart.clear()
        binding.progressBar.visibility = View.GONE
        binding.lineGraph.visibility = View.GONE
        binding.pieChart.visibility = View.GONE
        binding.errorMessage.visibility = View.GONE
        binding.statMessage.visibility = View.GONE
        binding.errorMessage.text = ""
        binding.headerText.text = ""
        binding.statMessage.text = ""
    }

    fun initLoading() {
        resetJob()
        cleanAllViews()
        binding.progressBar.visibility = View.VISIBLE
    }

    fun initInvalid() {
        resetJob()
        cleanAllViews()
        initErrorTextView(null, R.string.graph_stat_view_invalid_setup)
    }

    fun initError(graphOrStat: GraphOrStat?, errorTextId: Int) {
        resetJob()
        initErrorTextView(graphOrStat, errorTextId)
    }

    private fun initErrorTextView(graphOrStat: GraphOrStat?, errorTextId: Int) {
        cleanAllViews()
        graphOrStat?.let { initHeader(graphOrStat) }
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = context.getString(errorTextId)
    }

    private fun initHeader(graphOrStat: GraphOrStat) {
        binding.headerText.text = graphOrStat.name
    }

    fun initTimeSinceStat(graphOrStat: GraphOrStat, timeSinceLastStat: TimeSinceLastStat) {
        try {
            resetJob()
            cleanAllViews()
            binding.statMessage.visibility = View.INVISIBLE
            initHeader(graphOrStat)
            viewScope!!.launch { initTimeSinceStatBody(graphOrStat, timeSinceLastStat) }
        } catch (e: Exception) {
            initError(graphOrStat, R.string.graph_stat_view_unkown_error)
        }
    }

    private suspend fun initTimeSinceStatBody(graphOrStat: GraphOrStat, timeSinceLastStat: TimeSinceLastStat) {
        binding.progressBar.visibility = View.VISIBLE
        val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(timeSinceLastStat.featureId) }
        var lastDataPointTimeStamp: OffsetDateTime? = null
        val lastDataPoint = withContext(Dispatchers.IO) {
            dataSource.getLastDataPointBetween(feature.id, timeSinceLastStat.fromValue, timeSinceLastStat.toValue)
        }
        if (lastDataPoint != null) lastDataPointTimeStamp = lastDataPoint.timestamp
        binding.statMessage.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
        if (lastDataPointTimeStamp == null) { initErrorTextView(graphOrStat, R.string.graph_stat_view_not_enough_data_stat) }
        else {
            while (true) {
                setTimeSinceStatText(Duration.between(lastDataPointTimeStamp, OffsetDateTime.now()))
                delay(1000)
                yield()
            }
        }
    }

    fun initAverageTimeBetweenStat(graphOrStat: GraphOrStat, timeBetweenStat: AverageTimeBetweenStat) {
        try {
            resetJob()
            cleanAllViews()
            binding.statMessage.visibility = View.INVISIBLE
            initHeader(graphOrStat)
            viewScope!!.launch { initAverageTimeBetweenStatBody(graphOrStat, timeBetweenStat) }
        } catch (e: Exception) { initError(graphOrStat, R.string.graph_stat_view_unkown_error) }
    }

    private suspend fun initAverageTimeBetweenStatBody(graphOrStat: GraphOrStat, timeBetweenStat: AverageTimeBetweenStat) {
        binding.progressBar.visibility = View.VISIBLE
        val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(timeBetweenStat.featureId) }
        val dataPoints = withContext(Dispatchers.IO) {
            if (timeBetweenStat.duration == null) {
                dataSource.getDataPointsBetween(feature.id, timeBetweenStat.fromValue, timeBetweenStat.toValue)
            } else {
                val now = OffsetDateTime.now()
                val cutOff = now.minus(timeBetweenStat.duration)
                dataSource.getDataPointsBetweenInTimeRange(feature.id, timeBetweenStat.fromValue, timeBetweenStat.toValue, cutOff, now)
            }
        }
        if (dataPoints.size < 2) initErrorTextView(graphOrStat, R.string.graph_stat_view_not_enough_data_stat)
        else {
            val totalMillis = Duration.between(dataPoints.first().timestamp, dataPoints.last().timestamp).toMillis().toDouble()
            val averageMillis = totalMillis / dataPoints.size.toDouble()
            setAverageTimeBetweenStatText(averageMillis)
        }

        binding.statMessage.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    private fun setAverageTimeBetweenStatText(millis: Double) {
        val days = "%.1f".format((millis / 86400000).toFloat())
        binding.statMessage.text = "$days ${context.getString(R.string.days)}"
    }

    private fun setTimeSinceStatText(duration: Duration) {
        val totalSeconds = duration.toMillis() / 1000.toDouble()
        val daysNum = (totalSeconds / 86400).toInt()
        val days = daysNum.toString()
        val hours = "%02d".format(((totalSeconds % 86400) / 3600).toInt())
        val minutes = "%02d".format(((totalSeconds % 3600) / 60).toInt())
        val seconds = "%02d".format((totalSeconds % 60).toInt())
        val hms = "$hours:$minutes:$seconds"
        binding.statMessage.text =
            when {
                daysNum == 1 -> "$days ${context.getString(R.string.day)}\n$hms"
                daysNum > 0 -> "$days ${context.getString(R.string.days)}\n$hms"
                else -> hms
            }
    }

    fun initFromLineGraph(graphOrStat: GraphOrStat, lineGraph: LineGraph, listViewMode: Boolean = false) {
        try {
            this.listViewMode = listViewMode
            resetJob()
            cleanAllViews()
            binding.lineGraph.visibility = View.INVISIBLE
            initHeader(graphOrStat)
            viewScope!!.launch { initFromLineGraphBody(graphOrStat, lineGraph) }
        } catch (e: Exception) { initError(graphOrStat, R.string.graph_stat_view_unkown_error) }
    }

    private suspend fun initFromLineGraphBody(graphOrStat: GraphOrStat, lineGraph: LineGraph) {
        binding.progressBar.visibility = View.VISIBLE
        if (tryDrawLineGraphFeaturesAndCacheTimeRange(lineGraph)) {
            setUpLineGraphXAxis()
            setLineGraphBounds(lineGraph)
            binding.lineGraph.redraw()
            binding.lineGraph.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        } else {
            initErrorTextView(graphOrStat, R.string.graph_stat_view_not_enough_data_graph)
        }
    }

    private fun setLineGraphBounds(lineGraph: LineGraph) {
        val bounds = RectRegion()
        currentXYRegions.forEach { r -> bounds.union(r) }
        if (lineGraph.yRangeType == YRangeType.FIXED) {
            bounds.minY = lineGraph.yFrom
            bounds.maxY = lineGraph.yTo
            binding.lineGraph.setRangeBoundaries(bounds.minY, bounds.maxY, BoundaryMode.FIXED)
        }
        binding.lineGraph.bounds.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
        binding.lineGraph.outerLimits.set(bounds.minX, bounds.maxX, bounds.minY, bounds.maxY)
        setLineGraphPaddingFromBounds(bounds)
    }

    private fun setLineGraphPaddingFromBounds(bounds: RectRegion) {
        val minY = bounds.minY?.toDouble() ?: 0.toDouble()
        val maxY = bounds.maxY?.toDouble() ?: 0.toDouble()
        val maxBound = max(abs(minY), abs(maxY))
        val numDigits = log10(maxBound).toFloat() + 3
        binding.lineGraph.graph.paddingLeft = (numDigits-1) * (context.resources.displayMetrics.scaledDensity) * 3.5f
        binding.lineGraph.graph.refreshLayout()
    }

    fun initFromPieChart(graphOrStat: GraphOrStat, pieChart: PieChart) {
        try {
            resetJob()
            cleanAllViews()
            binding.pieChart.visibility = View.INVISIBLE
            initHeader(graphOrStat)
            viewScope!!.launch { initFromPieChartBody(graphOrStat, pieChart) }
        } catch (e: Exception) { initError(graphOrStat, R.string.graph_stat_view_unkown_error) }
    }

    private suspend fun initFromPieChartBody(graphOrStat: GraphOrStat, pieChart: PieChart) {
        binding.progressBar.visibility = View.VISIBLE
        val dataSample = tryGetPlottableDataForPieChart(pieChart)
        if (dataSample == null) {
            initErrorTextView(graphOrStat, R.string.graph_stat_view_not_enough_data_graph)
            return
        }
        val segments = getPieChartSegments(dataSample)
        val total = withContext(Dispatchers.IO) {
            segments.sumByDouble { s -> s.value.toDouble() }
        }
        plotPieChartSegments(segments, total)
        binding.pieChart.redraw()
        binding.pieChart.getRenderer(PieRenderer::class.java).setDonutSize(0f, PieRenderer.DonutMode.PERCENT)
        binding.pieChart.visibility = View.VISIBLE
        binding.progressBar.visibility = View.GONE
    }

    private fun plotPieChartSegments(segments: List<Segment>, total: Double) {
        segments.forEachIndexed { i, s ->
            val index = (dataVisColorGenerator * i) % dataVisColorList.size
            val colorId = dataVisColorList[index]
            val segForm = SegmentFormatter(getColor(context, colorId))
            segForm.labelPaint.color = Color.TRANSPARENT
            val percentage = "%.1f".format((s.value.toDouble() / total) * 100f)
            inflateGraphLegendItem(index, "${s.title} ($percentage%)")
            binding.pieChart.addSegment(s, segForm)
        }
    }

    private suspend fun getPieChartSegments(dataSample: RawDataSample) = withContext(Dispatchers.IO) {
        dataSample.dataPoints
            .drop(dataSample.plotFrom)
            .groupingBy { dp -> dp.label }
            .eachCount()
            .map { b -> Segment(b.key, b.value) }
    }

    private suspend fun tryGetPlottableDataForPieChart(pieChart: PieChart): RawDataSample? {
        val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(pieChart.featureId) }
        val dataSample = sampleData(feature.id, pieChart.duration, null, null)
        return if (dataPlottable(dataSample)) dataSample else null
    }

    private suspend fun tryDrawLineGraphFeaturesAndCacheTimeRange(lineGraph: LineGraph): Boolean {
        val bools = lineGraph.features.map {
            yield()
            drawLineGraphFeature(lineGraph, it)
        }
        return bools.any { b -> b }
    }

    private fun setUpLineGraphXAxis() {
        binding.lineGraph.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format = object : Format() {
            override fun format(obj: Any, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
                val millis = (obj as Number).toLong()
                val duration = Duration.ofMillis(millis)
                val timeStamp = creationTime.plus(duration)
                val formatter = getDateTimeFormatForDuration(
                    binding.lineGraph.bounds.minX,
                    binding.lineGraph.bounds.maxX
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

    private class RawDataSample(val dataPoints: List<DataPoint>, val plotFrom: Int)

    private suspend fun drawLineGraphFeature(lineGraph: LineGraph, lineGraphFeature: LineGraphFeature): Boolean {
        inflateGraphLegendItem(lineGraphFeature.colorIndex, lineGraphFeature.name)
        val movingAvDuration = movingAverageDurations[lineGraphFeature.averagingMode]
        val plottingPeriod = plottingModePeriods[lineGraphFeature.plottingMode]
        val rawDataSample = sampleData(lineGraphFeature.featureId, lineGraph.duration, movingAvDuration, plottingPeriod)
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

    private fun inflateGraphLegendItem(colorIndex: Int, label: String) {
        val colorId = dataVisColorList[colorIndex]
        binding.legendFlexboxLayout.addView(GraphLegendItemView(context, colorId, label))
    }

    private fun dataPlottable(rawData: RawDataSample, minDataPoints: Int = 1): Boolean {
        return rawData.plotFrom >= 0
                && rawData.dataPoints.size - rawData.plotFrom >= minDataPoints
    }

    private suspend fun sampleData(featureId: Long, sampleDuration: Duration?,
                                   averagingDuration: Duration?, plottingPeriod: Period?): RawDataSample {
        return withContext(Dispatchers.IO) {
            if (sampleDuration == null) RawDataSample(dataSource.getDataPointsForFeatureAscSync(featureId), 0)
            else {
                val latest = getLatestTimeOrNowForFeature(featureId)
                val startDate = latest.minus(sampleDuration)
                val plottingDuration = plottingPeriod?.let { Duration.between(latest, latest.plus(plottingPeriod)) }
                val maxSampleDuration = listOf(
                    sampleDuration,
                    averagingDuration?.plus(sampleDuration),
                    plottingDuration?.plus(sampleDuration)
                ).maxBy { d -> d ?: Duration.ZERO }
                val minSampleDate = latest.minus(maxSampleDuration)
                val dataPoints = dataSource.getDataPointsForFeatureBetweenAscSync(featureId, minSampleDate, latest)
                val startIndex = dataPoints.indexOfFirst { dp -> dp.timestamp.isAfter(startDate) }
                RawDataSample(dataPoints, startIndex)
            }
        }
    }

    private suspend fun getLatestTimeOrNowForFeature(featureId: Long): OffsetDateTime {
        val lastDataPointList = dataSource.getLastDataPointForFeatureSync(featureId)
        val now = OffsetDateTime.now()
        val latest = lastDataPointList.firstOrNull()?.let {
            it.timestamp.plusSeconds(1)
        }
        return listOfNotNull(now, latest).max()!!
    }

    private suspend fun createAndAddSeries(rawData: RawDataSample, lineGraphFeature: LineGraphFeature) {
        val series = getXYSeriesFromRawDataSample(rawData, lineGraphFeature)
        addSeries(series, lineGraphFeature)
    }

    private fun addSeries(series: FastXYSeries, lineGraphFeature: LineGraphFeature) {
        val seriesFormat =
            if (listViewMode) {
                val sf = FastLineAndPointRenderer.Formatter(
                    getColor(context, dataVisColorList[lineGraphFeature.colorIndex]),
                    null,
                    null
                )
                sf.linePaint.isAntiAlias = false
                sf.linePaint.strokeWidth = 2f * resources.displayMetrics.density
                sf
            } else {
                val sf = LineAndPointFormatter(context, R.xml.line_point_formatter)
                sf.linePaint.color = getColor(context, dataVisColorList[lineGraphFeature.colorIndex])
                sf
            }
        currentXYRegions.add(series.minMax())
        binding.lineGraph.addSeries(series, seriesFormat)
    }

    private suspend fun getXYSeriesFromRawDataSample(rawData: RawDataSample, lineGraphFeature: LineGraphFeature)
            = withContext(Dispatchers.IO) {

        val yValues = when (lineGraphFeature.averagingMode) {
            LineGraphAveraginModes.NO_AVERAGING -> rawData.dataPoints.drop(rawData.plotFrom).map { dp -> dp.value.toDouble() }
            else -> calculateMovingAverage(rawData, movingAverageDurations[lineGraphFeature.averagingMode]!!)
        }.map { v -> (v * lineGraphFeature.scale) + lineGraphFeature.offset }

        val xValues = rawData.dataPoints.drop(rawData.plotFrom).map {
                dp -> Duration.between(creationTime, dp.timestamp).toMillis()
        }

        var yRegion = SeriesUtils.minMax(yValues)
        if (abs(yRegion.min.toDouble() - yRegion.max.toDouble()) < 0.1)
            yRegion = Region(yRegion.min, yRegion.min.toDouble() + 0.1)
        val xRegion = SeriesUtils.minMax(xValues)
        val rectRegion = RectRegion(xRegion.min, xRegion.max, yRegion.min, yRegion.max)

        return@withContext object: FastXYSeries {
            override fun minMax() = rectRegion
            override fun getX(index: Int): Number = xValues[index]
            override fun getY(index: Int): Number = yValues[index]
            override fun getTitle() = lineGraphFeature.name
            override fun size() = rawData.dataPoints.size - rawData.plotFrom
        }
    }

    private suspend fun calculateDurationAccumulatedValues(rawData: RawDataSample, period: Period): RawDataSample {
        if (rawData.dataPoints.isEmpty()) return rawData
        var plotFrom = 0
        var foundPlotFrom = false
        val featureId = rawData.dataPoints[0].featureId
        val newData = mutableListOf<DataPoint>()
        var currentTimeStamp = findBeginningOfPeriod(rawData.dataPoints[0].timestamp, period)
        val latest = getNowOrLatest(rawData)
        var index = 0
        while (currentTimeStamp.isBefore(latest)) {
            currentTimeStamp = currentTimeStamp.with {ld -> ld.plus(period)}
            val points = rawData.dataPoints.drop(index).takeWhile { dp -> dp.timestamp.isBefore(currentTimeStamp) }
            val total = points.sumByDouble { dp -> dp.value }
            index += points.size
            if (index > rawData.plotFrom && !foundPlotFrom) {
                plotFrom = newData.size
                foundPlotFrom = true
            }
            newData.add(DataPoint(currentTimeStamp, featureId, total, ""))
            yield()
        }
        return RawDataSample(newData, plotFrom)
    }

    private fun getNowOrLatest(rawData: RawDataSample): OffsetDateTime {
        val now = OffsetDateTime.now()
        if (rawData.dataPoints.isEmpty()) return now
        val latest = rawData.dataPoints.last().timestamp
        return if (latest > now) latest else now
    }

    private fun findBeginningOfPeriod(startDateTime: OffsetDateTime, period: Period): OffsetDateTime {
        var dt = startDateTime
        val minusAWeek = period.minus(Period.ofWeeks(1))
        val minusAMonth = period.minus(Period.ofMonths(1))
        val minusAYear = period.minus(Period.ofYears(1))
        if (minusAYear.days >= 0 && !minusAYear.isNegative) {
            dt = startDateTime.withDayOfYear(1)
        }
        else if (minusAMonth.days >= 0 && !minusAMonth.isNegative) {
            dt = startDateTime
                .withDayOfMonth(1)
        }
        else if (minusAWeek.days >= 0 && !minusAWeek.isNegative) {
            dt = startDateTime.with(TemporalAdjusters.previousOrSame(
                WeekFields.of(Locale.getDefault()).firstDayOfWeek
            ))
        }
        return dt.withHour(0).withMinute(0).withSecond(0).minusSeconds(1)
    }

    private suspend fun calculateMovingAverage(rawData: RawDataSample, movingAvDuration: Duration): List<Double> {
        return rawData.dataPoints
            .drop(rawData.plotFrom)
            .mapIndexed { index, dataPoint ->
                val inRange = mutableListOf(dataPoint)
                var i = rawData.plotFrom+index-1
                while (i > 0 && Duration.between(rawData.dataPoints[i].timestamp, dataPoint.timestamp) <= movingAvDuration) {
                    inRange.add(rawData.dataPoints[i])
                    i--
                }
                yield()
                inRange.sumByDouble { dp -> dp.value.toDouble() } / inRange.size.toDouble()
            }
    }

    fun dispose() { currJob?.cancel() }
}


