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
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class CustomGraphUiTests : LuaEngineImplTest() {
    @Test
    fun `graph UI legend is available as an installed pure Lua module`() = runBlocking {
        val engine = uut()
        val lock = engine.acquireVM()
        try {
            val legend = engine.runLuaScript(
                lock,
                InlineLuaScriptAccessor(
                    """
                    local core = require("tng.core")
                    local graphui = require("tng.graphui")
                    return graphui.legend({
                        items = {{ color = core.COLOR.BLUE, label = "Steps" }},
                    })
                    """.trimIndent(),
                ),
            )

            assertEquals("FLOW_ROW", legend["type"]?.asString())
            assertEquals("CENTER", legend["horizontal_arrangement"]?.asString())
            assertEquals(7.0, legend["children"]?.get(1)?.get("children")?.get(1)
                ?.get("modifier")?.get(3)?.get("color")?.asDouble() ?: -1.0, 0.0)
            assertEquals("Steps", legend["children"]?.get(1)?.get("children")?.get(3)
                ?.get("text")?.asString())
        } finally {
            engine.releaseVM(lock)
        }
    }

    @Test
    fun `retained Lua functions accept arbitrary argument counts`() = runBlocking {
        val engine = uut()
        val lock = engine.acquireVM()
        try {
            val function = requireNotNull(
                engine.runLuaScript(
                    lock,
                    InlineLuaScriptAccessor(
                        "return function(...) return { count = select('#', ...), values = {...} } end"
                    ),
                ).asFunction()
            )

            val noArguments = engine.callLuaFunction(lock, function)
            assertEquals(0.0, noArguments["count"]?.asDouble() ?: -1.0, 0.0)

            val threeArguments = engine.callLuaFunction(
                lock,
                function,
                LuaValueSnapshot.StringValue("first"),
                LuaValueSnapshot.NumberValue(2.0),
                LuaValueSnapshot.BooleanValue(true),
            )
            assertEquals(3.0, threeArguments["count"]?.asDouble() ?: -1.0, 0.0)
            assertEquals("first", threeArguments["values"]?.get(1)?.asString())
            assertEquals(2.0, threeArguments["values"]?.get(2)?.asDouble() ?: 0.0, 0.0)
            assertEquals(true, threeArguments["values"]?.get(3)?.asBoolean())
        } finally {
            engine.releaseVM(lock)
        }
    }

    @Test
    fun `custom UI render and callback failures are reported as Lua script exceptions`() = runBlocking {
        val engine = uut()
        val script = """
            return {
                type = "CUSTOM",
                prepare_data = function() return {} end,
                render_graph = function(params)
                    local context = params.context
                    local state = params.state
                    if context.theme == "DARK" then error("render failed") end
                    return {
                        ui = {
                            type = "CANVAS",
                            draw = function() error("draw failed") end,
                        },
                        state = state,
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
                engine.renderLuaUi(
                    renderLock,
                    customData.renderFunction,
                    customData.renderState,
                    context(LuaUiContext.Theme.DARK),
                    mapOf("data" to customData.renderData),
                )
            }
        } finally {
            engine.releaseVM(renderLock)
        }
        assertTrue(renderError.message.orEmpty().contains("render failed"))

        val successfulRenderLock = engine.acquireVM(customData.renderFunction.affinity)
        val tree = try {
            engine.renderLuaUi(
                successfulRenderLock,
                customData.renderFunction,
                customData.renderState,
                context(LuaUiContext.Theme.LIGHT),
                mapOf("data" to customData.renderData),
            )
        } finally {
            engine.releaseVM(successfulRenderLock)
        }
        val draw = requireNotNull(tree["ui"]?.get("draw")?.asFunction())
        val drawLock = engine.acquireVM(draw.affinity)
        val drawError = try {
            assertThrows(LuaScriptException::class.java) {
                engine.callLuaFunction(drawLock, draw, LuaValueSnapshot.Nil)
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
                prepare_data = function(params) return { value = params.state.seed } end,
                render_graph = function(params)
                    local context = params.context
                    local state = params.state
                    local data = params.data
                    state.renders = state.renders + 1
                    return {
                        ui = setmetatable({
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
                        }),
                        state = state,
                    }
                end,
            }
        """.trimIndent()

        val initialLock = engine.acquireVM()
        val customData = try {
            engine.runLuaGraph(
                initialLock,
                script,
                LuaGraphEngineParams(
                    emptyMap(),
                    LuaStateJsonCodec.decode("{\"seed\":7,\"renders\":0}"),
                ),
            ).data
                as LuaGraphResultData.CustomData
        } finally {
            engine.releaseVM(initialLock)
        }

        val renderLock = engine.acquireVM(customData.renderFunction.affinity)
        val tree = try {
            engine.renderLuaUi(
                renderLock,
                customData.renderFunction,
                customData.renderState,
                LuaUiContext(
                    LuaUiContext.Constraints(0, 1080, 0, 600),
                    density = 3f,
                    fontScale = 1.2f,
                    layoutDirection = LuaUiContext.LayoutDirection.LTR,
                    theme = LuaUiContext.Theme.DARK,
                ),
                mapOf("data" to customData.renderData),
            )
        } finally {
            engine.releaseVM(renderLock)
        }

        val ui = requireNotNull(tree["ui"])
        assertEquals("CANVAS", ui["type"]?.asString())
        assertEquals(7.0, ui["value"]?.asDouble() ?: 0.0, 0.0)
        assertEquals("DARK", ui["theme"]?.asString())
        assertEquals(1080.0, ui["max_width"]?.asDouble() ?: 0.0, 0.0)
        assertNull(ui["not_declared"])
        val state = requireNotNull(tree["state"])
        assertEquals(7.0, state["seed"]?.asDouble() ?: 0.0, 0.0)
        assertEquals(1.0, state["renders"]?.asDouble() ?: 0.0, 0.0)
        assertEquals(
            "{\"seed\":7.0,\"renders\":1.0}",
            LuaStateJsonCodec.encode(requireNotNull(state.snapshot())),
        )

        val draw = requireNotNull(ui["draw"]?.asFunction())
        val drawLock = engine.acquireVM(draw.affinity)
        val instructions = try {
            engine.callLuaFunction(
                drawLock,
                draw,
                LuaValueSnapshot.TableValue(mapOf(
                    "bounds" to LuaValueSnapshot.TableValue(mapOf(
                        "width" to LuaValueSnapshot.NumberValue(200.0),
                        "height" to LuaValueSnapshot.NumberValue(100.0),
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
