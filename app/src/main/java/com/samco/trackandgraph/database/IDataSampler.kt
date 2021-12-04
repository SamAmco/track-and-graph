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

package com.samco.trackandgraph.database

import com.samco.trackandgraph.database.dto.IDataPoint
import com.samco.trackandgraph.functionslib.DataSample

interface IDataSampler {
    //TODO we should be able to get rid of these two functions and just have one that is backed by
    // a cursor. Any other filtering on the returned data could be done with a function wrapper
    // the same way it is done for clipping
    fun getLastDataPointBetween(
        dataSource: DataSource,
        min: String,
        max: String
    ): IDataPoint?

    fun getLastDataPointWithValue(
        dataSource: DataSource,
        values: List<Int>
    ): IDataPoint?

    fun getDataPointsBetween(
        dataSource: DataSource,
        min: String,
        max: String
    ): DataSample

    fun getDataPointsWithValue(
        dataSource: DataSource,
        values: List<Int>
    ): DataSample

    fun getDataPointsForDataSource(dataSource: DataSource): DataSample
}