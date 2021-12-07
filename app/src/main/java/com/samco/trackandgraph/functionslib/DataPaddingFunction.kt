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

package com.samco.trackandgraph.functionslib

import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.database.entity.DataType
import com.samco.trackandgraph.functionslib.exceptions.InvalidRegularityException
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.TemporalAmount

/**
 * Requires regular input. Makes sure that the output sample at least contains data up to
 * the end time and down to the start time. No clipping is performed here, so if the input sample is
 * larger than the given time range then the input sample will not be modified.
 */
// TODO write tests around this
class DataPaddingFunction : DataSampleFunction {
    private val timeHelper: TimeHelper
    private val endTime: OffsetDateTime
    private val startTime: OffsetDateTime
    private val defaultValue: Double
    private val defaultDataType: DataType
    private val defaultLabel: String

    constructor(
        timeHelper: TimeHelper,
        endTime: OffsetDateTime?,
        duration: TemporalAmount?,
        defaultValue: Double = 0.0,
        defaultLabel: String = "",
        defaultDataType: DataType = DataType.CONTINUOUS
    ) {
        this.timeHelper = timeHelper
        this.endTime = endTime ?: OffsetDateTime.now()
        this.startTime = duration?.let { this.endTime.minus(it) } ?: this.endTime
        this.defaultValue = defaultValue
        this.defaultLabel = defaultLabel
        this.defaultDataType = defaultDataType
    }

    constructor(
        timeHelper: TimeHelper,
        endTime: OffsetDateTime?,
        startTime: OffsetDateTime?,
        defaultValue: Double = 0.0,
        defaultLabel: String = "",
        defaultDataType: DataType = DataType.CONTINUOUS
    ) {
        this.timeHelper = timeHelper
        this.endTime = endTime ?: OffsetDateTime.now()
        this.startTime = startTime ?: OffsetDateTime.now()
        this.defaultValue = defaultValue
        this.defaultLabel = defaultLabel
        this.defaultDataType = defaultDataType
    }

    override suspend fun mapSample(dataSample: DataSample): DataSample {
        if (dataSample.dataSampleProperties.regularity == null) throw InvalidRegularityException()
        return DataSample.fromSequence(
            getSequence(dataSample, dataSample.dataSampleProperties.regularity),
            dataSample.dataSampleProperties,
            dataSample::getRawDataPoints
        )
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
        val beginning = timeHelper.toZonedDateTime(startTime)
        while (current > beginning) {
            yield(createDataPoint(current.toOffsetDateTime()))
            current = current.minus(period)
        }
    }

    //TODO test full generate empty data
    //TODO test no end time
    //TODO test no duration time
    //TODO test end time and duration
    //TODO test no end time or duration
    //TODO test points fall on boundaries
    //TODO test no duplicate points when padding data start or end

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
        override val dataType: DataType = defaultDataType
        override val value: Double = defaultValue
        override val label: String = defaultLabel
        override val note: String = ""
    }
}