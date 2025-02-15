package com.samco.trackandgraph.lua.graphadapters

import com.samco.trackandgraph.lua.apiimpl.ColorParser
import com.samco.trackandgraph.lua.dto.LuaGraphResult
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.PieChartSegment
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class PieChartLuaGraphAdapter @Inject constructor(
    private val colorParser: ColorParser
) : LuaGraphAdaptor<LuaGraphResultData.PieChartData>{

    companion object {
        const val VALUE = "value"
        const val LABEL = "label"
        const val COLOR = "color"
    }

    override fun process(data: LuaValue): LuaGraphResultData.PieChartData = when {
        data.isnil() -> LuaGraphResultData.PieChartData(null)
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