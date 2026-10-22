package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaValueData
import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertThrows
import org.junit.Assert.assertTrue
import org.junit.Test
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

class LuaStateJsonCodecTest {
    @Test
    fun `round trips JSON-compatible nested state`() {
        val state = LuaValueData.TableValue(
            mapOf(
                "name" to LuaValueData.StringValue("calendar"),
                "enabled" to LuaValueData.BooleanValue(true),
                "threshold" to LuaValueData.NumberValue(2.5),
                "values" to LuaValueData.ArrayValue(
                    listOf(
                        LuaValueData.StringValue("first"),
                        LuaValueData.NumberValue(4.0),
                    )
                ),
                "nested" to LuaValueData.TableValue(
                    mapOf("value" to LuaValueData.NumberValue(7.0))
                ),
            )
        )

        assertEquals(state, LuaStateJsonCodec.decode(LuaStateJsonCodec.encode(state)))
    }

    @Test
    fun `snapshot omits functions unsupported keys non-finite numbers and cyclic branches`() {
        val state = LuaTable()
        state.set("kept", "yes")
        state.set("function", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.NIL
        })
        state.set("infinite", Double.POSITIVE_INFINITY)
        state.set(LuaTable(), LuaValue.valueOf("unsupported key"))
        state.set("cycle", state)

        val snapshot = requireNotNull(LuaStateJsonCodec.snapshot(state))
        val encoded = Json.parseToJsonElement(LuaStateJsonCodec.encode(snapshot)) as JsonObject

        assertEquals(JsonPrimitive("yes"), encoded["kept"])
        assertEquals(null, encoded["function"])
        assertEquals(null, encoded["infinite"])
        assertEquals(null, encoded["cycle"])
        assertEquals(1, encoded.size)
    }

    @Test
    fun `dense numeric tables snapshot as arrays while empty and mixed tables snapshot as objects`() {
        val dense = LuaTable().apply {
            set(1, "a")
            set(2, object : ZeroArgFunction() {
                override fun call(): LuaValue = LuaValue.NIL
            })
            set(3, "c")
        }
        val empty = LuaTable()
        val mixed = LuaTable().apply {
            set(1, "ignored")
            set("name", "kept")
        }

        assertEquals(
            JsonArray(listOf(JsonPrimitive("a"), JsonNull, JsonPrimitive("c"))),
            Json.parseToJsonElement(
                LuaStateJsonCodec.encode(requireNotNull(LuaStateJsonCodec.snapshot(dense)))
            ),
        )
        assertEquals(
            JsonObject(emptyMap()),
            Json.parseToJsonElement(
                LuaStateJsonCodec.encode(requireNotNull(LuaStateJsonCodec.snapshot(empty)))
            ),
        )
        assertEquals(
            JsonObject(mapOf("name" to JsonPrimitive("kept"))),
            Json.parseToJsonElement(
                LuaStateJsonCodec.encode(requireNotNull(LuaStateJsonCodec.snapshot(mixed)))
            ),
        )
    }

    @Test
    fun `JSON null decodes to immutable nil`() {
        assertEquals(LuaValueData.Nil, LuaStateJsonCodec.decode(JsonNull.toString()))
    }

    @Test
    fun `stored configuration is decoded only when accessed`() {
        val configuration = LuaStateJsonCodec.decodeConfiguration("{not valid JSON")

        assertNotNull(configuration)
        assertFalse(configuration!!.isInitialized())
        assertThrows(Exception::class.java) { configuration.value }
    }

    @Test
    fun `valid stored configuration remains lazy until read`() {
        val configuration = requireNotNull(
            LuaStateJsonCodec.decodeConfiguration("{\"enabled\":true}")
        )

        assertFalse(configuration.isInitialized())
        assertEquals(
            LuaValueData.TableValue(
                mapOf("enabled" to LuaValueData.BooleanValue(true))
            ),
            configuration.value,
        )
        assertTrue(configuration.isInitialized())
    }
}
