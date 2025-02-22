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
package com.samco.trackandgraph.lua.graphadapters

import com.samco.trackandgraph.lua.apiimpl.ColorParser
import com.samco.trackandgraph.lua.apiimpl.DateTimeParser
import com.samco.trackandgraph.lua.dto.Line
import com.samco.trackandgraph.lua.dto.LinePoint
import com.samco.trackandgraph.lua.dto.LinePointStyle
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class LineGraphLuaGraphAdapter @Inject constructor(
    private val dateTimeParser: DateTimeParser,
    private val colorParser: ColorParser,
) : LuaGraphAdaptor<LuaGraphResultData.LineGraphData> {

    companion object {
        const val DURATION_BASED_RANGE = "duration_based_range"
        const val RANGE_BOUNDS = "range_bounds"
        const val MIN = "min"
        const val MAX = "max"
        const val LINES = "lines"
        const val LINE_COLOR = "line_color"
        const val POINT_STYLE = "point_style"
        const val LINE_POINTS = "line_points"
        const val LABEL = "label"
        const val VALUE = "value"
    }

    override fun process(data: LuaValue): LuaGraphResultData.LineGraphData = when {
        data.istable() -> parseLineGraphData(data.checktable()!!)
        else -> throw IllegalArgumentException("Invalid data type for line graph")
    }

    private fun parseLineGraphData(data: LuaTable): LuaGraphResultData.LineGraphData {
        val lines = parseLines(data[LINES].checktable()!!)
        val durationBasedRange = data[DURATION_BASED_RANGE].optboolean(false)
        var yMin: Double? = null
        var yMax: Double? = null
        data[RANGE_BOUNDS].takeIf { !it.isnil() }?.let {
            yMin = it[MIN].optnumber(null)?.todouble()
            yMax = it[MAX].optnumber(null)?.todouble()
        }
        return LuaGraphResultData.LineGraphData(
            lines = lines,
            yMin = yMin,
            yMax = yMax,
            durationBasedRange = durationBasedRange
        )
    }

    private fun parseLines(data: LuaTable): List<Line> {
        val lines = mutableListOf<Line>()
        for (key in data.keys()) {
            lines.add(parseLine(data[key]))
        }
        return lines
    }

    private fun parseLine(data: LuaValue): Line {
        val lineColor = colorParser.parseColorOrNull(data[LINE_COLOR])
        val pointStyle = parsePointStyle(data[POINT_STYLE])
        val linePoints = parseLinePoints(data[LINE_POINTS].checktable()!!)
        val label = data[LABEL].optjstring(null)
        return Line(
            lineColor = lineColor,
            pointStyle = pointStyle,
            label = label,
            linePoints = linePoints,
        )
    }

    private fun parseLinePoints(data: LuaTable): List<LinePoint> {
        val linePoints = mutableListOf<LinePoint>()
        for (key in data.keys()) {
            linePoints.add(parseLinePoint(data[key]))
        }
        return linePoints
    }

    private fun parseLinePoint(data: LuaValue): LinePoint {
        val timestamp = dateTimeParser.parseOffsetDateTimeOrThrow(data)
        val value = data[VALUE].checknumber()!!.todouble()
        return LinePoint(timestamp, value)
    }

    private fun parsePointStyle(data: LuaValue): LinePointStyle? {
        return when {
            data.isstring() -> LinePointStyle.entries.first { it.luaVlaue == data.tojstring() }
            else -> null
        }
    }
}