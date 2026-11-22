/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua.community_test_runner

import com.samco.trackandgraph.data.lua.apiimpl.ModuleLoadInterceptor
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import java.util.IdentityHashMap

/** Test-only module gate; fixtures and assertions run with tracking disabled. */
internal class ApiUsageTracker(private val readResource: (String) -> String) : ModuleLoadInterceptor {
    private val sessions = IdentityHashMap<Globals, LuaTable>()

    private fun session(globals: Globals): LuaTable = sessions.getOrPut(globals) {
        val module = globals.load(readResource("generated/lua-community/test-utils/api-gate.lua")).call()
        module["new"].call().checktable()!!.also { it["set_tracking"].call(LuaValue.valueOf(false)) }
    }

    override fun onModuleLoad(globals: Globals, moduleName: String, module: LuaTable): LuaTable {
        if (!moduleName.startsWith("tng.")) return module
        val name = moduleName.removePrefix("tng.")
        val spec = globals.load(readResource("generated/lua-api/$name.apispec.lua")).call()
        return session(globals)["gate"].invoke(LuaValue.varargsOf(arrayOf(
            module, spec, LuaValue.valueOf(moduleName),
        ))).arg1().checktable()!!
    }

    fun <T> track(globals: Globals, block: () -> T): T {
        val gate = session(globals)
        val previous = gate["set_tracking"].call(LuaValue.valueOf(true))
        return try { block() } finally { gate["set_tracking"].call(previous) }
    }

    fun reset(globals: Globals) { session(globals)["reset"].call() }

    fun validate(globals: Globals, major: Int) {
        session(globals)["validate"].call(LuaValue.valueOf(major))
    }
}
