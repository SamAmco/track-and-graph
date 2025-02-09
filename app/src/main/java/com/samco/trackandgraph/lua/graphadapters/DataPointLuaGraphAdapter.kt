package com.samco.trackandgraph.lua.graphadapters

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.lua.LuaEngine
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import org.luaj.vm2.LuaValue
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import javax.inject.Inject

class DataPointLuaGraphAdapter @Inject constructor() : LuaGraphAdaptor<LuaGraphResultData.DataPointData> {
    override fun process(scriptResult: LuaValue): LuaGraphResultData.DataPointData? {
        val generatorFunction = scriptResult[LuaEngine.GENERATOR].checkfunction()
            ?: throw IllegalArgumentException("No valid generator function found")
        val result = generatorFunction.call()

        if (result == LuaValue.NIL)  return LuaGraphResultData.DataPointData(
            dataPoint = null,
            isDuration = false
        )

        val timestamp = result[LuaEngine.TIMESTAMP].checklong()

        return LuaGraphResultData.DataPointData(
            dataPoint = DataPoint(
                timestamp = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(timestamp),
                    OffsetDateTime.now().offset
                ),
                featureId = result[LuaEngine.FEATURE_ID].optlong(0L),
                value = result[LuaEngine.VALUE].optdouble(0.0),
                label = result[LuaEngine.LABEL].optjstring("") ?: "",
                note = result[LuaEngine.NOTE].optjstring("") ?: ""

            ),
            isDuration = scriptResult[LuaEngine.IS_DURATION].optboolean(false),
        )
    }
}
