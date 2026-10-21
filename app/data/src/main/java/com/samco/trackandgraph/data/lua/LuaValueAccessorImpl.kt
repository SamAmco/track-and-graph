/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import org.luaj.vm2.LuaValue

/** Raw access deliberately avoids invoking a table's __index compatibility-test metatable. */
internal class LuaValueAccessorImpl(
    private val value: LuaValue,
    private val affinity: LuaVMAffinity,
) : LuaValueAccessor {
    override fun get(key: String): LuaValueAccessor? =
        if (value.istable()) value.rawget(key).accessorOrNull() else null

    /** Lua arrays are exposed with their native one-based indices. */
    override fun get(index: Int): LuaValueAccessor? =
        if (value.istable()) value.rawget(index).accessorOrNull() else null

    override val arraySize: Int?
        get() = if (value.istable()) value.rawlen() else null

    override fun asDouble(): Double? = if (value.isnumber()) value.todouble() else null
    override fun asString(): String? = if (value.isstring()) value.tojstring() else null
    override fun asStringMap(): Map<String, String>? {
        if (!value.istable()) return null
        val table = value.checktable()!!
        return buildMap {
            table.keys().forEach { key ->
                val entry = table.rawget(key)
                if (key.type() == LuaValue.TSTRING && entry.type() == LuaValue.TSTRING) {
                    put(key.tojstring(), entry.tojstring())
                }
            }
        }
    }
    override fun asBoolean(): Boolean? = if (value.isboolean()) value.toboolean() else null
    override fun asFunction(): LuaValueReference? =
        if (value.isfunction()) LuaValueReferenceImpl(value, affinity) else null

    private fun LuaValue.accessorOrNull(): LuaValueAccessor? =
        if (isnil()) null else LuaValueAccessorImpl(this, affinity)
}
