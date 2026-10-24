/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.graphstatinput.configviews.viewmodel

import com.samco.trackandgraph.data.database.dto.LuaGraphFeature
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.util.FeaturePathProvider

internal fun luaGraphFeatureMap(
    features: List<LuaGraphFeature>,
    pathProvider: FeaturePathProvider,
): LuaValueSnapshot = LuaValueSnapshot.TableValue(features.associate { feature ->
    feature.name to LuaValueSnapshot.TableValue(mapOf(
        "id" to LuaValueSnapshot.NumberValue(feature.featureId.toDouble()),
        "type" to LuaValueSnapshot.StringValue("FEATURE"),
        "name" to LuaValueSnapshot.StringValue(
            pathProvider.featureName(feature.featureId).orEmpty(),
        ),
        "path" to LuaValueSnapshot.StringValue(
            pathProvider.getPathForFeature(feature.featureId),
        ),
    ))
})

internal fun luaGraphFeatureInputs(
    inputs: LuaValueAccessor,
    luaGraphId: Long,
): List<LuaGraphFeature> = (1..(inputs.arraySize ?: 0)).mapNotNull { index ->
    val input = inputs[index] ?: return@mapNotNull null
    val name = input["name"]?.asString()?.takeIf(String::isNotBlank)
        ?: return@mapNotNull null
    val featureId = input["feature_id"]?.asDouble()
        ?.takeIf { it.isFinite() && it % 1.0 == 0.0 }
        ?.toLong()
        ?: return@mapNotNull null
    LuaGraphFeature(
        id = index.toLong(),
        luaGraphId = luaGraphId,
        featureId = featureId,
        name = name,
    )
}
