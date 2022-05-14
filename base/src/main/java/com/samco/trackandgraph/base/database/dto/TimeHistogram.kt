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

package com.samco.trackandgraph.base.database.dto

import com.samco.trackandgraph.base.database.entity.TimeHistogram
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

data class TimeHistogram(
    val id: Long,
    val graphStatId: Long,
    val featureId: Long,
    val duration: Duration?,
    val window: TimeHistogramWindow,
    val sumByCount: Boolean,
    val endDate: OffsetDateTime?
) {
    internal fun toEntity() = TimeHistogram(
        id,
        graphStatId,
        featureId,
        duration,
        window,
        sumByCount,
        endDate
    )
}