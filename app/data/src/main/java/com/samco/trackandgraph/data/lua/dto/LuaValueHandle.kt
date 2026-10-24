/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua.dto

import com.samco.trackandgraph.data.lua.LuaVMAffinity

/**
 * A live [LuaValueAccessor] whose identity belongs to the VM identified by [affinity].
 *
 * Raw value and table reads do not invoke Lua code, but callers must not traverse a value while
 * its owning VM could be mutating it. Passing this value back to Lua or invoking a function
 * obtained through [asFunction] requires an execution lock for [affinity]. Unlike an immutable,
 * detached [LuaValueSnapshot], a handle preserves Lua identity, closures, and mutable table state.
 */
interface LuaValueHandle : LuaValueAccessor {
    val affinity: LuaVMAffinity

    override fun get(key: String): LuaValueHandle?
    override fun get(index: Int): LuaValueHandle?

    /** Returns this handle when it contains a Lua function, otherwise null. */
    fun asFunction(): LuaValueHandle?

    /** Copies the JSON-compatible portion of this live value into an immutable snapshot. */
    fun snapshot(): LuaValueSnapshot?
}
