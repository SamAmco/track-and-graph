package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
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
    fun `snapshot and JSON round trip preserve numeric strings as strings`() {
        val strings = listOf("0", "100", "-3.1253", "001", "1e3", "-")
        val state = LuaTable().apply {
            set("fields", LuaTable().apply {
                strings.forEachIndexed { index, text -> set(index + 1, text) }
            })
            set("number", 100.0)
            set("enabled", LuaValue.valueOf(true))
        }

        val snapshot = requireNotNull(LuaStateJsonCodec.snapshot(state))
        val restored = LuaStateJsonCodec.decode(LuaStateJsonCodec.encode(snapshot)).toLuaValue()

        strings.forEachIndexed { index, text ->
            val field = restored.get("fields").get(index + 1)
            assertEquals(text, LuaValue.TSTRING, field.type())
            assertEquals(text, field.tojstring())
        }
        assertEquals(LuaValue.TNUMBER, restored.get("number").type())
        assertEquals(100.0, restored.get("number").todouble(), 0.0)
        assertEquals(LuaValue.TBOOLEAN, restored.get("enabled").type())
        assertTrue(restored.get("enabled").toboolean())
    }

    @Test
    fun `numeric string keys remain distinct object keys`() {
        val keys = listOf("1", "01", "0", "-1", "1.5", "1e3")
        val state = LuaTable().apply {
            keys.forEach { key -> set(key, "value for $key") }
        }

        val snapshot = requireNotNull(LuaStateJsonCodec.snapshot(state))
        val encoded = Json.parseToJsonElement(LuaStateJsonCodec.encode(snapshot))

        assertEquals(JsonObject(keys.associateWith { JsonPrimitive("value for $it") }), encoded)
        val restored = LuaStateJsonCodec.decode(encoded.toString()).toLuaValue()
        keys.forEach { key -> assertEquals("value for $key", restored.get(key).tojstring()) }
        assertTrue(restored.get(1).isnil())
    }

    @Test
    fun `round trips JSON-compatible nested state`() {
        val state = LuaValueSnapshot.TableValue(
            mapOf(
                "name" to LuaValueSnapshot.StringValue("calendar"),
                "enabled" to LuaValueSnapshot.BooleanValue(true),
                "threshold" to LuaValueSnapshot.NumberValue(2.5),
                "values" to LuaValueSnapshot.ArrayValue(
                    listOf(
                        LuaValueSnapshot.StringValue("first"),
                        LuaValueSnapshot.NumberValue(4.0),
                    )
                ),
                "nested" to LuaValueSnapshot.TableValue(
                    mapOf("value" to LuaValueSnapshot.NumberValue(7.0))
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
        assertEquals(LuaValueSnapshot.Nil, LuaStateJsonCodec.decode(JsonNull.toString()))
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
            LuaValueSnapshot.TableValue(
                mapOf("enabled" to LuaValueSnapshot.BooleanValue(true))
            ),
            configuration.value,
        )
        assertTrue(configuration.isInitialized())
    }
}
