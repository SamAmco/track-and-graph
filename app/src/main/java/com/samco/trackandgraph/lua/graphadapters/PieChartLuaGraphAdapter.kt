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
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.PieChartSegment
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class PieChartLuaGraphAdapter @Inject constructor(
    private val colorParser: ColorParser
) : LuaGraphAdaptor<LuaGraphResultData.PieChartData> {

    companion object {
        const val VALUE = "value"
        const val LABEL = "label"
        const val COLOR = "color"
    }

    override fun process(data: LuaValue): LuaGraphResultData.PieChartData = when {
        data.istable() -> parseSegments(data.checktable()!!)
        else -> throw IllegalArgumentException("Invalid data type for pie chart")
    }

    private fun parseSegments(data: LuaTable): LuaGraphResultData.PieChartData {
        val segments = mutableListOf<PieChartSegment>()
        for (key in data.keys()) {
            segments.add(parseSegment(data[key]))
        }
        return LuaGraphResultData.PieChartData(segments)
    }

    private fun parseSegment(data: LuaValue): PieChartSegment {
        return PieChartSegment(
            value = data[VALUE].checknumber()!!.todouble(),
            label = data[LABEL].checkjstring()!!,
            color = colorParser.parseColorOrNull(data[COLOR])
        )
    }
}
