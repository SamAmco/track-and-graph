/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaUiScriptAdapter @Inject constructor() {
    fun render(
        vmLock: LuaVMLock,
        renderFunction: LuaValueHandle,
        renderState: LuaValueAccessor?,
        context: LuaUiContext,
        additionalParams: Map<String, LuaValueAccessor>,
    ): LuaValueHandle {
        val luaRenderState = renderState?.valueFor(vmLock) ?: LuaValue.NIL
        val params = LuaTable().apply {
            set(CONTEXT, context.toLuaValue())
            set(STATE, luaRenderState)
            additionalParams.forEach { (key, value) -> set(key, value.valueFor(vmLock)) }
        }
        val result = renderFunction.valueFor(vmLock)
            .call(params)
        return LuaValueHandleImpl(result, vmLock.affinity)
    }

    private companion object {
        const val CONTEXT = "context"
        const val STATE = "state"
    }

    private fun LuaUiContext.toLuaValue(): LuaValue = LuaTable().apply {
        set("constraints", LuaTable().apply {
            set("min_width", constraints.minWidth)
            set("max_width", constraints.maxWidth)
            set("min_height", constraints.minHeight)
            set("max_height", constraints.maxHeight)
        })
        set("density", density.toDouble())
        set("font_scale", fontScale.toDouble())
        set("layout_direction", layoutDirection.name)
        set("theme", theme.name)
    }
}
