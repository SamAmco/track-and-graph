package com.samco.grapheasy.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
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
import com.samco.grapheasy.databinding.GraphStatViewBinding
import kotlinx.coroutines.*
import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import org.threeten.bp.temporal.TemporalAdjusters
import org.threeten.bp.temporal.WeekFields
import timber.log.Timber
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import java.util.*
import kotlin.math.abs
import kotlin.math.roundToLong

class GraphStatView(
    context: Context,
    attrSet: AttributeSet
) : FrameLayout(context, attrSet) {
    private var binding = GraphStatViewBinding.inflate(LayoutInflater.from(context), this, true)
    private var currJob: Job? = null
    private var viewScope: CoroutineScope? = null
    private val dataSource = GraphEasyDatabase.getInstance(context.applicationContext).graphEasyDatabaseDao

    private class NullableTimeRange(val minDateTime: OffsetDateTime?, val maxDateTime: OffsetDateTime?)
    private class MutableTimeRange(min: OffsetDateTime, max: OffsetDateTime) {
        var minDateTime: OffsetDateTime = min
            set(value) {
                field = value
                timeDiffMillis = calculateTimeDiff(minDateTime, maxDateTime)
            }
        var maxDateTime: OffsetDateTime = max
            set(value) {
                field = value
                timeDiffMillis = calculateTimeDiff(minDateTime, maxDateTime)
            }
        var timeDiffMillis: Long = calculateTimeDiff(min, max)
            private set
        fun calculateTimeDiff(min: OffsetDateTime, max: OffsetDateTime) = Duration.between(min, max).toMillis()
        init { timeDiffMillis = calculateTimeDiff(min, max) }
    }

    private val currentTimeRange: MutableTimeRange = MutableTimeRange(OffsetDateTime.now(), OffsetDateTime.now())

    private val lineGraphHoursDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    private val lineGraphDaysDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM")
        .withZone(ZoneId.systemDefault())
    private val lineGraphMonthsDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MM/yy")
        .withZone(ZoneId.systemDefault())

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

    private fun cleanAllViews() {
        binding.legendFlexboxLayout.removeAllViews()
        binding.lineGraph.clear()
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

    fun initInvalid() {
        resetJob()
        cleanAllViews()
        initError(null, R.string.graph_stat_view_invalid_setup)
    }

    private fun initError(graphOrStat: GraphOrStat?, errorTextId: Int) {
        cleanAllViews()
        graphOrStat?.let { initHeader(graphOrStat) }
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = context.getString(errorTextId)
    }

    private fun initHeader(graphOrStat: GraphOrStat) {
        binding.headerText.text = graphOrStat.name
    }

    fun initTimeSinceStat(graphOrStat: GraphOrStat, timeSinceLastStat: TimeSinceLastStat) {
        resetJob()
        cleanAllViews()
        binding.statMessage.visibility = View.INVISIBLE
        initHeader(graphOrStat)
        viewScope!!.launch {
            binding.progressBar.visibility = View.VISIBLE
            val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(timeSinceLastStat.featureId) }
            var lastDataPointTimeStamp: OffsetDateTime? = null
            val lastDataPoint = withContext(Dispatchers.IO) {
                dataSource.getLastDataPointBetween(feature.id, timeSinceLastStat.fromValue, timeSinceLastStat.toValue)
            }
            if (lastDataPoint != null) lastDataPointTimeStamp = lastDataPoint.timestamp
            binding.statMessage.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
            if (lastDataPointTimeStamp == null) { initError(graphOrStat, R.string.graph_stat_view_not_enough_data_stat) }
            else {
                while (true) {
                    setTimeSinceStatText(Duration.between(lastDataPointTimeStamp, OffsetDateTime.now()))
                    delay(1000)
                    yield()
                }
            }
        }
    }

    fun initAverageTimeBetweenStat(graphOrStat: GraphOrStat, timeBetweenStat: AverageTimeBetweenStat) {
        resetJob()
        cleanAllViews()
        binding.statMessage.visibility = View.INVISIBLE
        initHeader(graphOrStat)
        viewScope!!.launch {
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
            if (dataPoints.size < 2) initError(graphOrStat, R.string.graph_stat_view_not_enough_data_stat)
            else {
                val totalMillis = Duration.between(dataPoints.first().timestamp, dataPoints.last().timestamp).toMillis().toDouble()
                val averageMillis = totalMillis / dataPoints.size.toDouble()
                setAverageTimeBetweenStatText(averageMillis)
            }

            binding.statMessage.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
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

    fun initFromLineGraph(graphOrStat: GraphOrStat, lineGraph: LineGraph) {
        resetJob()
        cleanAllViews()
        binding.lineGraph.visibility = View.INVISIBLE
        initHeader(graphOrStat)
        viewScope!!.launch {
            binding.progressBar.visibility = View.VISIBLE
            if (tryDrawLineGraphFeaturesAndCacheTimeRange(lineGraph)) {
                setUpLineGraphXAxis()
                binding.lineGraph.redraw()
                binding.lineGraph.visibility = View.VISIBLE
                binding.progressBar.visibility = View.GONE
            } else {
                initError(graphOrStat, R.string.graph_stat_view_not_enough_data_graph)
            }
        }
    }

    fun initFromPieChart(graphOrStat: GraphOrStat, pieChart: PieChart) {
        resetJob()
        cleanAllViews()
        binding.pieChart.visibility = View.INVISIBLE
        initHeader(graphOrStat)
        viewScope!!.launch {
            binding.progressBar.visibility = View.VISIBLE
            val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(pieChart.featureId) }
            val dataSample = sampleData(feature.id, pieChart.duration, null, null)
            if (!dataPlottable(dataSample)) {
                initError(graphOrStat, R.string.graph_stat_view_not_enough_data_graph)
                return@launch
            }
            val segments = dataSample.dataPoints
                .drop(dataSample.plotFrom)
                .groupingBy { dp -> dp.label }
                .eachCount()
                .map { b -> Segment(b.key, b.value) }
            val total = segments.sumByDouble { s -> s.value.toDouble() }
            segments.forEachIndexed { i, s ->
                val index = (dataVisColorGenerator * i) % dataVisColorList.size
                val colorId = dataVisColorList[index]
                val segForm = SegmentFormatter(getColor(context, colorId))
                segForm.labelPaint.color = Color.TRANSPARENT
                val percentage = "%.1f".format((s.value.toDouble() / total) * 100f)
                inflateGraphLegendItem(colorId, "${s.title} ($percentage%)")
                binding.pieChart.addSegment(s, segForm)
            }

            binding.pieChart.redraw()
            binding.pieChart.getRenderer(PieRenderer::class.java).setDonutSize(0f, PieRenderer.DonutMode.PERCENT)
            binding.pieChart.visibility = View.VISIBLE
            binding.progressBar.visibility = View.GONE
        }
    }

    private suspend fun tryDrawLineGraphFeaturesAndCacheTimeRange(lineGraph: LineGraph): Boolean {
        var minDateTime: OffsetDateTime? = null
        var maxDateTime: OffsetDateTime? = null
        lineGraph.features.forEach {
            val timeRange = drawLineGraphFeature(lineGraph, it)
            minDateTime = listOf(minDateTime, timeRange.minDateTime).minBy { dt ->
                if (dt == null) return@minBy OffsetDateTime.MAX
                else return@minBy dt!!
            }
            maxDateTime = listOf(maxDateTime, timeRange.maxDateTime).maxBy { dt ->
                if (dt == null) return@maxBy OffsetDateTime.MIN
                else return@maxBy dt!!
            }
            yield()
        }
        if (minDateTime == null || maxDateTime == null) return false
        currentTimeRange.minDateTime = minDateTime!!
        currentTimeRange.maxDateTime = maxDateTime!!
        return true
    }

    private fun setUpLineGraphXAxis() {
        val duration = Duration.between(currentTimeRange.minDateTime, currentTimeRange.maxDateTime)
        val formatter = getDateTimeFormatForDuration(duration)
        val timeDiff = duration.toMinutes().toDouble()
        binding.lineGraph.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format = object : Format() {
            override fun format(obj: Any, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
                val ratio = (obj as Number).toDouble()
                val timeStamp = currentTimeRange.minDateTime.plusMinutes((ratio * timeDiff).roundToLong())
                return toAppendTo.append(formatter.format(timeStamp))
            }
            override fun parseObject(source: String, pos: ParsePosition) = null
        }
    }

    private fun getDateTimeFormatForDuration(duration: Duration) = when {
        duration.toDays() >= 304 -> lineGraphMonthsDateFormat
        duration.toDays() >= 1 -> lineGraphDaysDateFormat
        else -> lineGraphHoursDateFormat
    }

    private class RawDataSample(val dataPoints: List<DataPoint>, val plotFrom: Int)

    private suspend fun drawLineGraphFeature(lineGraph: LineGraph, lineGraphFeature: LineGraphFeature): NullableTimeRange {
        inflateGraphLegendItem(lineGraphFeature.colorId, lineGraphFeature.name)
        val movingAvDuration = movingAverageDurations[lineGraphFeature.averagingMode]
        val plottingPeriod = plottingModePeriods[lineGraphFeature.plottingMode]
        val rawDataSample = sampleData(lineGraphFeature.featureId, lineGraph.duration, movingAvDuration, plottingPeriod)
        val plottingData = withContext(Dispatchers.IO) {
            when (lineGraphFeature.plottingMode) {
                LineGraphPlottingModes.WHEN_TRACKED -> rawDataSample
                else -> calculateDurationAccumulatedValues(rawDataSample, plottingPeriod!!)
            }
        }
        return if (dataPlottable(plottingData)) {
            createAndAddSeries(plottingData, lineGraphFeature)
        } else NullableTimeRange(null, null)
    }

    private fun inflateGraphLegendItem(colorId: Int, label: String) {
        binding.legendFlexboxLayout.addView(GraphLegendItemView(context, colorId, label))
    }

    private fun dataPlottable(rawData: RawDataSample): Boolean {
        return rawData.plotFrom >= 0
            && rawData.dataPoints.size - rawData.plotFrom > 1
            && rawData.dataPoints[rawData.plotFrom].timestamp.isBefore(rawData.dataPoints.last().timestamp)
    }

    private suspend fun sampleData(featureId: Long, sampleDuration: Duration?,
                                   averagingDuration: Duration?, plottingPeriod: Period?): RawDataSample {
        return withContext(Dispatchers.IO) {
            if (sampleDuration == null) RawDataSample(dataSource.getDataPointsForFeatureAscSync(featureId), 0)
            else {
                val now = OffsetDateTime.now()
                val startDate = now.minus(sampleDuration)
                val plottingDuration = plottingPeriod?.let { Duration.between(now, now.minus(plottingPeriod)) }
                val maxSampleDuration = listOf(
                    sampleDuration,
                    averagingDuration?.plus(sampleDuration),
                    plottingDuration
                ).maxBy { d -> d ?: Duration.ZERO }
                val minSampleDate = now.minus(maxSampleDuration)
                val dataPoints = dataSource.getDataPointsForFeatureBetweenAscSync(featureId, minSampleDate, now)
                val startIndex = dataPoints.indexOfFirst { dp -> dp.timestamp.isAfter(startDate) }
                RawDataSample(dataPoints, startIndex)
            }
        }
    }

    private suspend fun createAndAddSeries(rawData: RawDataSample, lineGraphFeature: LineGraphFeature): NullableTimeRange {
        val minX = rawData.dataPoints[rawData.plotFrom].timestamp
        val maxX = rawData.dataPoints.last().timestamp
        val series = getXYSeriesFromRawDataSample(rawData, lineGraphFeature, currentTimeRange)
        addSeries(series, lineGraphFeature)
        return NullableTimeRange(minX, maxX)
    }

    private fun addSeries(series: XYSeries, lineGraphFeature: LineGraphFeature) {
        val seriesFormat = LineAndPointFormatter(context, R.xml.line_point_formatter)
        seriesFormat.linePaint.color = getColor(context, lineGraphFeature.colorId)
        binding.lineGraph.addSeries(series, seriesFormat)
    }

    private suspend fun getXYSeriesFromRawDataSample(rawData: RawDataSample, lineGraphFeature: LineGraphFeature,
                                             timeRange: MutableTimeRange) = withContext(Dispatchers.IO) {
        val yValues = when (lineGraphFeature.averagingMode) {
            LineGraphAveraginModes.NO_AVERAGING -> rawData.dataPoints.drop(rawData.plotFrom).map { dp -> dp.value.toDouble() }
            else -> calculateMovingAverage(rawData, movingAverageDurations[lineGraphFeature.averagingMode]!!)
        }.map { v -> (v * lineGraphFeature.scale) + lineGraphFeature.offset }
        return@withContext object: FastXYSeries {
            private val rectRegion: RectRegion by lazy {
                var yRegion = SeriesUtils.minMax(yValues)
                if (abs(yRegion.min.toDouble() - yRegion.max.toDouble()) < 0.1)
                    yRegion = Region(yRegion.min, yRegion.min.toDouble() + 0.1)
                RectRegion(0, 1, yRegion.min, yRegion.max)
            }
            override fun minMax() = rectRegion
            override fun getX(index: Int): Number {
                //TODO this could be more efficient, we could let android plot worry about x values for us rather than always using
                //0 and 1 and calculating using expensive duration division
                return Duration.between(timeRange.minDateTime, rawData.dataPoints[rawData.plotFrom + index].timestamp)
                    .toMillis().toDouble() / timeRange.timeDiffMillis.toDouble()
            }
            override fun getY(index: Int): Number = yValues[index]
            override fun getTitle() = lineGraphFeature.name
            override fun size() = rawData.dataPoints.size - rawData.plotFrom
        }
    }

    private suspend fun calculateDurationAccumulatedValues(rawData: RawDataSample, period: Period): RawDataSample {
        var plotFrom = 0
        var foundPlotFrom = false
        val featureId = rawData.dataPoints[0].featureId
        val newData = mutableListOf<DataPoint>()
        var currentTimeStamp = findBeginningOfPeriod(rawData.dataPoints[0].timestamp, period)
        val now = OffsetDateTime.now()
        var index = 0
        while (currentTimeStamp.isBefore(now)) {
            currentTimeStamp = currentTimeStamp.with {ld -> ld.plus(period)}
            val points = rawData.dataPoints.drop(index).takeWhile { dp -> dp.timestamp.isBefore(currentTimeStamp) }
            val total = points.sumByDouble { dp -> dp.value.toDouble() }.toString()
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

