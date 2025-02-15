package com.samco.trackandgraph.lua.apiimpl

import com.samco.trackandgraph.lua.dto.ColorSpec
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class ColorParser @Inject constructor() {
    fun parseColorOrNull(value: LuaValue): ColorSpec? = when {
        value.isint() -> ColorSpec.ColorIndex(value.toint())
        value.isstring() -> ColorSpec.HexColor(value.tojstring())
        else -> null
    }
}