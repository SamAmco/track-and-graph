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

package com.samco.trackandgraph.calculators

/**
 * A calculator that simply applies the operations of all provided calculators in order
 */
class CompositeCalculator(vararg val calculators: DataCalculator) : DataCalculator {
    override suspend fun execute(dataSample: DataSample): DataSample {
        var runningSample = dataSample
        for (calculator in calculators) runningSample = calculator.execute(runningSample)
        return runningSample
    }
}