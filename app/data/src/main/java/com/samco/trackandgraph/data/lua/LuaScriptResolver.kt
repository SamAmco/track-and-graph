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

package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.apiimpl.RequireApiImpl
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject

internal class LuaScriptResolver @Inject constructor(
    private val requireApi: RequireApiImpl,
) {
    fun resolveLuaScript(script: String, vmLease: VMLease): LuaValue {
        return vmLease.globals
            .load(script.cleanLuaScript(), "=script", createEnvironment(vmLease))
            .call()
    }

    fun resolveLuaScript(accessor: LuaGraphScriptAccessor, vmLease: VMLease): LuaValue {
        val environment = createEnvironment(vmLease)
        environment[RequireApiImpl.REQUIRE] = requireApi.createRequireFunction(
            globals = vmLease.globals,
            environment = environment,
            scriptAccessor = accessor,
        )
        return vmLease.globals
            .load(accessor.mainScript().cleanLuaScript(), "@main.lua", environment)
            .call()
    }

    private fun createEnvironment(vmLease: VMLease) = LuaTable().apply {
        setmetatable(LuaTable().apply { set("__index", vmLease.globals) })
        set("_G", this)
    }

    private fun String.cleanLuaScript(): String {
        return this
            // Replace NBSP with space
            .replace(Regex("\\u00A0"), " ")
            // Remove zero-width space and BOM
            .replace(Regex("[\\u200B\\uFEFF]"), "")
            // Replace all new lines with the same newline character
            .replace(Regex("[\\r\\n]+"), "\n")
            .trim()
    }
}
