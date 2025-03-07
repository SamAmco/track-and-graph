package com.samco.trackandgraph.lua.graphadapters

import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TextAlignment
import com.samco.trackandgraph.lua.dto.TextSize
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class TextLuaGraphAdapter @Inject constructor() : LuaGraphAdaptor<LuaGraphResultData.TextData> {

    companion object {
        const val TEXT = "text"
        const val SIZE = "size"
        const val ALIGN = "align"
        const val START = "start"
        const val END = "end"
        const val CENTER = "center"
        const val CENTER_ALT = "centre"
    }

    private fun parseSize(data: LuaValue): TextSize = when {
        data.isint() -> TextSize.entries[data.checkint(1) - 1]
        else -> TextSize.MEDIUM
    }

    private fun parseAlignment(data: LuaValue): TextAlignment {
        return when {
            data.isstring() -> when (data.tojstring()) {
                START -> TextAlignment.START
                CENTER, CENTER_ALT -> TextAlignment.CENTER
                END -> TextAlignment.END
                else -> throw IllegalArgumentException("Invalid alignment value: ${data.tojstring()}")
            }

            else -> TextAlignment.CENTER
        }
    }

    override fun process(data: LuaValue): LuaGraphResultData.TextData {
        return when {
            data.istable() -> LuaGraphResultData.TextData(
                text = data[TEXT].optjstring(null),
                size = parseSize(data[SIZE]),
                alignment = parseAlignment(data[ALIGN]),
            )

            else -> LuaGraphResultData.TextData(
                text = data.optjstring(null),
                size = TextSize.LARGE,
                alignment = TextAlignment.CENTER,
            )
        }
    }
}