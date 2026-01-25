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
package com.samco.trackandgraph.data.lua.dto

import com.samco.trackandgraph.data.database.dto.DataPoint
import org.threeten.bp.ZonedDateTime
import org.threeten.bp.temporal.TemporalAmount

sealed interface LuaGraphResultData {
    data class DataPointData(
        val dataPoint: DataPoint?,
        val isDuration: Boolean,
    ) : LuaGraphResultData

    data class TextData(
        val text: String?,
        val size: TextSize,
        val alignment: TextAlignment,
    ) : LuaGraphResultData

    data class PieChartData(
        val segments: List<PieChartSegment>?
    ) : LuaGraphResultData

    data class LineGraphData(
        val lines: List<Line>?,
        val yMin: Double?,
        val yMax: Double?,
        val durationBasedRange: Boolean
    ) : LuaGraphResultData

    data class TimeBarChartData(
        val barDuration: TemporalAmount,
        val endTime: ZonedDateTime,
        val durationBasedRange: Boolean,
        val bars: List<TimeBar>,
        val yMax: Double?
    ) : LuaGraphResultData
}
