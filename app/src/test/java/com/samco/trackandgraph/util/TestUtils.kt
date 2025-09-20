/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.util

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.sampling.RawDataSample

fun rawDataSampleFromSequence(
    data: Sequence<DataPoint>,
    onDispose: () -> Unit
) = object : RawDataSample() {
    private val visited = mutableListOf<DataPoint>()
    override fun getRawDataPoints() = visited
    override fun iterator() = data.onEach { visited.add(it) }.iterator()
    override fun dispose() = onDispose()
}

fun group(name: String = "", id: Long = 0, parentId: Long? = 0): Group {
    return Group(id, name, 0, parentId, 0)
}
