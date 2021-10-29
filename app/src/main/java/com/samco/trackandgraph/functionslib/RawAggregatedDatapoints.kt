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

import com.samco.trackandgraph.database.entity.AggregatedDataPoint

/**
 * This class represents the initial points clustered into the parent-attribute of the AggregatedDataPoints.
 * To obtain usable values, one of the follow-up functions like sum, average, or max need to be called.
 * Note that some follow-up functions drop data-points without parents. This is supposed to be intuitive :)
 */
internal class RawAggregatedDatapoints(private val points: List<AggregatedDataPoint>) {
    fun sum() = DataSample(
        this.points.map { it.copy(value = it.parents.sumOf { par -> par.value }) }
    )


    // When using duration based aggregated values, some points can have no parents.
    // The most intuitive solution is probably to just remove these aggregated points
    // There might be cases where it can make sense to define a fallback value, e.g. 0
    fun max() = DataSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.maxOf { par -> par.value }) }
    )

    fun average() = DataSample(
        this.points
            .filter { it.parents.isNotEmpty() }
            .map { it.copy(value = it.parents.map { par -> par.value }.average()) }
    )
}
