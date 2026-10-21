/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua.dto

/** A rendered Lua UI tree and the state to use for its next render. */
data class LuaUiRenderResult(
    val ui: LuaValueAccessor,
    val state: LuaValueReference,
    val serializedState: String?,
)
