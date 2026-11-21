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
import com.samco.trackandgraph.data.lua.LuaVMAffinity
import com.samco.trackandgraph.data.lua.LuaVMLock
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle

/** Retains one renderer and its current state on their originating Lua runtime. */
internal class RetainedLuaUiContent(
    private val luaEngine: LuaEngine,
    private val renderFunction: LuaValueHandle,
    initialState: LuaValueAccessor?,
    private val defaultAdditionalParams: Map<String, LuaValueAccessor> = emptyMap(),
) : LuaUiContent {
    private var renderState = initialState
    override val state: LuaValueAccessor?
        get() = renderState

    override suspend fun render(
        context: LuaUiContext,
        additionalParams: Map<String, LuaValueAccessor>,
    ): LuaValueHandle = withVMLock { vmLock ->
        renderLocked(vmLock, context, additionalParams)
    }

    override suspend fun interact(
        context: LuaUiContext,
        actions: List<LuaUiAction>,
        additionalParams: Map<String, LuaValueAccessor>,
    ): LuaValueHandle = withVMLock { vmLock ->
        actions.forEach { action ->
            luaEngine.callLuaFunction(
                vmLock,
                action.function,
                *action.arguments.toTypedArray(),
            )
        }
        renderLocked(vmLock, context, additionalParams)
    }

    override suspend fun call(
        function: LuaValueHandle,
        vararg arguments: LuaValueAccessor,
    ): LuaValueHandle = withVMLock(function.affinity) { vmLock ->
        luaEngine.callLuaFunction(vmLock, function, *arguments)
    }

    private fun renderLocked(
        vmLock: LuaVMLock,
        context: LuaUiContext,
        additionalParams: Map<String, LuaValueAccessor>,
    ): LuaValueHandle {
        val result = luaEngine.renderLuaUi(
            vmLock,
            renderFunction,
            renderState,
            context,
            defaultAdditionalParams + additionalParams,
        )
        result[STATE]?.let { renderState = it }
        return result
    }

    private suspend fun <T> withVMLock(
        affinity: LuaVMAffinity = renderFunction.affinity,
        block: (LuaVMLock) -> T,
    ): T {
        val vmLock = luaEngine.acquireVM(affinity)
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
