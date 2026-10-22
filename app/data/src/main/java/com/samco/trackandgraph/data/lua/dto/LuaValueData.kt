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
 * An immutable, VM-independent snapshot of JSON-compatible Lua-shaped data.
 *
 * Use this for values Kotlin owns and may later pass into Lua. It cannot contain functions,
 * userdata, metatables, or other runtime state. Use [LuaValueAccessor] to inspect a value that
 * still exists in a Lua runtime, and [LuaValueReference] to retain such a value for a later
 * engine operation on its owning runtime.
 */
sealed interface LuaValueData {
    data object Nil : LuaValueData
    data class BooleanValue(val value: Boolean) : LuaValueData
    data class NumberValue(val value: Double) : LuaValueData
    data class StringValue(val value: String) : LuaValueData
    data class ArrayValue(val values: List<LuaValueData>) : LuaValueData
    data class TableValue(val values: Map<String, LuaValueData>) : LuaValueData
}
