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
package com.samco.trackandgraph.data.lua.graphadapters

import com.samco.trackandgraph.data.lua.apiimpl.ColorParser
import com.samco.trackandgraph.data.lua.apiimpl.DateTimeParser
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.TimeBar
import com.samco.trackandgraph.data.lua.dto.TimeBarSegment
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.threeten.bp.Duration
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount
import javax.inject.Inject

internal class TimeBarChartLuaGraphAdapter @Inject constructor(
    private val colorParser: ColorParser,
    private val dateTimeParser: DateTimeParser,
) : LuaGraphAdaptor<LuaGraphResultData.TimeBarChartData> {
    companion object {
        const val BAR_DURATION = "bar_duration"
        const val BAR_PERIOD = "bar_period"
        const val BAR_PERIOD_MULTIPLE = "bar_period_multiple"
        const val DURATION_BASED_RANGE = "duration_based_range"
        const val BARS = "bars"
        const val Y_MAX = "y_max"
        const val VALUE = "value"
        const val LABEL = "label"
        const val COLOR = "color"
        const val END_TIME = "end_time"
    }

    override fun process(data: LuaValue): LuaGraphResultData.TimeBarChartData = when {
        data.istable() -> parseTimeBarChartData(data.checktable()!!)
        else -> throw IllegalArgumentException("Invalid data type for time bar chart")
    }

    private fun parseTimeBarChartData(data: LuaTable): LuaGraphResultData.TimeBarChartData = LuaGraphResultData.TimeBarChartData(
        barDuration = parseBarDuration(data),
        endTime = dateTimeParser.parseTimestampOrThrow(data[END_TIME]),
        durationBasedRange = data[DURATION_BASED_RANGE].optboolean(false),
        bars = parseBars(data[BARS].checktable()!!),
        yMax = data[Y_MAX].optnumber(null)?.todouble(),
    )

    private fun parseBarDuration(data: LuaValue): TemporalAmount {
        val barDuration = data[BAR_DURATION].optnumber(null)?.tolong()
        if (barDuration != null) {
            return Duration.ofMillis(barDuration)
        }

        val barPeriod = data[BAR_PERIOD]
        if (!barPeriod.isstring()) {
            throw IllegalArgumentException("Bar chart must provide either bar_duration or bar_period")
        }

        val barPeriodString = barPeriod.tojstring()
        val barPeriodMultiple = data[BAR_PERIOD_MULTIPLE].optint(1)
        return Period.parse(barPeriodString).multipliedBy(barPeriodMultiple)
    }

    private fun parseBars(data: LuaTable): List<TimeBar> = data.keys().map { parseBar(data[it]) }

    private fun parseBar(data: LuaValue): TimeBar = when {
        data.isnumber() -> TimeBar(listOf(TimeBarSegment(value = data.todouble())))
        data.istable() -> TimeBar(parseSegments(data.checktable()!!))
        else -> throw IllegalArgumentException("Invalid bar data")
    }

    private fun parseSegments(data: LuaTable): List<TimeBarSegment> = when {
        data[VALUE].isnumber() -> listOf(parseSegment(data))
        else -> data.keys().map { parseSegment(data[it]) }
    }

    private fun parseSegment(data: LuaValue): TimeBarSegment = when {
        data.isnumber() -> TimeBarSegment(value = data.todouble())
        data.istable() -> {
            TimeBarSegment(
                value = data[VALUE].checkdouble(),
                label = data[LABEL].optjstring(null),
                color = colorParser.parseColorOrNull(data[COLOR])
            )
        }

        else -> throw IllegalArgumentException("Invalid segment data")
    }
}
