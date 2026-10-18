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
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaScriptException
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueData
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class CustomGraphUiTests : LuaEngineImplTest() {
    @Test
    fun `custom UI render and callback failures are reported as Lua script exceptions`() = runBlocking {
        val engine = uut()
        val script = """
            return {
                type = "CUSTOM",
                data = function() return {} end,
                render = function(_, context)
                    if context.theme == "DARK" then error("render failed") end
                    return {
                        type = "CANVAS",
                        draw = function() error("draw failed") end,
                    }
                end,
            }
        """.trimIndent()
        val initialLock = engine.acquireVM()
        val customData = try {
            engine.runLuaGraph(initialLock, script, LuaGraphEngineParams(emptyMap())).data
                as LuaGraphResultData.CustomData
        } finally {
            engine.releaseVM(initialLock)
        }

        val renderLock = engine.acquireVM(customData.renderFunction.affinity)
        val renderError = try {
            assertThrows(LuaScriptException::class.java) {
                engine.renderLuaGraph(
                    renderLock,
                    customData.renderFunction,
                    customData.renderData,
                    context(LuaUiContext.Theme.DARK),
                )
            }
        } finally {
            engine.releaseVM(renderLock)
        }
        assertTrue(renderError.message.orEmpty().contains("render failed"))

        val successfulRenderLock = engine.acquireVM(customData.renderFunction.affinity)
        val tree = try {
            engine.renderLuaGraph(
                successfulRenderLock,
                customData.renderFunction,
                customData.renderData,
                context(LuaUiContext.Theme.LIGHT),
            )
        } finally {
            engine.releaseVM(successfulRenderLock)
        }
        val draw = requireNotNull(tree["draw"]?.asFunction())
        val drawLock = engine.acquireVM(draw.affinity)
        val drawError = try {
            assertThrows(LuaScriptException::class.java) {
                engine.callLuaUiFunction(drawLock, draw, LuaValueData.Nil)
            }
        } finally {
            engine.releaseVM(drawLock)
        }
        assertTrue(drawError.message.orEmpty().contains("draw failed"))
        Unit
    }

    @Test
    fun `engine rejects a released VM lease`() = runBlocking {
        val engine = uut()
        val lease = engine.acquireVM()
        engine.releaseVM(lease)

        assertThrows(IllegalStateException::class.java) {
            engine.runLuaGraph(
                lease,
                "return { type = 'TEXT', text = 'invalid' }",
                LuaGraphEngineParams(emptyMap()),
            )
        }
        Unit
    }

    @Test
    fun `custom UI render and retained callback stay on their owning VM`() = runBlocking {
        val engine = uut()
        val script = """
            local canvas = require("tng.canvas")
            local graph = require("tng.graph")
            local theme = require("tng.theme")
            local ui = require("tng.ui")

            return {
                type = graph.GRAPH_TYPE.CUSTOM,
                data = function() return { value = 7 } end,
                render = function(data, context)
                    return setmetatable({
                        type = canvas.TYPE,
                        value = data.value,
                        theme = context.theme,
                        max_width = context.constraints.max_width,
                        draw = function(canvas_context)
                            return {
                                type = canvas.TYPE,
                                instructions = {{
                                    type = "CIRCLE",
                                    color = theme.color.primary,
                                    radius = canvas_context.bounds.width / 2,
                                    center = { x = 5, y = 6 },
                                }},
                            }
                        end,
                    }, {
                        __index = function(_, key) error("unexpected key: " .. key) end,
                    })
                end,
            }
        """.trimIndent()

        val initialLock = engine.acquireVM()
        val customData = try {
            engine.runLuaGraph(initialLock, script, LuaGraphEngineParams(emptyMap())).data
                as LuaGraphResultData.CustomData
        } finally {
            engine.releaseVM(initialLock)
        }

        val renderLock = engine.acquireVM(customData.renderFunction.affinity)
        val tree = try {
            engine.renderLuaGraph(
                renderLock,
                customData.renderFunction,
                customData.renderData,
                LuaUiContext(
                    LuaUiContext.Constraints(0, 1080, 0, 600),
                    density = 3f,
                    fontScale = 1.2f,
                    layoutDirection = LuaUiContext.LayoutDirection.LTR,
                    theme = LuaUiContext.Theme.DARK,
                ),
            )
        } finally {
            engine.releaseVM(renderLock)
        }

        assertEquals("CANVAS", tree["type"]?.asString())
        assertEquals(7.0, tree["value"]?.asDouble() ?: 0.0, 0.0)
        assertEquals("DARK", tree["theme"]?.asString())
        assertEquals(1080.0, tree["max_width"]?.asDouble() ?: 0.0, 0.0)
        assertNull(tree["not_declared"])

        val draw = requireNotNull(tree["draw"]?.asFunction())
        val drawLock = engine.acquireVM(draw.affinity)
        val instructions = try {
            engine.callLuaUiFunction(
                drawLock,
                draw,
                LuaValueData.TableValue(mapOf(
                    "bounds" to LuaValueData.TableValue(mapOf(
                        "width" to LuaValueData.NumberValue(200.0),
                        "height" to LuaValueData.NumberValue(100.0),
                    )),
                )),
            )
        } finally {
            engine.releaseVM(drawLock)
        }

        assertEquals(100.0, instructions["instructions"]?.get(1)?.get("radius")?.asDouble() ?: 0.0, 0.0)
    }

    private fun context(theme: LuaUiContext.Theme) = LuaUiContext(
        constraints = LuaUiContext.Constraints(0, 1080, 0, 600),
        density = 3f,
        fontScale = 1.2f,
        layoutDirection = LuaUiContext.LayoutDirection.LTR,
        theme = theme,
    )
}
