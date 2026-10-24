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
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle

/** Retains one renderer and its current state on their originating Lua runtime. */
internal class RetainedLuaUiContent(
    private val luaEngine: LuaEngine,
    private val renderFunction: LuaValueHandle,
    initialState: LuaValueAccessor?,
    private val renderArguments: List<LuaValueAccessor> = emptyList(),
) : LuaUiContent {
    private var renderState = initialState
    override val state: LuaValueAccessor?
        get() = renderState

    override suspend fun render(context: LuaUiContext): LuaValueHandle = withRenderLock { vmLock ->
        renderLocked(vmLock, context)
    }

    override suspend fun interact(
        context: LuaUiContext,
        function: LuaValueHandle,
    ): LuaValueHandle = withRenderLock { vmLock ->
        luaEngine.callLuaFunction(vmLock, function)
        renderLocked(vmLock, context)
    }

    override suspend fun call(
        function: LuaValueHandle,
        vararg arguments: LuaValueAccessor,
    ): LuaValueHandle {
        val vmLock = luaEngine.acquireVM(function.affinity)
        return try {
            luaEngine.callLuaFunction(vmLock, function, *arguments)
        } finally {
            luaEngine.releaseVM(vmLock)
        }
    }

    private fun renderLocked(
        vmLock: LuaVMLock,
        context: LuaUiContext,
    ): LuaValueHandle {
        val result = luaEngine.renderLuaUi(
            vmLock,
            renderFunction,
            renderState,
            context,
            *renderArguments.toTypedArray(),
        )
        result[STATE]?.let { renderState = it }
        return result
    }

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

    private companion object {
        const val STATE = "state"
    }
}
