/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import com.samco.trackandgraph.data.database.dto.LuaGraphSource
import com.samco.trackandgraph.data.lua.InlineLuaScriptAccessor
import com.samco.trackandgraph.data.lua.LuaEngine
import com.samco.trackandgraph.data.lua.LuaScriptAccessor
import com.samco.trackandgraph.data.lua.ZipLuaScriptAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import javax.inject.Inject

/** Resolves the optional configuration renderer without exposing how the script is stored. */
class LuaGraphConfigurationProvider @Inject constructor(
    private val luaEngine: LuaEngine,
) {
    suspend operator fun invoke(
        source: LuaGraphSource,
        configuration: Lazy<LuaValueSnapshot>?,
        featureMap: LuaValueSnapshot,
    ): LuaUiContent? {
        val vmLock = luaEngine.acquireVM()
        return try {
            val descriptor = luaEngine.runLuaScript(vmLock, source.scriptAccessor())
            val renderer = descriptor[RENDER_CONFIGURATION]?.asFunction() ?: return null
            RetainedLuaUiContent(
                luaEngine = luaEngine,
                renderFunction = renderer,
                initialState = configuration?.value,
                additionalRenderParams = mapOf(FEATURE_MAP to featureMap),
            )
        } finally {
            luaEngine.releaseVM(vmLock)
        }
    }

    private fun LuaGraphSource.scriptAccessor(): LuaScriptAccessor = when (this) {
        is LuaGraphSource.Inline -> InlineLuaScriptAccessor(script)
        is LuaGraphSource.Package -> ZipLuaScriptAccessor(packageData.archive)
    }

    private companion object {
        const val RENDER_CONFIGURATION = "render_configuration"
        const val FEATURE_MAP = "feature_map"
    }
}
