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

import com.samco.trackandgraph.data.database.sampling.DataSample

/**
 * Represents a function that can modify a given [DataSample]
 */
interface DataSampleFunction {
    /**
     * Should check the validity of the input properties immediately and throw an exception
     * if they are invalid. However the sequence should be generated lazily where ever possible
     * without draining the upstream (for example by converting it to a list) or pre-calculating
     * any values which may not be needed if the downstream ceases to consume the sequence.
     */
    suspend fun mapSample(dataSample: DataSample): DataSample
}