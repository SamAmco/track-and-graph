package com.samco.trackandgraph.graphstatinput.configviews.viewmodel

import com.samco.trackandgraph.data.database.dto.LuaGraphFeature
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.util.FeaturePathProvider
import org.junit.Assert.assertEquals
import org.junit.Test
import org.mockito.kotlin.mock
import org.mockito.kotlin.whenever

class LuaGraphFeatureInteropTest {
    @Test
    fun `feature map contains current names and resolved paths keyed by input name`() {
        val pathProvider: FeaturePathProvider = mock()
        whenever(pathProvider.featureName(42)).thenReturn("Steps")
        whenever(pathProvider.getPathForFeature(42)).thenReturn("/Health/Activity/Steps")

        val result = luaGraphFeatureMap(
            listOf(LuaGraphFeature(1, 2, 42, "source")),
            pathProvider,
        )

        assertEquals(42.0, result["source"]?.get("id")?.asDouble())
        assertEquals("FEATURE", result["source"]?.get("type")?.asString())
        assertEquals("Steps", result["source"]?.get("name")?.asString())
        assertEquals("/Health/Activity/Steps", result["source"]?.get("path")?.asString())
    }

    @Test
    fun `feature inputs decode valid bindings and ignore malformed entries`() {
        val inputs = LuaValueSnapshot.ArrayValue(listOf(
            LuaValueSnapshot.TableValue(mapOf(
                "name" to LuaValueSnapshot.StringValue("source"),
                "feature_id" to LuaValueSnapshot.NumberValue(42.0),
            )),
            LuaValueSnapshot.TableValue(mapOf(
                "name" to LuaValueSnapshot.StringValue(""),
                "feature_id" to LuaValueSnapshot.NumberValue(7.0),
            )),
        ))

        assertEquals(
            listOf(LuaGraphFeature(1, 9, 42, "source")),
            luaGraphFeatureInputs(inputs, 9),
        )
    }
}
