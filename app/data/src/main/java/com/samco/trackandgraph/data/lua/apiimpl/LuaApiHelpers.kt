/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.data.lua.apiimpl

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.OneArgFunction
import org.luaj.vm2.lib.ThreeArgFunction
import org.luaj.vm2.lib.TwoArgFunction
import org.luaj.vm2.lib.ZeroArgFunction

/**
 * Fails eagerly if the declaration is not found because that means it was not declared in the
 * lua api file. This makes sure unit tests fail if we rename/remove declarations in lua and
 * forget to update the implementation. It does not save us from added declarations that
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
