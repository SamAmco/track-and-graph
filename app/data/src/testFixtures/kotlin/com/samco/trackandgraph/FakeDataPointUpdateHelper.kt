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

package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.entity.DataPoint
import com.samco.trackandgraph.data.interactor.DataPointUpdateHelper

internal class FakeDataPointUpdateHelper : DataPointUpdateHelper {
    override fun performUpdate(
        whereValue: Double?,
        whereLabel: String?,
        toValue: Double?,
        toLabel: String?,
        getNumDataPoints: () -> Int,
        isDuration: () -> Boolean,
        getDataPoints: DataPointUpdateHelper.DataPointRetriever,
        performUpdate: (List<DataPoint>) -> Unit
    ) {
        // No-op for tests that don't exercise data point updates
    }
}
