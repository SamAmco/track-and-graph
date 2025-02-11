package com.samco.trackandgraph.lua.graphadapters

import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import org.luaj.vm2.LuaValue

interface LuaGraphAdaptor<T : LuaGraphResultData> {
    fun process(data: LuaValue): T?
}