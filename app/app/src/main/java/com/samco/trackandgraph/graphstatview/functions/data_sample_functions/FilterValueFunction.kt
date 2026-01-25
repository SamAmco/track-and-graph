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

import com.samco.trackandgraph.data.sampling.DataSample

/**
 * A function that will filter all data points in the given input sample and return only those that
 * are greater than or equal to the fromValue and smaller than or equal to the toValue.
 */
class FilterValueFunction(
    val fromValue: Double,
    val toValue: Double
) : DataSampleFunction {
    override suspend fun mapSample(dataSample: DataSample): DataSample {
        return DataSample.fromSequence(
            dataSample.filter { it.value in fromValue..toValue },
            dataSample.dataSampleProperties,
            dataSample::getRawDataPoints,
            dataSample::dispose
        )
    }
}