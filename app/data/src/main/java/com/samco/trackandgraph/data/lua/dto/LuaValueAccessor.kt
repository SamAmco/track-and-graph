/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.data.lua.dto

/**
 * A read-only view of a Lua value retained by its originating VM.
 *
 * Unlike the immutable, detached [LuaValueData], this reads the real runtime value and may expose
 * a function as a [LuaValueReference]. Use [LuaValueReference] itself when a runtime value must be
 * passed back to the engine later without copying it.
 *
 * Plain values and tables that Lua treats as immutable may be traversed after the VM execution
 * lock has been released. Access uses raw table lookup and never invokes Lua metamethods. Callers
 * must not read a table while Lua code could be mutating that same table on another thread.
 *
 * Functions are different: [asFunction] returns an affinity-bound [LuaValueReference]. Invoking
 * one requires acquiring a new execution lock for that reference's affinity and passing both the
 * lock and reference back to [com.samco.trackandgraph.data.lua.LuaEngine]. The accessor itself
 * must never be used as a way to invoke Lua without the originating VM's execution lock.
 */
interface LuaValueAccessor {
    operator fun get(key: String): LuaValueAccessor?
    operator fun get(index: Int): LuaValueAccessor?

    val arraySize: Int?
    fun asDouble(): Double?
    fun asString(): String?
    /** Returns string-keyed string values when this value is a table. */
    fun asStringMap(): Map<String, String>? = null
    fun asBoolean(): Boolean?
    fun asFunction(): LuaValueReference?

    /**
     * Copies the JSON-compatible portion of this live value into detached [LuaValueData].
     * This traverses the value when called; callers that only need eventual persistence should
     * defer it with [lazy]. Unsupported runtime values are omitted, so the result is null when
     * the value itself cannot be represented.
     */
    fun snapshot(): LuaValueData?
}
