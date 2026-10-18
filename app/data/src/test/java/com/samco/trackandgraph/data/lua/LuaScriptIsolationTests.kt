/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test

internal class LuaScriptIsolationTests : LuaEngineImplTest() {

    @Test
    fun `plain script globals do not leak into the next execution on the same VM`() = runBlocking {
        val engine = uut()
        val lock = engine.acquireVM()

        val second = try {
            engine.runLuaFunction(
                lock,
                "_G.leaked = 'yes'; return { id = 'first' }",
            )
            engine.runLuaFunction(
                lock,
                "return { id = _G.leaked or 'isolated' }",
            )
        } finally {
            engine.releaseVM(lock)
        }

        assertEquals("isolated", second.id)
    }

    @Test
    fun `returned closures retain their globals and can use built-in modules`() = runBlocking {
        val engine = uut()
        val lock = engine.acquireVM()

        val results = try {
            engine.runLuaFunctionGenerator(
                vmLock = lock,
                script = """
                    local core = require("tng.core")
                    _G.counter = 0
                    return function()
                        return function()
                            _G.counter = _G.counter + 1
                            if _G.counter > 2 then return nil end
                            local result = core.time()
                            result.value = _G.counter
                            return result
                        end
                    end
                """.trimIndent(),
                dataSources = emptyList(),
                configuration = emptyList(),
            ).toList()
        } finally {
            engine.releaseVM(lock)
        }

        assertEquals(listOf(1.0, 2.0), results.map { it.value })
    }
}
