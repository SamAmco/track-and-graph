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

import com.samco.trackandgraph.database.entity.AggregatedDataPoint
import com.samco.trackandgraph.functionslib.DataSampleProperties

/**
 * This class represents the initial points clustered into the parent-attribute of the AggregatedDataPoints.
 * To obtain usable values, one of the follow-up functions like sum, average, or max need to be called.
 * Note that some follow-up functions drop data-points without parents. This is supposed to be intuitive :)
 */
internal abstract class AggregatedDataSample(
    val dataSampleProperties: DataSampleProperties
) : Sequence<AggregatedDataPoint> {
    companion object {
        fun fromSequence(
            data: Sequence<AggregatedDataPoint>,
            dataSampleProperties: DataSampleProperties = DataSampleProperties()
        ): AggregatedDataSample {
            return object : AggregatedDataSample(dataSampleProperties) {
                override fun iterator(): Iterator<AggregatedDataPoint> = data.iterator()
            }
        }
    }
}