package com.samco.grapheasy.ui

import android.content.Context
import android.util.AttributeSet
import android.view.LayoutInflater
import android.view.View
import android.widget.FrameLayout
import androidx.core.content.ContextCompat.getColor
import com.androidplot.Plot
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

    private val lineGraphHoursDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("HH:mm")
        .withZone(ZoneId.systemDefault())
    private val lineGraphDaysDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("dd/MM")
        .withZone(ZoneId.systemDefault())
    private val lineGraphMonthsDateFormat: DateTimeFormatter = DateTimeFormatter
        .ofPattern("MM/YY")
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

    fun initInvalid() {
        resetJob()
        binding.invalidSetupLayout.visibility = View.VISIBLE
        binding.lineGraph.visibility = View.INVISIBLE
    }

    fun initFromLineGraph(graphOrStat: GraphOrStat, lineGraph: LineGraph) {
        resetJob()
        binding.lineGraph.clear()
        binding.invalidSetupLayout.visibility = View.INVISIBLE
        binding.lineGraph.visibility = View.VISIBLE
        viewScope!!.launch {
            val timeRange = drawLineGraphFeaturesAndCalculateTimeRange(lineGraph)
            setUpLineGraphXAxis(timeRange)
            binding.lineGraph.redraw()
        }
    }

    private class TimeRange(val minDateTime: OffsetDateTime?, val maxDateTime: OffsetDateTime?)

    private suspend fun drawLineGraphFeaturesAndCalculateTimeRange(lineGraph: LineGraph): TimeRange {
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
        return TimeRange(minDateTime, maxDateTime)
    }

    private fun setUpLineGraphXAxis(timeRange: TimeRange) {
        val minDateTime = timeRange.minDateTime ?: OffsetDateTime.now().minusDays(1)
        val maxDateTime = timeRange.maxDateTime ?: OffsetDateTime.now()

        val duration = Duration.between(minDateTime, maxDateTime)
        val formatter = getDateTimeFormatForDuration(duration)
        val timeDiff = duration.toMinutes().toDouble()
        binding.lineGraph.graph.getLineLabelStyle(XYGraphWidget.Edge.BOTTOM).format = object : Format() {
            override fun format(obj: Any, toAppendTo: StringBuffer, pos: FieldPosition): StringBuffer {
                val ratio = (obj as Number).toDouble()
                val timeStamp = minDateTime.plusMinutes(round(ratio * timeDiff).toLong())
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

    private suspend fun drawLineGraphFeature(lineGraph: LineGraph, lineGraphFeature: LineGraphFeature, feature: Feature): TimeRange {
        val rawDataSample = sampleData(feature, lineGraph.duration, movingAverageDurations[lineGraphFeature.mode])
        return if (!dataPlottable(rawDataSample)) {
            addSeries(getEmptyXYSeries(feature), lineGraphFeature)
            return TimeRange(null, null)
        } else createAndAddSeries(rawDataSample, feature, lineGraphFeature)
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
                val startDate = OffsetDateTime.now().minus(sampleDuration)
                val maxSampleDuration = averagingDuration?.plus(sampleDuration) ?: sampleDuration
                val minSampleDate = OffsetDateTime.now().minus(maxSampleDuration)
                val dataPoints = dataSource.getDataPointsForFeatureAfterAscSync(feature.id, minSampleDate)
                val startIndex = dataPoints.indexOfFirst { dp -> dp.timestamp.isAfter(startDate) }
                RawDataSample(dataPoints, startIndex)
            }
        }
    }

    private fun createAndAddSeries(rawData: RawDataSample, feature: Feature, lineGraphFeature: LineGraphFeature): TimeRange {
        val minX = rawData.dataPoints[rawData.plotFrom].timestamp
        val maxX = rawData.dataPoints.last().timestamp
        val timeDiff = Duration.between(minX, maxX).toHours().toDouble()
        val series = getXYSeriesFromRawDataSample(feature, rawData, lineGraphFeature, minX, timeDiff)
        addSeries(series, lineGraphFeature)
        return TimeRange(minX, maxX)
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

    private fun getXYSeriesFromRawDataSample(feature: Feature, rawData: RawDataSample, lineGraphFeature: LineGraphFeature,
                                             minX: OffsetDateTime, timeDiff: Double) = object: XYSeries {
        private val yValues = when (lineGraphFeature.mode) {
            LineGraphFeatureMode.TRACKED_VALUES -> {
                rawData.dataPoints
                    .drop(rawData.plotFrom)
                    .map { dp -> dp.value.toDouble() }
            }
            else -> calculateMovingAverage(rawData, movingAverageDurations[lineGraphFeature.mode]!!)
        }
        override fun getX(index: Int): Number {
            return Duration.between(minX, rawData.dataPoints[rawData.plotFrom + index].timestamp)
                .toHours().toDouble() / timeDiff
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

