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
 * A calculator that simply applies the operations of all provided calculators in order
 */
class CompositeFunction(private vararg val calculators: DataSampleFunction) :
    DataSampleFunction {

    constructor(calculators: List<DataSampleFunction>) : this(*calculators.toTypedArray())

    override suspend fun mapSample(dataSample: DataSample): DataSample {
        var runningSample = dataSample
        for (calculator in calculators) runningSample = calculator.mapSample(runningSample)
        return runningSample
    }
}