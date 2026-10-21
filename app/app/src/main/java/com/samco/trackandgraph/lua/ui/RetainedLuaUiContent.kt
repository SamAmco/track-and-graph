/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaVMLock
import com.samco.trackandgraph.data.lua.dto.LuaUiContent
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaUiRenderResult
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import com.samco.trackandgraph.data.lua.dto.LuaValueReference

/** Retains one renderer and its current state on their originating Lua runtime. */
internal class RetainedLuaUiContent(
    private val luaEngine: LuaEngine,
    private val renderFunction: LuaValueReference,
    initialState: LuaValueReference,
    private val renderArguments: List<LuaValueReference> = emptyList(),
) : LuaUiContent {
    private var state = initialState

    override suspend fun render(context: LuaUiContext): LuaUiRenderResult = withRenderLock { vmLock ->
        renderLocked(vmLock, context)
    }

    override suspend fun interact(
        context: LuaUiContext,
        function: LuaValueReference,
    ): LuaUiRenderResult = withRenderLock { vmLock ->
        luaEngine.callLuaFunction(vmLock, function)
        renderLocked(vmLock, context)
    }

    override suspend fun call(
        function: LuaValueReference,
        argument: LuaValueData,
    ): LuaValueAccessor {
        val vmLock = luaEngine.acquireVM(function.affinity)
        return try {
            luaEngine.callLuaFunction(vmLock, function, argument)
        } finally {
            luaEngine.releaseVM(vmLock)
        }
    }

    private fun renderLocked(
        vmLock: LuaVMLock,
        context: LuaUiContext,
    ): LuaUiRenderResult = luaEngine.renderLuaUi(
        vmLock,
        renderFunction,
        state,
        context,
        *renderArguments.toTypedArray(),
    ).also { state = it.state }

    private suspend fun <T> withRenderLock(
        block: (LuaVMLock) -> T,
    ): T {
        val vmLock = luaEngine.acquireVM(renderFunction.affinity)
        return try {
            block(vmLock)
        } finally {
            luaEngine.releaseVM(vmLock)
        }
    }
}
