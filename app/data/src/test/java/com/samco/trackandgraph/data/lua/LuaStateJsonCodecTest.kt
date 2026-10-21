package com.samco.trackandgraph.data.lua

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import org.luaj.vm2.lib.ZeroArgFunction

class LuaStateJsonCodecTest {
    @Test
    fun `round trips JSON-compatible nested state`() {
        val state = LuaTable().apply {
            set("name", "calendar")
            set("enabled", LuaValue.valueOf(true))
            set("threshold", 2.5)
            set("values", LuaTable().apply {
                set(1, "first")
                set(2, LuaValue.valueOf(4))
            })
            set("nested", LuaTable().apply { set("value", 7) })
        }

        val encoded = LuaStateJsonCodec.encode(state)
        val decoded = LuaStateJsonCodec.decode(encoded)

        assertEquals("calendar", decoded["name"].tojstring())
        assertTrue(decoded["enabled"].toboolean())
        assertEquals(2.5, decoded["threshold"].todouble(), 0.0)
        assertEquals("first", decoded["values"][1].tojstring())
        assertEquals(4, decoded["values"][2].toint())
        assertEquals(7, decoded["nested"]["value"].toint())
    }

    @Test
    fun `omits functions unsupported keys non-finite numbers and cyclic branches`() {
        val state = LuaTable()
        state.set("kept", "yes")
        state.set("function", object : ZeroArgFunction() {
            override fun call(): LuaValue = LuaValue.NIL
        })
        state.set("infinite", Double.POSITIVE_INFINITY)
        state.set(LuaTable(), LuaValue.valueOf("unsupported key"))
        state.set("cycle", state)

        val encoded = Json.parseToJsonElement(LuaStateJsonCodec.encode(state)) as JsonObject

        assertEquals(JsonPrimitive("yes"), encoded["kept"])
        assertEquals(null, encoded["function"])
        assertEquals(null, encoded["infinite"])
        assertEquals(null, encoded["cycle"])
        assertEquals(1, encoded.size)
    }

    @Test
    fun `dense numeric tables are arrays while empty and mixed tables are objects`() {
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
            Json.parseToJsonElement(LuaStateJsonCodec.encode(dense)),
        )
        assertEquals(JsonObject(emptyMap()), Json.parseToJsonElement(LuaStateJsonCodec.encode(empty)))
        assertEquals(
            JsonObject(mapOf("name" to JsonPrimitive("kept"))),
            Json.parseToJsonElement(LuaStateJsonCodec.encode(mixed)),
        )
    }

    @Test
    fun `JSON null decodes to Lua nil`() {
        assertTrue(LuaStateJsonCodec.decode(null).isnil())
        assertTrue(LuaStateJsonCodec.decode(JsonNull.toString()).isnil())
    }
}
