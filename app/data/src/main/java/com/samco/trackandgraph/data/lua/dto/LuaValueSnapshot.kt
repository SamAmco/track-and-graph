/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua.dto

/**
 * An immutable [LuaValueAccessor] detached from any Lua VM.
 *
 * Snapshots contain only ordinary JSON-compatible Lua-shaped data. They may be persisted, passed
 * between runtimes, or read from any thread without acquiring a Lua VM execution lock. Unlike a
 * live [LuaValueHandle], a snapshot has no VM affinity and cannot contain or invoke functions.
 */
sealed interface LuaValueSnapshot : LuaValueAccessor {
    data object Nil : LuaValueSnapshot {
        override fun get(key: String) = null
        override fun get(index: Int) = null
        override val arraySize = null
        override fun asDouble() = null
        override fun asString() = null
        override fun asBoolean() = null
    }

    data class BooleanValue(val value: Boolean) : LuaValueSnapshot {
        override fun get(key: String) = null
        override fun get(index: Int) = null
        override val arraySize = null
        override fun asDouble() = null
        override fun asString() = null
        override fun asBoolean() = value
    }

    data class NumberValue(val value: Double) : LuaValueSnapshot {
        override fun get(key: String) = null
        override fun get(index: Int) = null
        override val arraySize = null
        override fun asDouble() = value
        override fun asString() = null
        override fun asBoolean() = null
    }

    data class StringValue(val value: String) : LuaValueSnapshot {
        override fun get(key: String) = null
        override fun get(index: Int) = null
        override val arraySize = null
        override fun asDouble() = null
        override fun asString() = value
        override fun asBoolean() = null
    }

    data class ArrayValue(val values: List<LuaValueSnapshot>) : LuaValueSnapshot {
        override fun get(key: String) = null
        override fun get(index: Int) = values.getOrNull(index - 1)?.takeUnless { it === Nil }
        override val arraySize = values.size
        override fun asDouble() = null
        override fun asString() = null
        override fun asBoolean() = null
    }

    data class TableValue(val values: Map<String, LuaValueSnapshot>) : LuaValueSnapshot {
        override fun get(key: String) = values[key]?.takeUnless { it === Nil }
        override fun get(index: Int) = null
        override val arraySize = 0
        override fun asDouble() = null
        override fun asString() = null
        override fun asStringMap(): Map<String, String> = values.mapNotNull { (key, value) ->
            value.asString()?.let { key to it }
        }.toMap()
        override fun asBoolean() = null
    }
}
