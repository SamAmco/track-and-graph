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

import com.samco.trackandgraph.antlr.evaluation.AggregationEnum
import com.samco.trackandgraph.database.entity.AggregatedDataPoint
import com.samco.trackandgraph.database.entity.DataPointInterface
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.functionslib.DataSample

/**
 * This class represents the initial points clustered into the parent-attribute of the AggregatedDataPoints.
 * To obtain usable values, one of the follow-up functions like sum, average, or max need to be called.
 * Note that some follow-up functions drop data-points without parents. This is supposed to be intuitive :)
 */
internal class RawAggregatedDatapoints(private val points: List<AggregatedDataPoint>, val featureType: FeatureType) {
    private fun makeSample(datapoints: List<DataPointInterface>) = DataSample(datapoints, featureType)

    fun sum() = makeSample(
        this.points.map { it.copy(value = it.parents.sumOf { par -> par.value }) }
    )


    // When using duration based aggregated values, some points can have no parents.
    // The most intuitive solution is probably to just remove these aggregated points
    // There might be cases where it can make sense to define a fallback value, e.g. 0
    fun max() = makeSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.maxOf { par -> par.value }) }
    )

    fun min() = makeSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.minOf { par -> par.value }) }
    )

    fun average() = makeSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.map { par -> par.value }.average()) }
    )

    fun median() : DataSample{
        fun median_internal(list: List<Double>): Double {
            val sorted = list.sortedBy { it }
            return when (sorted.size % 2) {
                1 -> return sorted[sorted.size.div(2)]
                0 -> return sorted.subList(sorted.size.div(2)-1, sorted.size.div(2)+1).average() // sublist 2nd arg is exclusive
                else -> throw Exception("unreachable. positive int modulo 2 is always 1 or 0")
            }
        }
        return makeSample(
            this.points
                .filter { it.parents.isNotEmpty() }
                .map { it.copy(value =
                median_internal(it.parents.map { par -> par.value })) }
        )
    }

    fun earliest() = makeSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.sortedBy { par -> par.timestamp }.first().value) }
    )

    fun latest() = makeSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.sortedBy { par -> par.timestamp }.last().value) }
    )

    fun count() = makeSample(
        this.points
            .map { it.copy(value = it.parents.size.toDouble()) }
    )

    operator fun invoke(aggregationFunction: AggregationEnum) : DataSample {
        return when (aggregationFunction) {
            AggregationEnum.SUM -> this.sum()
            AggregationEnum.AVERAGE -> this.average()
            AggregationEnum.MAX -> this.max()
            AggregationEnum.MIN -> this.min()
            AggregationEnum.MEDIAN -> this.median()
            AggregationEnum.EARLIEST -> this.earliest()
            AggregationEnum.LATEST -> this.latest()
            AggregationEnum.COUNT -> this.count()
        }
    }

}