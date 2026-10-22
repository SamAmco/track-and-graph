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
 * A rendered Lua UI tree, a readable view of its live [state], and the opaque [retainedState]
 * that feeds that same real Lua value into the next interaction.
 */
data class LuaUiRenderResult(
    val ui: LuaValueAccessor,
    val state: LuaValueAccessor?,
    val retainedState: LuaValueReference?,
)
