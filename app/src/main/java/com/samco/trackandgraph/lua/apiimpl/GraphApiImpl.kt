package com.samco.trackandgraph.lua.apiimpl

import com.samco.trackandgraph.lua.LuaEngine
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.LuaValue.Companion.NIL
import org.luaj.vm2.LuaValue.Companion.valueOf
import javax.inject.Inject

class GraphApiImpl @Inject constructor(
    private val dateTimeParser: DateTimeParser,
    private val dataPointParser: DataPointParser,
) {

    companion object {
        const val GRAPH = "graph"
        const val SOURCES = "sources"
        const val DP = "dp"
        const val DP_BATCH = "dpbatch"
        const val DP_ALL = "dpall"
        const val DP_AFTER = "dpafter"
    }

    fun installIn(table: LuaTable, sources: LuaEngine.LuaGraphEngineParams) = table[GRAPH].apply {
        overrideOrThrow(SOURCES, getSourcesLuaFunction(sources))
        overrideOrThrow(DP, getDpLuaFunction(sources))
        overrideOrThrow(DP_BATCH, getDpBatchLuaFunction(sources))
        overrideOrThrow(DP_ALL, getDpAllLuaFunction(sources))
        overrideOrThrow(DP_AFTER, getDpAfterLuaFunction(sources))
    }

    private fun getDpAfterLuaFunction(sources: LuaEngine.LuaGraphEngineParams) = twoArgFunction { arg1, arg2 ->
        val name = arg1.checkjstring()
            ?: throw IllegalArgumentException("Name must be provided and be a string")
        val zonedDateTime = dateTimeParser.parseDateTime(arg2)
        val batch = sources.dataSources[name]
            ?.takeWhile {  it.timestamp > zonedDateTime.toOffsetDateTime()  }
            ?.map { dataPointParser.toLuaValueNullable(it) }
            ?.toList()
            ?.toTypedArray()
        return@twoArgFunction batch?.let { LuaValue.listOf(batch) } ?: NIL
    }

    private fun getDpAllLuaFunction(sources: LuaEngine.LuaGraphEngineParams): LuaValue = oneArgFunction { arg: LuaValue ->
        val name = arg.checkjstring()
            ?: throw IllegalArgumentException("Name must be provided and be a string")
        val batch = sources.dataSources[name]?.map { dataPointParser.toLuaValueNullable(it) }?.toList()
        return@oneArgFunction batch?.let { LuaValue.listOf(it.toTypedArray()) } ?: NIL
    }

    private fun getSourcesLuaFunction(sources: LuaEngine.LuaGraphEngineParams): LuaValue = zeroArgFunction {
        return@zeroArgFunction LuaValue.listOf(sources.dataSources.keys.map { valueOf(it) }.toTypedArray())
    }

    private fun getDpBatchLuaFunction(sources: LuaEngine.LuaGraphEngineParams): LuaValue = twoArgFunction { arg1, arg2 ->
        val name = arg1.checkjstring()
            ?: throw IllegalArgumentException("Name must be provided and be a string")
        val count = arg2.optint(1)
        val batch = sources.dataSources[name]?.take(count)?.map { dataPointParser.toLuaValueNullable(it) }?.toList()
        return@twoArgFunction batch?.let { LuaValue.listOf(it.toTypedArray()) } ?: NIL
    }

    private fun getDpLuaFunction(sources: LuaEngine.LuaGraphEngineParams): LuaValue = oneArgFunction { arg ->
        val name = arg.checkjstring()
            ?: throw IllegalArgumentException("Name must be provided and be a string")
        return@oneArgFunction dataPointParser.toLuaValueNullable(sources.dataSources[name]?.first())
    }

}
