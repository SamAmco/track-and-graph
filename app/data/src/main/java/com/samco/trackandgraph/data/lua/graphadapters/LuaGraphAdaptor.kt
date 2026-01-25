package com.samco.trackandgraph.data.lua.graphadapters

import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import org.luaj.vm2.LuaValue

internal interface LuaGraphAdaptor<T : LuaGraphResultData> {
    fun process(data: LuaValue): T?
}