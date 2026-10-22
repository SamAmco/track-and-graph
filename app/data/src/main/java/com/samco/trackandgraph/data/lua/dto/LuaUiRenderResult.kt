/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua.dto

/**
 * A rendered Lua UI tree, its detached immutable [state] snapshot, and the opaque
 * [retainedState] that keeps the real Lua value available to the next interaction.
 */
data class LuaUiRenderResult(
    val ui: LuaValueAccessor,
    val state: LuaValueData?,
    val retainedState: LuaValueReference?,
)
