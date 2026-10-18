/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaScriptException
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test
import java.io.ByteArrayOutputStream
import java.util.zip.ZipEntry
import java.util.zip.ZipOutputStream

internal class LuaGraphPackageTests : LuaEngineImplTest() {
    @Test
    fun `zip accessor resolves dotted modules and init modules`() {
        val accessor = ZipLuaGraphScriptAccessor(zip(
            "main.lua" to "return {}",
            "chart/renderer.lua" to "return 1",
            "helpers/init.lua" to "return 2",
        ))

        assertEquals("return {}", accessor.mainScript())
        assertEquals("return 1", accessor.moduleScript("chart.renderer"))
        assertEquals("return 2", accessor.moduleScript("helpers"))
        assertEquals(null, accessor.moduleScript("missing"))
    }

    @Test
    fun `zip accessor permits entries larger than one MiB`() {
        val largeModule = "-".repeat(2 * 1024 * 1024)
        val accessor = ZipLuaGraphScriptAccessor(zip(
            "main.lua" to "return {}",
            "large.lua" to largeModule,
        ))

        assertEquals(largeModule, accessor.moduleScript("large"))
    }

    @Test
    fun `zip accessor rejects traversal entries`() {
        assertThrows(IllegalArgumentException::class.java) {
            ZipLuaGraphScriptAccessor(zip(
                "main.lua" to "return {}",
                "../escape.lua" to "return {}",
            ))
        }
    }

    @Test
    fun `circular require is detected before looking up the module again`() = runBlocking {
        var moduleLookups = 0
        val accessor = object : LuaGraphScriptAccessor {
            override fun mainScript() = "return require('loop')"

            override fun moduleScript(moduleName: String): String {
                moduleLookups++
                return "return require('loop')"
            }
        }
        val engine = uut()
        val lock = engine.acquireVM()

        val error = try {
            assertThrows(LuaScriptException::class.java) {
                engine.runLuaScript(lock, accessor)
            }
        } finally {
            engine.releaseVM(lock)
        }

        assertEquals("circular require for module 'loop'", error.message)
        assertEquals(1, moduleLookups)
    }

    @Test
    fun `failed module lookup does not remain marked as loading`() = runBlocking {
        val accessor = object : LuaGraphScriptAccessor {
            override fun mainScript() = """
                pcall(require, "missing")
                local _, second_error = pcall(require, "missing")
                return {
                    purpose = "GRAPH",
                    id = "missing-module-test",
                    version = "1.0.0",
                    title = string.find(second_error, "not found", 1, true)
                        and "not found"
                        or second_error,
                }
            """.trimIndent()

            override fun moduleScript(moduleName: String): String? = null
        }
        val engine = uut()
        val lock = engine.acquireVM()

        val result = try {
            engine.runLuaScript(lock, accessor)
        } finally {
            engine.releaseVM(lock)
        }

        assertEquals("not found", result["title"]?.asString())
    }

    @Test
    fun `package entry point and graph can use required modules`() = runBlocking {
        val engine = uut()
        val accessor = ZipLuaGraphScriptAccessor(zip(
            "main.lua" to """
                local renderer = require("renderer")
                return {
                    id = "example.package",
                    version = "1.2.3",
                    title = {
                        en = "Example package",
                        es = "Paquete de ejemplo",
                    },
                    purpose = "GRAPH",
                    type = "CUSTOM",
                    data = function() return { value = 41 } end,
                    render = renderer,
                }
            """.trimIndent(),
            "renderer.lua" to """
                return function(data)
                    return { type = "TEXT", text = tostring(data.value + 1) }
                end
            """.trimIndent(),
        ))

        val scriptLock = engine.acquireVM()
        val descriptor = try {
            engine.runLuaScript(scriptLock, accessor)
        } finally {
            engine.releaseVM(scriptLock)
        }
        assertEquals("example.package", descriptor["id"]?.asString())
        assertEquals("1.2.3", descriptor["version"]?.asString())
        assertEquals("Example package", descriptor["title"]?.get("en")?.asString())
        assertEquals("Paquete de ejemplo", descriptor["title"]?.get("es")?.asString())

        val graphLock = engine.acquireVM()
        val data = try {
            engine.runLuaGraph(
                graphLock,
                accessor,
                LuaGraphEngineParams(emptyMap()),
            ).data as LuaGraphResultData.CustomData
        } finally {
            engine.releaseVM(graphLock)
        }
        val renderLock = engine.acquireVM(data.renderFunction.affinity)
        val rendered = try {
            engine.renderLuaGraph(
                renderLock,
                data.renderFunction,
                data.renderData,
                LuaUiContext(
                    constraints = LuaUiContext.Constraints(0, 100, 0, 100),
                    density = 1f,
                    fontScale = 1f,
                    layoutDirection = LuaUiContext.LayoutDirection.LTR,
                    theme = LuaUiContext.Theme.LIGHT,
                ),
            )
        } finally {
            engine.releaseVM(renderLock)
        }
        assertEquals("42", rendered["text"]?.asString())
    }

    @Test
    fun `retained render uses its own package after another graph runs on the VM`() = runBlocking {
        val engine = uut()
        val accessor = ZipLuaGraphScriptAccessor(zip(
            "main.lua" to """
                return {
                    type = "CUSTOM",
                    data = function() return {} end,
                    render = function() return require("renderer") end,
                }
            """.trimIndent(),
            "renderer.lua" to "return { type = 'TEXT', text = 'package renderer' }",
        ))
        val initialLock = engine.acquireVM()
        val data = try {
            engine.runLuaGraph(
                initialLock,
                accessor,
                LuaGraphEngineParams(emptyMap()),
            ).data as LuaGraphResultData.CustomData
        } finally {
            engine.releaseVM(initialLock)
        }

        val retainedLock = engine.acquireVM(data.renderFunction.affinity)
        val rendered = try {
            engine.runLuaGraph(
                retainedLock,
                "return { type = 'TEXT', text = 'another graph' }",
                LuaGraphEngineParams(emptyMap()),
            )
            engine.renderLuaGraph(
                retainedLock,
                data.renderFunction,
                data.renderData,
                LuaUiContext(
                    LuaUiContext.Constraints(0, 100, 0, 100),
                    1f,
                    1f,
                    LuaUiContext.LayoutDirection.LTR,
                    LuaUiContext.Theme.LIGHT,
                ),
            )
        } finally {
            engine.releaseVM(retainedLock)
        }
        assertEquals("package renderer", rendered["text"]?.asString())
    }

    private fun zip(vararg entries: Pair<String, String>): ByteArray {
        val output = ByteArrayOutputStream()
        ZipOutputStream(output).use { zip ->
            entries.forEach { (name, contents) ->
                zip.putNextEntry(ZipEntry(name))
                zip.write(contents.toByteArray(Charsets.UTF_8))
                zip.closeEntry()
            }
        }
        return output.toByteArray()
    }
}
