package com.samco.trackandgraph.lua.apiimpl

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * Fails eagerly if the declaration is not found because that means it was not declared in the
 * tng.lua api file. This makes sure unit tests fail if we rename/remove declarations in tng.lua and
 * forget to update the implementation. It unfortunately does not save us from added declarations that
 * are not implemented or not implemented correctly. For this we must rely on adding tests.
 */
internal fun LuaValue?.overrideOrThrow(name: String, value: LuaValue): LuaValue {
    if (this == null || this[name].isnil()) {
        throw IllegalStateException("No declaration found for $name found")
    } else {
        this[name] = value
        return value
    }
}

internal fun merge(vararg values: LuaTable): LuaTable {
    val result = LuaTable()
    values.forEach { table ->
        table.keys().forEach { key ->
            result[key] = table[key]
        }
    }
    return result
}

internal fun zeroArgFunction(callback: () -> LuaValue) = object : ZeroArgFunction() {
    override fun call(): LuaValue = callback()
}

internal fun oneArgFunction(callback: (LuaValue) -> LuaValue) = object : OneArgFunction() {
    override fun call(arg: LuaValue): LuaValue = callback(arg)
}

internal fun twoArgFunction(callback: (LuaValue, LuaValue) -> LuaValue) = object : TwoArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue): LuaValue = callback(arg1, arg2)
}

internal fun threeArgFunction(callback: (LuaValue, LuaValue, LuaValue) -> LuaValue) = object : ThreeArgFunction() {
    override fun call(arg1: LuaValue, arg2: LuaValue, arg3: LuaValue): LuaValue = callback(arg1, arg2, arg3)
}
