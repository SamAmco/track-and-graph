package com.samco.trackandgraph.lua

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.lua.dto.LuaGraphResult

interface LuaEngine {
    companion object {
        const val TNG = "Tng"
        const val GRAPH = "graph"
        const val NEXT = "next"
        const val NEXT_BATCH = "nextBatch"
        const val GENERATOR = "generator"
        const val TYPE = "type"
        const val DATAPOINT = "datapoint"
        const val IS_DURATION = "isDuration"
        const val TIMESTAMP = "timestamp"
        const val FEATURE_ID = "featureId"
        const val VALUE = "value"
        const val LABEL = "label"
        const val NOTE = "note"
    }


    fun runLuaGraphScript(
        script: String,
        next: (String, Int) -> List<DataPoint>
    ): LuaGraphResult
}
