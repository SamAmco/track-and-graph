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

package com.samco.trackandgraph.functionslib.aggregation

import com.samco.trackandgraph.database.entity.AggregatedDataPoint
import com.samco.trackandgraph.database.entity.IDataPoint
import com.samco.trackandgraph.functionslib.DataSample
import com.samco.trackandgraph.functionslib.TimeHelper
import kotlinx.coroutines.yield
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.TemporalAmount

/**
 * Aggregate all data points into binSize length bins. For example if the bin size is 1 day and the
 * sample data contains 3 data points {1, 3, 7} all tracked on the same day then the function will
 * return a data sample with all three data-points as its parent.
 *
 * The currently supported binSize values are: Duration.ofHours(1), Period.ofDays(1),
 * Period.ofWeeks(1), Period.ofMonths(1), Period.ofYears(1)
 *
 * If sampleDuration is provided then totals will be generated at least as far back as now minus the
 * sampleDuration. However if there is more data before this period then it that data will also be
 * totalled. For clipping see clipDataSample.
 *
 * If end time is provided then totals will be generated at least up to this time. However if there
 * is more data after the end time in the input sample then that data will also be totalled. For
 * clipping see clipDataSample.
 *
 * sampleData.dataPoints should be sorted from oldest timestamp to newest timestamp
 */

internal class FixedBinAggregator(
    private val timeHelper: TimeHelper,
    private val featureId: Long,
    private val sampleDuration: Duration?,
    private val endTime: OffsetDateTime?,
    private val binSize: TemporalAmount,
) : DataAggregator {

    override suspend fun aggregate(dataSample: DataSample): AggregatedDataSample {
        return AggregatedDataSample.fromSequence(
            getSequence(dataSample),
            dataSample.dataSampleProperties
        )
    }

    override suspend fun aggregate(dataSample: AggregatedDataSample): AggregatedDataSample {
        return AggregatedDataSample.fromSequence(
            getSequence(dataSample),
            dataSample.dataSampleProperties
        )
    }

    private fun getSequence(dataSample: Sequence<IDataPoint>) = sequence {
        //TODO would be really nice if there was an implementation that didn't require
        // copying the whole sequence to memory

        //Get the points in order from oldest to newest
        val dataPoints = dataSample.toList().asReversed()

        val firstDataPointTime = dataPoints.firstOrNull()?.cutoffTimestampForAggregation()
        val lastDataPointTime = dataPoints.lastOrNull()?.cutoffTimestampForAggregation()

        val latest = getEndTimeNowOrLatest(lastDataPointTime, endTime)
        val earliest = getStartTimeOrFirst(firstDataPointTime, latest, endTime, sampleDuration)
        var currentTimeStamp = timeHelper
            .findBeginningOfTemporal(earliest, binSize)
            .minusNanos(1)
        var index = 0
        while (currentTimeStamp.isBefore(latest)) {
            currentTimeStamp = currentTimeStamp.with { ld -> ld.plus(binSize) }
            val points = dataPoints
                .drop(index)
                .takeWhile { dp ->
                    dp.cutoffTimestampForAggregation().isBefore(currentTimeStamp)
                }
            index += points.size
            yield(
                AggregatedDataPoint(
                    timestamp = currentTimeStamp,
                    value = Double.NaN, // this value gets overwritten when calling a function on RawAggregatedDatapoints
                    featureId = featureId,
                    parents = points
                )
            )
        }
    }

    private fun getEndTimeNowOrLatest(
        lastDataPointTime: OffsetDateTime?,
        endTime: OffsetDateTime?
    ): OffsetDateTime {
        val now = OffsetDateTime.now()
        return when (endTime) {
            null -> listOf(lastDataPointTime, now).maxByOrNull { t -> t ?: OffsetDateTime.MIN }
            else -> listOf(lastDataPointTime, endTime).maxByOrNull { t -> t ?: OffsetDateTime.MIN }
        } ?: now
    }

    private fun getStartTimeOrFirst(
        firstDataPointTime: OffsetDateTime?,
        latest: OffsetDateTime,
        endTime: OffsetDateTime?,
        sampleDuration: Duration?
    ): OffsetDateTime {
        val beginningOfDuration = sampleDuration?.let { endTime?.minus(it) }
        val durationBeforeLatest = sampleDuration?.let { latest.minus(it) }
        return listOf(
            firstDataPointTime,
            beginningOfDuration,
            durationBeforeLatest,
            latest
        ).minByOrNull { t -> t ?: OffsetDateTime.MAX } ?: latest
    }

    private fun IDataPoint.cutoffTimestampForAggregation(): OffsetDateTime {
        return timestamp - timeHelper.aggregationPreferences.startTimeOfDay
    }
}