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

import com.samco.trackandgraph.database.entity.DataPointInterface
import com.samco.trackandgraph.database.entity.FeatureType

class DataSample(val dataPoints: List<DataPointInterface>, val featureType: FeatureType) {

    companion object {
        // when there are no points, we are ok with receiving null as featureType, and provide a fallback for that case
        fun emptySample(featureType: FeatureType?) =
            DataSample(listOf(), featureType ?: FeatureType.CONTINUOUS)
    }
}
