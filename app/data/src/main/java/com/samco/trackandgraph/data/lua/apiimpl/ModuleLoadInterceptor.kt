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
package com.samco.trackandgraph.data.lua.apiimpl

import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable

internal interface ModuleLoadInterceptor {
    /**
     * Allows an interceptor to modify a module before it is returned to the caller.
     * Useful in testing to gate the API access.
     */
    fun onModuleLoad(globals: Globals, moduleName: String, module: LuaTable): LuaTable
}

/**
 * Don't need to do anything special when loading a module in prod. This is used in
 * test code to gate/assert API access.
 */
internal class NoOpModuleLoadInterceptorImpl : ModuleLoadInterceptor {
    override fun onModuleLoad(globals: Globals, moduleName: String, module: LuaTable) = module
}
