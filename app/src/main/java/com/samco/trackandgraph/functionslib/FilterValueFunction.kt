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

import com.samco.trackandgraph.database.entity.DataType

/**
 * A function that will filter all data points in the given input sample and return only those that
 * match the given constraints. Any data point that is marked as discrete will be checked against the
 * given discreteValues. Otherwise the data point will be checked to see if it is greater than
 * or equal to the fromValue and smaller than or equal to the toValue.
 */
class FilterValueFunction(
    val fromValue: Double,
    val toValue: Double,
    val discreteValues: List<Int>
) : DataSampleFunction {
    override suspend fun mapSample(dataSample: DataSample): DataSample {
        return DataSample.fromSequence(
            dataSample.filter {
                if (it.dataType == DataType.DISCRETE) it.value.toInt() in discreteValues
                else it.value in fromValue..toValue
            },
            dataSample.dataSampleProperties,
            dataSample::getRawDataPoints
        )
    }
}