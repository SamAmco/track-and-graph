package com.samco.trackandgraph.lua.graphadapters

import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TextSize
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class TextLuaGraphAdapter @Inject constructor() : LuaGraphAdaptor<LuaGraphResultData.TextData> {

    companion object {
        const val TEXT = "text"
        const val SIZE = "size"
    }

    private fun parseSize(data: LuaValue): TextSize {
        return when {
            data.isint() -> TextSize.entries[data.optint(1) - 1]
            else -> TextSize.LARGE
        }
    }

    override fun process(data: LuaValue): LuaGraphResultData.TextData {
        return when {
            data.istable() -> LuaGraphResultData.TextData(
                text = data[TEXT].optjstring(null),
                size = parseSize(data[SIZE])
            )

            else -> LuaGraphResultData.TextData(
                text = data.optjstring(null),
                size = TextSize.LARGE
            )
        }
    }
}