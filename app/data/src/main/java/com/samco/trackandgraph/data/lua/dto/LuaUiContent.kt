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

/** A session-bound source of a Lua UI tree and invoker for callbacks contained in that tree. */
interface LuaUiContent {
    suspend fun render(context: LuaUiContext): LuaUiRenderResult

    /** Calls an action and rerenders before releasing their shared VM execution lock. */
    suspend fun interact(
        context: LuaUiContext,
        function: LuaValueReference,
    ): LuaUiRenderResult

    /** Calls an auxiliary function, such as a deferred Canvas draw callback, without rerendering. */
    suspend fun call(
        function: LuaValueReference,
        argument: LuaValueData = LuaValueData.Nil,
    ): LuaValueAccessor
}
