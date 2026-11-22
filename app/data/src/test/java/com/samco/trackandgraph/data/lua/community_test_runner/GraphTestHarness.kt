/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua.community_test_runner

import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaScriptAccessor
import com.samco.trackandgraph.data.lua.LuaStateJsonCodec
import com.samco.trackandgraph.data.lua.LuaValueHandleImpl
import com.samco.trackandgraph.data.lua.VMLease
import com.samco.trackandgraph.data.lua.apiimpl.DataPointParser
import com.samco.trackandgraph.data.lua.dto.LuaGraphEngineParams
import com.samco.trackandgraph.data.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.data.lua.dto.LuaUiContext
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.data.lua.toLuaValue
import com.samco.trackandgraph.data.lua.valueFor
import com.samco.trackandgraph.data.sampling.RawDataSample
import io.github.z4kn4fein.semver.toVersion
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaUserdata
import org.luaj.vm2.LuaValue
import org.luaj.vm2.Varargs
import org.luaj.vm2.lib.VarArgFunction

/** Real-engine operations exposed only to Lua tests; the runner owns one lease per test file. */
internal class GraphTestHarness(
    private val engine: LuaEngine,
    private val lease: VMLease,
    private val accessor: LuaScriptAccessor,
    private val apiUsage: ApiUsageTracker,
    private val dataPointParser: DataPointParser,
    readResource: (String) -> String,
) {
    private val descriptor = apiUsage.track(lease.globals) { engine.runLuaScript(lease, accessor) }
    private val major = requireNotNull(descriptor["version"]?.asString()) { "Graph needs a version" }.toVersion().major

    init { apiUsage.validate(lease.globals, major) }

    private fun <T> execute(block: () -> T): T = apiUsage.track(lease.globals, block).also {
        apiUsage.validate(lease.globals, major)
    }

    private fun handle(value: LuaValue) = LuaValueHandleImpl(value, lease.affinity)
    private fun snapshot(value: LuaValue): LuaValueSnapshot? = handle(value).snapshot()

    val api: LuaValue = run {
        val native = LuaTable()
        native["descriptor"] = descriptor.valueFor(lease)
        native["prepare"] = function { args ->
            val params = args.arg1()
            val sourceTable = params["sources"]
            val sources = if (sourceTable.isnil()) emptyMap() else {
                sourceTable.checktable()!!.keys().sortedBy { it.tojstring() }.associate { key ->
                    val rows = sourceTable[key].checktable()!!
                    val points = (1..rows.rawlen()).map { dataPointParser.parseDataPoint(rows[it]) }
                    val visited = mutableListOf<DataPoint>()
                    key.checkjstring()!! to RawDataSample.fromSequence(
                        points.asSequence().onEach { visited.add(it) }, { visited }, {},
                    )
                }
            }
            val result = execute {
                engine.runLuaGraph(lease, accessor, LuaGraphEngineParams(sources, snapshot(params["state"])))
            }
            result.error?.let { throw AssertionError("Graph preparation failed", it) }
            LuaTable().apply {
                when (val data = result.data) {
                    null -> set("empty", LuaValue.valueOf(true))
                    is LuaGraphResultData.CustomData -> {
                        set("data", data.renderData.valueFor(lease))
                        set("renderer", data.renderFunction.valueFor(lease))
                        set("state", data.renderState?.valueFor(lease) ?: LuaValue.NIL)
                    }
                    // Native DTO details remain covered by Kotlin graph adapter tests.
                    else -> set("native", LuaUserdata(data))
                }
            }
        }
        native["render"] = function { args ->
            val context = args.arg(3)
            val constraints = context["constraints"].takeUnless { it.isnil() } ?: LuaTable()
            val uiContext = LuaUiContext(
                constraints = LuaUiContext.Constraints(
                    constraints["min_width"].optint(0), constraints["max_width"].optint(800),
                    constraints["min_height"].optint(0), constraints["max_height"].optint(1200),
                ),
                density = context["density"].optdouble(1.0).toFloat(),
                fontScale = context["font_scale"].optdouble(1.0).toFloat(),
                layoutDirection = LuaUiContext.LayoutDirection.valueOf(context["layout_direction"].optjstring("LTR")!!),
                theme = LuaUiContext.Theme.valueOf(context["theme"].optjstring("LIGHT")!!),
            )
            val additional = args.arg(4).checktable()!!.let { table ->
                table.keys().associate { it.checkjstring()!! to handle(table[it]) }
            }
            execute { engine.renderLuaUi(lease, handle(args.arg1()), handle(args.arg(2)), uiContext, additional) }
                .valueFor(lease)
        }
        native["call"] = function { args ->
            val arguments = (2..args.narg()).map { handle(args.arg(it)) }.toTypedArray()
            execute { engine.callLuaFunction(lease, handle(args.arg1()), *arguments) }.valueFor(lease)
        }
        native["round_trip"] = function { args ->
            snapshot(args.arg1())?.let { value ->
                LuaStateJsonCodec.decode(LuaStateJsonCodec.encode(value)).toLuaValue()
            } ?: LuaValue.NIL
        }
        lease.globals.load(readResource("generated/lua-community/test-utils/graph-harness.lua"))
            .call().call(native)
    }

    private fun function(block: (Varargs) -> LuaValue) = object : VarArgFunction() {
        override fun invoke(args: Varargs): Varargs = block(args)
    }
}
