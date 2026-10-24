/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle

/** App-layer session that owns a Lua UI renderer and its effective state. */
interface LuaUiContent {
    /** Effective state that will be passed to the next render. */
    val state: LuaValueAccessor?

    /** Returns the complete top-level table produced by the Lua renderer. */
    suspend fun render(context: LuaUiContext): LuaValueHandle

    /** Calls an action and rerenders before releasing their shared VM execution lock. */
    suspend fun interact(
        context: LuaUiContext,
        function: LuaValueHandle,
    ): LuaValueHandle

    /** Calls an auxiliary function, such as a deferred Canvas draw callback, without rerendering. */
    suspend fun call(
        function: LuaValueHandle,
        vararg arguments: LuaValueAccessor,
    ): LuaValueHandle
}
