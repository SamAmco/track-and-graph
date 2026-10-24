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
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

/** Raw access deliberately avoids invoking a table's __index compatibility-test metatable. */
internal class LuaValueHandleImpl(
    internal val value: LuaValue,
    override val affinity: LuaVMAffinity,
) : LuaValueHandle {
    override fun get(key: String): LuaValueHandle? =
        if (value.istable()) value.rawget(key).handleOrNull() else null

    /** Lua arrays are exposed with their native one-based indices. */
    override fun get(index: Int): LuaValueHandle? =
        if (value.istable()) value.rawget(index).handleOrNull() else null

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
    override fun asFunction(): LuaValueHandle? = if (value.isfunction()) this else null
    override fun snapshot(): LuaValueSnapshot? = LuaStateJsonCodec.snapshotValue(value)

    private fun LuaValue.handleOrNull(): LuaValueHandle? =
        if (isnil()) null else LuaValueHandleImpl(this, affinity)
}

internal fun LuaValueHandle.valueFor(vmLock: LuaVMLock): LuaValue {
    vmLock.requireActiveLease()
    require(affinity === vmLock.affinity) { "Lua value used with the wrong VM" }
    return (this as LuaValueHandleImpl).value
}

internal fun LuaValueAccessor.valueFor(vmLock: LuaVMLock): LuaValue = when (this) {
    is LuaValueHandle -> valueFor(vmLock)
    is LuaValueSnapshot -> toLuaValue()
}

internal fun LuaValueSnapshot.toLuaValue(): LuaValue = when (this) {
    LuaValueSnapshot.Nil -> LuaValue.NIL
    is LuaValueSnapshot.BooleanValue -> LuaValue.valueOf(value)
    is LuaValueSnapshot.NumberValue -> LuaValue.valueOf(value)
    is LuaValueSnapshot.StringValue -> LuaValue.valueOf(value)
    is LuaValueSnapshot.ArrayValue -> LuaTable().also { table ->
        values.forEachIndexed { index, value -> table.set(index + 1, value.toLuaValue()) }
    }
    is LuaValueSnapshot.TableValue -> LuaTable().also { table ->
        values.forEach { (key, value) -> table.set(key, value.toLuaValue()) }
    }
}
