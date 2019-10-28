package com.samco.grapheasy.ui

import android.content.Context
import android.graphics.Color
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getColor
import com.androidplot.Plot
import com.androidplot.pie.PieRenderer
import com.androidplot.pie.Segment
import com.androidplot.pie.SegmentFormatter
import com.androidplot.ui.HorizontalPositioning
import com.androidplot.ui.Size
import com.androidplot.ui.VerticalPositioning
import com.androidplot.xy.LineAndPointFormatter
import com.androidplot.xy.XYGraphWidget
import com.androidplot.xy.XYSeries
import com.samco.grapheasy.databinding.GraphStatViewBinding
import kotlinx.coroutines.*
import com.samco.grapheasy.R
import com.samco.grapheasy.database.*
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.format.DateTimeFormatter
import timber.log.Timber
import java.text.FieldPosition
import java.text.Format
import java.text.ParsePosition
import kotlin.math.round

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
        initError(R.string.graph_stat_view_invalid_setup)
    }

    private fun initError(errorTextId: Int) {
        cleanAllViews()
        binding.errorMessage.visibility = View.VISIBLE
        binding.errorMessage.text = context.getString(errorTextId)
    }

    private fun initHeader(graphOrStat: GraphOrStat) {
        binding.headerText.text = graphOrStat.name
    }

    fun initTimeSinceStat(graphOrStat: GraphOrStat, timeSinceLastStat: TimeSinceLastStat) {
        resetJob()
        cleanAllViews()
        initHeader(graphOrStat)
        binding.statMessage.visibility = View.VISIBLE
        viewScope!!.launch {
            val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(timeSinceLastStat.featureId) }
            var lastDataPointTimeStamp: OffsetDateTime? = null
            if (feature.featureType == FeatureType.CONTINUOUS) {
                val lastDataPoint = withContext(Dispatchers.IO) {
                    dataSource.getLastDataPointBetween(feature.id, timeSinceLastStat.fromValue, timeSinceLastStat.toValue)
                }
                if (lastDataPoint != null) lastDataPointTimeStamp = lastDataPoint.timestamp
            } else {
                val lastDataPoint = withContext(Dispatchers.IO) {
                    dataSource.getLastDataPointWithValue(feature.id, timeSinceLastStat.fromValue)
                }
                if (lastDataPoint != null) lastDataPointTimeStamp = lastDataPoint.timestamp
            }
            if (lastDataPointTimeStamp == null) { initError(R.string.graph_stat_view_not_enough_data_stat) }
            else {
                while (true) {
                    setTimeSinceStatText(lastDataPointTimeStamp)
                    delay(1000)
                    yield()
                }
            }
        }
    }

    private fun setTimeSinceStatText(timeStamp: OffsetDateTime) {
        val totalSeconds = Duration.between(timeStamp, OffsetDateTime.now()).toMillis() / 1000.toDouble()
        val daysNum = (totalSeconds / 86400).toInt()
        val days = daysNum.toString()
        val hours = "%02d".format(((totalSeconds % 86400) / 3600).toInt())
        val minutes = "%02d".format(((totalSeconds % 3600) / 60).toInt())
        val seconds = "%02d".format((totalSeconds % 60).toInt())
        binding.statMessage.text =
            if (daysNum > 0) "$days ${context.getString(R.string.days)}"
            else "$hours:$minutes:$seconds"
    }

    fun initFromLineGraph(graphOrStat: GraphOrStat, lineGraph: LineGraph) {
        resetJob()
        cleanAllViews()
        initHeader(graphOrStat)
        binding.lineGraph.visibility = View.VISIBLE
        viewScope!!.launch {
            drawLineGraphFeaturesAndCalculateTimeRange(lineGraph)
            setUpLineGraphXAxis()
            binding.lineGraph.redraw()
        }
    }

    fun initFromPieChart(graphOrStat: GraphOrStat, pieChart: PieChart) {
        resetJob()
        cleanAllViews()
        initHeader(graphOrStat)
        binding.pieChart.visibility = View.VISIBLE
        viewScope!!.launch {
            val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(pieChart.featureId) }
            val dataSample = sampleData(feature, pieChart.duration, null)
            if (!dataPlottable(dataSample)) {
                initError(R.string.graph_stat_view_not_enough_data_graph)
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
        }
    }

    private suspend fun drawLineGraphFeaturesAndCalculateTimeRange(lineGraph: LineGraph) {
        var minDateTime: OffsetDateTime? = null
        var maxDateTime: OffsetDateTime? = null
        lineGraph.features.forEach {
            val feature = withContext(Dispatchers.IO) { dataSource.getFeatureById(it.featureId) }
            val timeRange = drawLineGraphFeature(lineGraph, it, feature)
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
        currentTimeRange.minDateTime = minDateTime ?: OffsetDateTime.now().minusDays(1)
        currentTimeRange.maxDateTime = maxDateTime ?: OffsetDateTime.now()
    }

    private fun setUpLineGraphXAxis() {
        val duration = Duration.between(currentTimeRange.minDateTime, currentTimeRange.maxDateTime)
        val formatter = getDateTimeFormatForDuration(duration)
        val timeDiff = duration.toMinutes().toDouble()
        binding.lineGraph.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format = object : Format() {
            override fun format(obj: Any, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
                val ratio = (obj as Number).toDouble()
                val timeStamp = currentTimeRange.minDateTime.plusMinutes(round(ratio * timeDiff).toLong())
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

    private suspend fun drawLineGraphFeature(lineGraph: LineGraph, lineGraphFeature: LineGraphFeature, feature: Feature): NullableTimeRange {
        inflateGraphLegendItem(lineGraphFeature.colorId, feature.name)
        val rawDataSample = sampleData(feature, lineGraph.duration, movingAverageDurations[lineGraphFeature.mode])
        return if (!dataPlottable(rawDataSample)) {
            addSeries(getEmptyXYSeries(feature), lineGraphFeature)
            return NullableTimeRange(null, null)
        } else createAndAddSeries(rawDataSample, feature, lineGraphFeature)
    }

    private fun inflateGraphLegendItem(colorId: Int, label: String) {
        binding.legendFlexboxLayout.addView(GraphLegendItemView(context, colorId, label))
    }

    private fun dataPlottable(rawData: RawDataSample): Boolean {
        return rawData.dataPoints.size > 2
                && rawData.plotFrom >= 0
                && rawData.dataPoints[rawData.plotFrom].timestamp.isBefore(rawData.dataPoints.last().timestamp)
    }

    private suspend fun sampleData(feature: Feature, sampleDuration: Duration?, averagingDuration: Duration?): RawDataSample {
        return withContext(Dispatchers.IO) {
            if (sampleDuration == null) RawDataSample(dataSource.getDataPointsForFeatureAscSync(feature.id), 0)
            else {
                val now = OffsetDateTime.now()
                val startDate = now.minus(sampleDuration)
                val maxSampleDuration = averagingDuration?.plus(sampleDuration) ?: sampleDuration
                val minSampleDate = now.minus(maxSampleDuration)
                val dataPoints = dataSource.getDataPointsForFeatureAfterAscSync(feature.id, minSampleDate, now)
                val startIndex = dataPoints.indexOfFirst { dp -> dp.timestamp.isAfter(startDate) }
                RawDataSample(dataPoints, startIndex)
            }
        }
    }

    private fun createAndAddSeries(rawData: RawDataSample, feature: Feature, lineGraphFeature: LineGraphFeature): NullableTimeRange {
        val minX = rawData.dataPoints[rawData.plotFrom].timestamp
        val maxX = rawData.dataPoints.last().timestamp
        val series = getXYSeriesFromRawDataSample(feature, rawData, lineGraphFeature, currentTimeRange)
        addSeries(series, lineGraphFeature)
        return NullableTimeRange(minX, maxX)
    }

    private fun addSeries(series: XYSeries, lineGraphFeature: LineGraphFeature) {
        val seriesFormat = LineAndPointFormatter(context, R.xml.line_point_formatter)
        seriesFormat.linePaint.color = getColor(context, lineGraphFeature.colorId)
        binding.lineGraph.addSeries(series, seriesFormat)
    }

    private fun getEmptyXYSeries(feature: Feature) = object: XYSeries {
        override fun getX(index: Int) = 0
        override fun getY(index: Int) = 0
        override fun getTitle() = feature.name
        override fun size() = 0
    }

    private fun getXYSeriesFromRawDataSample(feature: Feature, rawData: RawDataSample,
                                             lineGraphFeature: LineGraphFeature,
                                             timeRange: MutableTimeRange) = object: XYSeries {
        private val yValues = when (lineGraphFeature.mode) {
            LineGraphFeatureMode.TRACKED_VALUES -> {
                rawData.dataPoints
                    .drop(rawData.plotFrom)
                    .map { dp -> dp.value.toDouble() }
            }
            else -> {
                val values = calculateMovingAverage(rawData, movingAverageDurations[lineGraphFeature.mode]!!)
                values
            }
        }
        override fun getX(index: Int): Number {
            return Duration.between(timeRange.minDateTime, rawData.dataPoints[rawData.plotFrom + index].timestamp)
                .toMillis().toDouble() / timeRange.timeDiffMillis.toDouble()
        }
        override fun getY(index: Int): Number = (yValues[index] * lineGraphFeature.scale) + lineGraphFeature.offset
        override fun getTitle() = feature.name
        override fun size() = rawData.dataPoints.size - rawData.plotFrom
    }

    private fun calculateMovingAverage(rawData: RawDataSample, movingAvDuration: Duration): List<Double> {
        return rawData.dataPoints
            .drop(rawData.plotFrom)
            .mapIndexed { index, dataPoint ->
                val inRange = mutableListOf(dataPoint)
                var i = rawData.plotFrom+index-1
                while (i > 0 && Duration.between(rawData.dataPoints[i].timestamp, dataPoint.timestamp) <= movingAvDuration) {
                    inRange.add(rawData.dataPoints[i])
                    i--
                }
                inRange.sumByDouble { dp -> dp.value.toDouble() } / inRange.size.toDouble()
            }
    }

    fun dispose() { currJob?.cancel() }
}

