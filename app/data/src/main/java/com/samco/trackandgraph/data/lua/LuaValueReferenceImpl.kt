/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import org.luaj.vm2.LuaValue

/** Data-layer implementation; the value is deliberately not decoded between Lua calls. */
internal class LuaValueReferenceImpl(
    val value: LuaValue,
    override val affinity: LuaVMAffinity,
) : LuaValueReference

internal fun LuaValueReference.valueFor(vmLock: LuaVMLock): LuaValue {
    vmLock.requireActiveLease()
    require(affinity === vmLock.affinity) { "Lua value used with the wrong VM" }
    return (this as LuaValueReferenceImpl).value
}
