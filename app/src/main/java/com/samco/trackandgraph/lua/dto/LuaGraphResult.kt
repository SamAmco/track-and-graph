package com.samco.trackandgraph.lua.dto

data class LuaGraphResult(
    val data: LuaGraphResultData? = null,
    val error: Throwable? = null,
)