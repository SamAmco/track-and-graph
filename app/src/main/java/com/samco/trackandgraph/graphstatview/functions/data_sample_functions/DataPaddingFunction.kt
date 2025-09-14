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

package com.samco.trackandgraph.graphstatview.functions.data_sample_functions

import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.sampling.DataSample
import com.samco.trackandgraph.graphstatview.functions.exceptions.InvalidRegularityException
import com.samco.trackandgraph.graphstatview.functions.helpers.TimeHelper
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAmount

/**
 * Makes sure that the output sample at least contains data up to
 * the end time and down to the start time. No clipping is performed here, so if the input sample is
 * larger than the given time range then the input sample will not be modified. If there is no data
 * in the input sample then empty points will be created for the entire range. Default values for
 * those data points can be optionally provided.
 *
 * The timestamp of each generated data point is provided by [TimeHelper.findEndOfTemporal] with
 * the temporal amount value given as the [DataSampleProperties.regularity]
 *
 * Requires regular input.
 */
class DataPaddingFunction : DataSampleFunction {
    private val timeHelper: TimeHelper
    private val endTime: ZonedDateTime
    private val startTime: ZonedDateTime
    private val defaultValue: Double
    private val defaultLabel: String

    constructor(
        timeHelper: TimeHelper,
        endTime: OffsetDateTime?,
        duration: TemporalAmount?,
        defaultValue: Double = 0.0,
        defaultLabel: String = ""
    ) {
        this.timeHelper = timeHelper
        this.endTime = endTime?.let { timeHelper.toZonedDateTime(it) } ?: ZonedDateTime.now()
        this.startTime = duration?.let { this.endTime.minus(it) } ?: this.endTime
        this.defaultValue = defaultValue
        this.defaultLabel = defaultLabel
    }

    constructor(
        timeHelper: TimeHelper,
        endTime: ZonedDateTime?,
        startTime: ZonedDateTime?,
        defaultValue: Double = 0.0,
        defaultLabel: String = ""
    ) {
        this.timeHelper = timeHelper
        this.endTime = endTime ?: ZonedDateTime.now()
        this.startTime = startTime ?: ZonedDateTime.now()
        this.defaultValue = defaultValue
        this.defaultLabel = defaultLabel
    }

    /**
     * @throws InvalidRegularityException if the data sample provided has a null regularity
     */
    override suspend fun mapSample(dataSample: DataSample): DataSample {
        return dataSample.dataSampleProperties.regularity?.let { regularity ->
            DataSample.fromSequence(
                getSequence(dataSample, regularity),
                dataSample.dataSampleProperties,
                dataSample::getRawDataPoints,
                dataSample::dispose
            )
        } ?: throw InvalidRegularityException()
    }

    private fun getSequence(dataSample: DataSample, period: TemporalAmount) = sequence {
        val first = dataSample.firstOrNull()?.let { timeHelper.toZonedDateTime(it.timestamp) }
        if (first == null) {
            yieldAll(fullRange(period))
            return@sequence
        }
        var current = timeHelper.findEndOfTemporal(endTime, period).minus(period)

        while (current > first) {
            yield(createDataPoint(current.toOffsetDateTime()))
            current = current.minus(period)
        }
        val iterator = dataSample.iterator()
        while (iterator.hasNext()) {
            val next = iterator.next()
            yield(next)
            if (!iterator.hasNext()) {
                current = timeHelper.toZonedDateTime(next.timestamp)
                    .minus(period)
            }
        }
        while (current > startTime) {
            yield(createDataPoint(current.toOffsetDateTime()))
            current = current.minus(period)
        }
    }

    private fun fullRange(period: TemporalAmount) = sequence {
        var current = timeHelper.findBeginningOfTemporal(endTime, period)
        val start = timeHelper.findEndOfTemporal(startTime, period)
        while (current >= start) {
            yield(createDataPoint(current.toOffsetDateTime()))
            current = current.minus(period)
        }
    }

    private fun createDataPoint(timestamp: OffsetDateTime): IDataPoint = object : IDataPoint() {
        override val timestamp = timestamp
        override val value: Double = defaultValue
        override val label: String = defaultLabel
    }
}