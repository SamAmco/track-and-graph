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
package com.samco.trackandgraph.graphstatview.factories

import com.androidplot.xy.RectRegion
import com.androidplot.xy.SimpleXYSeries
import com.androidplot.xy.StepMode
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.BarChart
import com.samco.trackandgraph.base.database.dto.BarChartBarPeriod
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.YRangeType
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import com.samco.trackandgraph.graphstatview.GraphStatInitException
import com.samco.trackandgraph.graphstatview.factories.viewdto.IBarChartData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.withContext
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount
import timber.log.Timber
import javax.inject.Inject

class BarChartDataFactory @Inject constructor(
    dataInteractor: DataInteractor,
    @IODispatcher ioDispatcher: CoroutineDispatcher
) : ViewDataFactory<BarChart, IBarChartData>(dataInteractor, ioDispatcher) {

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IBarChartData = dataInteractor.getBarChartByGraphStatId(graphOrStat.id)?.let {
        createViewData(graphOrStat, it, onDataSampled)
    } ?: object : IBarChartData {
        override val state = IGraphStatViewData.State.ERROR
        override val error = GraphStatInitException(R.string.graph_stat_view_not_found)
        override val graphOrStat = graphOrStat
    }

    private fun BarChartBarPeriod.asTemporalAmount(): TemporalAmount = when (this) {
        BarChartBarPeriod.HOUR -> Duration.ofHours(1)
        BarChartBarPeriod.DAY -> Period.ofDays(1)
        BarChartBarPeriod.WEEK -> Period.ofWeeks(1)
        BarChartBarPeriod.MONTH -> Period.ofMonths(1)
        BarChartBarPeriod.THREE_MONTHS -> Period.ofMonths(3)
        BarChartBarPeriod.SIX_MONTHS -> Period.ofMonths(6)
        BarChartBarPeriod.YEAR -> Period.ofYears(1)
    }

    private data class BarData(
        val bars: List<SimpleXYSeries>,
        val dates: List<OffsetDateTime>,
        val bounds: RectRegion
    )

    private fun getBarData(
        dataSample: DataSample,
        endTime: OffsetDateTime,
        barSize: TemporalAmount,
        duration: Duration?
    ): BarData {
        val barDates = mutableListOf<OffsetDateTime>()
        val barValuesByLabel = mutableMapOf<String, MutableList<Double>>()
        val iterator = dataSample.iterator()

        barDates.add(endTime)

        val endTimeMinusDuration = duration?.let { endTime.minus(it) }

        var currentBarStartTime = endTime.minus(barSize)

        while (iterator.hasNext()) {
            //Grab the next data point to be placed
            val next = iterator.next()

            //If the next data point is before the start of the duration we are interested in, we
            // can stop
            if (duration != null && next.timestamp.isBefore(endTimeMinusDuration)) break

            while (next.timestamp.isBefore(currentBarStartTime)) {
                //we have reached the end of the current bar, so we need to move to the next one
                currentBarStartTime = currentBarStartTime.minus(barSize)
                barDates.add(currentBarStartTime)
            }

            //Get the list of values for the label of the next data point. If there isn't one, create
            // a new one and add it to barValuesByLabel
            val values = barValuesByLabel.getOrPut(next.label) { mutableListOf() }

            //If the list is not the same size as barDates, we need to add some zeros to the end
            // of all the lists until they're all the same size as barDates
            if (values.size < barDates.size) barValuesByLabel.values.forEach {
                it.addAll(List(barDates.size - it.size) { 0.0 })
            }

            //Add the value to the double in the list
            values[values.size - 1] += next.value
        }

        val bars = barValuesByLabel.map { (label, values) ->
            SimpleXYSeries(values.asReversed(), SimpleXYSeries.ArrayFormat.Y_VALS_ONLY, label)
        }

        val dates = barDates.asReversed()

        //The values are essentially a grid and we want the largest column sum
        val maxY = (0..(barValuesByLabel.values.firstOrNull()?.size ?: 0))
            .maxOfOrNull { index -> barValuesByLabel.values.sumOf { it[index] } } ?: 0.0

        val bounds = RectRegion(
            minX = 0.0 as Number,
            maxX = (bars.size - 1) as Number,
            minY = 0.0 as Number,
            maxY = maxY as Number
        )

        return BarData(bars, dates, bounds)
    }

    private data class BarDataWithYAxisParams(
        val bars: List<SimpleXYSeries>,
        val dates: List<OffsetDateTime>,
        val bounds: RectRegion,
        val yAxisParameters: Pair<StepMode, Double>,
    )

    private fun getBarDataWithYAxisParams(
        dataSample: DataSample,
        endTime: OffsetDateTime,
        barSize: TemporalAmount,
        duration: Duration?,
        isDuration: Boolean,
        yRangeType: YRangeType
    ): BarDataWithYAxisParams {
        val barData = getBarData(dataSample, endTime, barSize, duration)

        val yAxisParameters = DataDisplayIntervalHelper().getYParameters(
            barData.bounds.minY.toDouble(),
            barData.bounds.maxY.toDouble(),
            isDuration,
            yRangeType == YRangeType.FIXED
        )

        val yAxisParameterPair = Pair(yAxisParameters.step_mode, yAxisParameters.n_intervals)

        return BarDataWithYAxisParams(
            bars = barData.bars,
            dates = barData.dates,
            bounds = barData.bounds,
            yAxisParameters = yAxisParameterPair
        )
    }

    override suspend fun createViewData(
        graphOrStat: GraphOrStat,
        config: BarChart,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IBarChartData {
        try {
            val dataSample = dataInteractor.getDataSampleForFeatureId(config.featureId)
            val endTime = config.endDate ?: OffsetDateTime.now()

            val barData = withContext(ioDispatcher) {
                getBarDataWithYAxisParams(
                    dataSample = dataSample,
                    endTime = endTime,
                    barSize = config.barPeriod.asTemporalAmount(),
                    duration = config.duration,
                    isDuration = dataSample.dataSampleProperties.isDuration,
                    yRangeType = config.yRangeType
                )
            }

            return object : IBarChartData {
                override val xDates = barData.dates
                override val bars = barData.bars
                override val durationBasedRange = dataSample.dataSampleProperties.isDuration
                override val endTime = endTime
                override val yRangeType = config.yRangeType
                override val bounds = barData.bounds
                override val yAxisRangeParameters = barData.yAxisParameters
                override val state = IGraphStatViewData.State.READY
                override val graphOrStat = graphOrStat
            }
        } catch (t: Throwable) {
            Timber.d(t, "Error creating bar chart data")
            return object : IBarChartData {
                override val state = IGraphStatViewData.State.ERROR
                override val error = GraphStatInitException(R.string.graph_stat_validation_unknown)
                override val graphOrStat = graphOrStat
            }
        }
    }
}