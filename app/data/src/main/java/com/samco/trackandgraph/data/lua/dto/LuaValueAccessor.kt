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
 * Read-only access to a value in Lua shape.
 *
 * This interface deliberately says nothing about the value's source, mutability, or VM ownership.
 * [LuaValueSnapshot] adds the guarantee that a value is immutable and detached, while
 * [LuaValueHandle] identifies a live value owned by a Lua VM.
 */
sealed interface LuaValueAccessor {
    operator fun get(key: String): LuaValueAccessor?
    operator fun get(index: Int): LuaValueAccessor?

    val arraySize: Int?
    fun asDouble(): Double?
    fun asString(): String?
    /** Returns string-keyed string values when this value is a table. */
    fun asStringMap(): Map<String, String>? = null
    fun asBoolean(): Boolean?
}
