package com.samco.trackandgraph.data.lua.dto

import com.samco.trackandgraph.data.sampling.RawDataSample

data class LuaGraphEngineParams(
    val dataSources: Map<String, RawDataSample>
)