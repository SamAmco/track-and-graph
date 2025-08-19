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

package com.samco.trackandgraph.data.database.dto

import com.samco.trackandgraph.data.database.entity.LineGraph
import org.threeten.bp.temporal.TemporalAmount

data class LineGraph(
    val id: Long,
    val graphStatId: Long,
    val sampleSize: TemporalAmount?,
    val yRangeType: YRangeType,
    val yFrom: Double,
    val yTo: Double,
    val endDate: GraphEndDate
) {
    internal fun toEntity() = LineGraph(
        id,
        graphStatId,
        sampleSize,
        yRangeType,
        yFrom,
        yTo,
        endDate
    )
}
