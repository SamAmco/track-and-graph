package com.samco.trackandgraph.lua.apiimpl

import org.luaj.vm2.LuaError
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class RequireApiImpl @Inject constructor() {

    companion object {
        const val REQUIRE = "require"
    }

    fun installIn(parent: LuaTable, installedApis: Map<String, LuaTable>) = parent.apply {
        parent[REQUIRE] = getRequireLuaFunction(installedApis)
    }

    private fun getRequireLuaFunction(installedApis: Map<String, LuaTable>): LuaValue = oneArgFunction { arg: LuaValue ->
        val moduleName = arg.tojstring()
        if (moduleName in installedApis) {
            return@oneArgFunction installedApis[moduleName] ?: LuaValue.NIL
        } else {
            throw LuaError("You can only require \"tng\" in graph scripts right now")
        }
    }
}
