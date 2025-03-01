package com.samco.trackandgraph.lua

import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class LuaEngineSettingsProvider @Inject constructor() {
    var settings: LuaEngineSettings = LuaEngineSettings(enabled = true)
}