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
package com.samco.trackandgraph.data.lua.graphadapters

import com.samco.trackandgraph.data.lua.LuaVMAffinity
import com.samco.trackandgraph.data.lua.LuaVMLock
import com.samco.trackandgraph.data.lua.dto.LuaValueReference
import org.luaj.vm2.LuaValue

/** Data-layer implementation; the value is deliberately not decoded between Lua calls. */
internal class LuaValueReferenceImpl(
    val value: LuaValue,
    override val affinity: LuaVMAffinity,
) : LuaValueReference

internal fun LuaValueReference.valueFor(vmLock: LuaVMLock): LuaValue {
    require(affinity === vmLock.affinity) { "Lua value used with the wrong VM" }
    return (this as LuaValueReferenceImpl).value
}
