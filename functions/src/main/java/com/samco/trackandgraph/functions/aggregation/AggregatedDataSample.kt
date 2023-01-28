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

package com.samco.trackandgraph.functions.aggregation

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.database.sampling.DataSampleProperties

/**
 * This class represents the initial points clustered into the parent-attribute of the AggregatedDataPoints.
 * To obtain usable values, one of the follow-up functions like sum, average, or max need to be called.
 * Note that some follow-up functions drop data-points without parents. This is supposed to be intuitive :)
 */
internal abstract class AggregatedDataSample(
    private val dataSample: DataSample,
    private val dataSampleProperties: DataSampleProperties
) : Sequence<AggregatedDataPoint> {
    companion object {
        fun fromDataSample(
            data: Sequence<AggregatedDataPoint>,
            dataSample: DataSample,
            dataSampleProperties: DataSampleProperties,
            getRawDataPoints: () -> List<DataPoint>
        ): AggregatedDataSample {
            return object : AggregatedDataSample(dataSample, dataSampleProperties) {
                override fun getRawDataPoints() = getRawDataPoints.invoke()
                override fun iterator(): Iterator<AggregatedDataPoint> = data.iterator()
            }
        }
    }

    abstract fun getRawDataPoints(): List<DataPoint>

    fun average() = DataSample.fromSequence(
        data = this
            .filter { it.parents.isNotEmpty() }
            .map { it.toDataPoint(it.parents.map { par -> par.value }.average()) },
        dataSampleProperties = dataSampleProperties,
        getRawDataPoints = this::getRawDataPoints,
        onDispose = dataSample::dispose
    )

    fun sum() = DataSample.fromSequence(
        data = this.map { it.toDataPoint(it.parents.sumOf { par -> par.value }) },
        dataSampleProperties = dataSampleProperties,
        getRawDataPoints = this::getRawDataPoints,
        onDispose = dataSample::dispose
    )

}