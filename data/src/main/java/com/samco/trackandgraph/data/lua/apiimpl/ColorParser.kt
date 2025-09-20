package com.samco.trackandgraph.data.lua.apiimpl

import com.samco.trackandgraph.data.lua.dto.ColorSpec
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class ColorParser @Inject constructor() {
    fun parseColorOrNull(value: LuaValue): ColorSpec? = when {
        value.isint() -> ColorSpec.ColorIndex(value.toint())
        value.isstring() -> ColorSpec.HexColor(value.tojstring())
        else -> null
    }
}