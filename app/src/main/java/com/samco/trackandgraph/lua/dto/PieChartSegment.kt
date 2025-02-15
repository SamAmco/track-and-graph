package com.samco.trackandgraph.lua.dto

data class PieChartSegment(
    val value: Double,
    val label: String,
    val color: ColorSpec?
)