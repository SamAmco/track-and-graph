package com.samco.trackandgraph.lua.dto

sealed class ColorSpec {
    data class HexColor(val hexString: String) : ColorSpec()
    data class ColorIndex(val index: Int) : ColorSpec()
}