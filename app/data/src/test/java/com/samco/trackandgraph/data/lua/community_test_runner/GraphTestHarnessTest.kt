/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua.community_test_runner

import com.samco.trackandgraph.data.lua.InlineLuaScriptAccessor
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test

internal class GraphTestHarnessTest : CommunityTestRunner() {
    private val apiUsage by lazy { ApiUsageTracker { requireNotNull(readAssetToString(it)) } }
    override fun getModuleLoadInterceptor() = apiUsage

    private fun run(script: String, test: String) {
        val provider = daggerComponent.provideVMProvider()
        val lease = acquireTestVMLease(provider)
        try {
            apiUsage.reset(lease.globals)
            val harness = GraphTestHarness(
                daggerComponent.provideLuaEngine(), lease, InlineLuaScriptAccessor(script), apiUsage,
                dataPointParser,
            ) { requireNotNull(readAssetToString(it)) }
            lease.globals.load("return function(h) $test end").call().call(harness.api)
        } finally {
            provider.release(lease)
        }
    }

    private fun rejects(script: String, test: String, symbol: String) {
        val failure = assertThrows(Throwable::class.java) { run(script, test) }
        assertTrue(failure.stackTraceToString(), failure.stackTraceToString().contains(symbol))
    }

    @Test
    fun `entry point lookups are validated after its version becomes available`() = rejects(
        "local text = require('tng.ui').text; return { version = '4.0.0', type = 'TEXT' }",
        "", "tng.ui.text",
    )

    @Test
    fun `APIs used only during preparation are checked`() = rejects(
        """return { version = '4.0.0', type = 'TEXT', prepare_data = function()
            require('tng.ui').text('too new'); return { type = 'TEXT', text = 'value' }
        end }""",
        "h:prepare()", "tng.ui.text",
    )

    @Test
    fun `configuration rendering is checked but fixture API calls are excluded`() {
        run("""return { version = '1.0.0', type = 'TEXT',
            prepare_data = function() return { type = 'TEXT', text = 'value' } end }""",
            "require('tng.ui').text({text = 'fixture'}); assert(h:prepare().native ~= nil)")
        rejects("""return { version = '4.0.0', render_configuration = function()
            return { ui = require('tng.ui').text({text = 'too new'}) } end }""",
            "h:configure()", "tng.ui.text")
    }

    @Test
    fun `a callback lookup cannot escape API tracking after the initial render`() = rejects(
        """return { version = '4.0.0', render_configuration = function(params)
            return { ui = { key = 'button', on_click = function()
                require('tng.ui').text({text = 'too new'}) end }, state = {} } end }""",
        "h:configure():invoke('button', 'on_click')", "tng.ui.text",
    )

    @Test
    fun `deferred canvas callbacks are checked when measured and invoked`() = rejects(
        """return { version = '4.0.0', type = 'CUSTOM',
            prepare_data = function() return {} end,
            render_graph = function(params)
                return { ui = { key = 'canvas', draw = function(context)
                    return { color = require('tng.theme').color.primary } end } }
            end }""",
        "h:prepare():render():draw('canvas', { bounds = { width = 100, height = 100 } })",
        "tng.theme.color",
    )

    @Test
    fun `graph preparation failures fail the test rather than becoming successful empty results`() = rejects(
        """return { version = '5.0.0', type = 'TEXT',
            prepare_data = function() error('broken graph') end }""",
        "h:prepare()", "broken graph",
    )
}
