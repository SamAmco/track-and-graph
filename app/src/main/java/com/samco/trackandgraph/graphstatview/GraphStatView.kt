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
import com.androidplot.xy.*
import com.samco.trackandgraph.databinding.GraphStatViewBinding
import kotlinx.coroutines.*
import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.*
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import com.samco.trackandgraph.graphstatview.IDecoratableGraphStatView.RawDataSample
import com.samco.trackandgraph.ui.GraphLegendItemView

class GraphStatView : FrameLayout, IDecoratableGraphStatView {
    constructor(context: Context) : super(context, null)
    constructor(context: Context, attrSet: AttributeSet) : super(context, attrSet)

    private var binding = GraphStatViewBinding.inflate(LayoutInflater.from(context), this, true)
    override fun getBinding() = binding
    private var currJob: Job? = null
    private var viewScope: CoroutineScope? = null
    private val dataSource = TrackAndGraphDatabase.getInstance(context.applicationContext).trackAndGraphDatabaseDao
    private var currentDecorator: IGraphStatViewDecorator? = null

    init {
        basicLineGraphSetup()
        initError(null, R.string.graph_stat_view_invalid_setup)
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

    private fun trySetDecorator(graphOrStat: GraphOrStat, decorator: IGraphStatViewDecorator) {
        resetJob()
        viewScope?.launch {
            try {
                cleanAllViews()
                this@GraphStatView.currentDecorator = decorator
                currentDecorator?.decorate(this@GraphStatView)
            } catch (exception: Exception) {
                cleanAllViews()
                val initException = if (exception is GraphStatInitException) exception else null
                val errorTextId = initException?.errorTextId ?: R.string.graph_stat_view_unkown_error
                this@GraphStatView.currentDecorator = GraphStatErrorDecorator(graphOrStat, errorTextId)
                currentDecorator?.decorate(this@GraphStatView)
            }
        }
    }

    fun initError(graphOrStat: GraphOrStat?, errorTextId: Int) {
        resetJob()
        viewScope?.launch {
            cleanAllViews()
            currentDecorator = GraphStatErrorDecorator(graphOrStat, errorTextId)
            currentDecorator?.decorate(this@GraphStatView)
        }
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
        if (lastDataPointTimeStamp == null) { initError(graphOrStat, R.string.graph_stat_view_not_enough_data_stat) }
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
        if (dataPoints.size < 2) initError(graphOrStat, R.string.graph_stat_view_not_enough_data_stat)
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
        trySetDecorator(
            graphOrStat,
            GraphStatLineGraphDecorator(graphOrStat, lineGraph, listViewMode)
        )
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
            initError(graphOrStat, R.string.graph_stat_view_not_enough_data_graph)
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

    private fun inflateGraphLegendItem(colorIndex: Int, label: String) {
        val colorId = dataVisColorList[colorIndex]
        binding?.legendFlexboxLayout.addView(
            GraphLegendItemView(
                context!!,
                colorId,
                label
            )
        )
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

    private fun dataPlottable(rawData: RawDataSample, minDataPoints: Int = 1): Boolean {
        return rawData.plotFrom >= 0 && rawData.dataPoints.size - rawData.plotFrom >= minDataPoints
    }

    override suspend fun sampleData(featureId: Long, sampleDuration: Duration?,
                                   averagingDuration: Duration?, plottingPeriod: Period?): RawDataSample {
        return withContext(Dispatchers.IO) {
            if (sampleDuration == null) RawDataSample(
                dataSource.getDataPointsForFeatureAscSync(featureId),
                0
            )
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
                RawDataSample(
                    dataPoints,
                    startIndex
                )
            }
        }
    }

    private fun getLatestTimeOrNowForFeature(featureId: Long): OffsetDateTime {
        val lastDataPointList = dataSource.getLastDataPointForFeatureSync(featureId)
        val now = OffsetDateTime.now()
        val latest = lastDataPointList.firstOrNull()?.let {
            it.timestamp.plusSeconds(1)
        }
        return listOfNotNull(now, latest).max()!!
    }

    fun dispose() { currJob?.cancel() }
}


