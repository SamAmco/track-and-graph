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
internal class RawAggregatedDatapoints(
    private val points: List<AggregatedDataPoint>,
    val featureType: FeatureType
) {
    private fun makeSample(datapoints: List<DataPointInterface>) = DataSample(
        datapoints,
        featureType
    )

    private fun applyFun(
        aggFun: (List<DataPointInterface>) -> Double,
        fallback: Double?
    ): DataSample {
        return makeSample(
            this.points.mapNotNull {
                it.let {
                    val newValue = if (it.parents.isNotEmpty()) aggFun(it.parents)
                                    else fallback
                    if (newValue != null) it.copy(value = newValue)
                    else null
                }
            }
        )
    }

    fun sum() = applyFun({parents -> parents.sumOf { it.value }}, fallback = 0.0) // sum of nothing is 0


    // When using duration based aggregated values, some points can have no parents.
    // The most intuitive solution is probably to just remove these aggregated points
    // There might be cases where it can make sense to define a fallback value, e.g. 0
    fun max(fallback: Double?) = applyFun({parents -> parents.maxOf { it.value }}, fallback)

    fun min(fallback: Double?) = applyFun({parents -> parents.minOf { it.value }}, fallback)

    fun average(fallback: Double? = null) = applyFun({parents -> parents.map { it.value }.average()}, fallback)


    fun median(fallback: Double?): DataSample {
        fun median_internal(list: List<Double>): Double {
            val sorted = list.sortedBy { it }
            return when (sorted.size % 2) {
                1 -> return sorted[sorted.size.div(2)]
                0 -> return sorted.subList(sorted.size.div(2)-1, sorted.size.div(2)+1).average() // sublist 2nd arg is exclusive
                else -> throw Exception("unreachable. positive int modulo 2 is always 1 or 0")
            }
        }

        return applyFun({parents -> median_internal(parents.map { it.value })}, fallback)
    }

    fun earliest(fallback: Double?) = applyFun({parents -> parents.sortedBy { it.timestamp }.first().value}, fallback)

    fun latest(fallback: Double?) = applyFun({parents -> parents.sortedBy { it.timestamp }.last().value}, fallback)

    fun count() = applyFun({parents -> parents.size.toDouble()}, fallback = 0.0)

    operator fun invoke(aggregationFunction: AggregationEnum, fallback: Double? = null) : DataSample {
        return when (aggregationFunction) {
            AggregationEnum.SUM -> this.sum()
            AggregationEnum.AVERAGE -> this.average(fallback)
            AggregationEnum.MAX -> this.max(fallback)
            AggregationEnum.MIN -> this.min(fallback)
            AggregationEnum.MEDIAN -> this.median(fallback)
            AggregationEnum.EARLIEST -> this.earliest(fallback)
            AggregationEnum.LATEST -> this.latest(fallback)
            AggregationEnum.COUNT -> this.count()
        }
    }

}