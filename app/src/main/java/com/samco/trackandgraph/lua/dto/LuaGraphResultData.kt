package com.samco.trackandgraph.lua.dto

import com.samco.trackandgraph.base.database.dto.DataPoint

sealed interface LuaGraphResultData {
    data class DataPointData(
        val dataPoint: DataPoint?,
        val isDuration: Boolean,
    ) : LuaGraphResultData

    data class TextData(
        val text: String?,
        val size: TextSize,
        val alignment: TextAlignment,
    ) : LuaGraphResultData

    data class PieChartData(
        val segments: List<PieChartSegment>?
    ) : LuaGraphResultData
}