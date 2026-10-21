/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import kotlinx.serialization.json.Json
import kotlinx.serialization.json.JsonArray
import kotlinx.serialization.json.JsonElement
import kotlinx.serialization.json.JsonNull
import kotlinx.serialization.json.JsonObject
import kotlinx.serialization.json.JsonPrimitive
import kotlinx.serialization.json.booleanOrNull
import kotlinx.serialization.json.doubleOrNull
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import java.util.Collections
import java.util.IdentityHashMap

/** Converts the JSON-compatible subset of a Lua state table without invoking metamethods. */
internal object LuaStateJsonCodec {
    private val json = Json

    fun encode(value: LuaValue): String {
        require(value.istable()) { "Lua UI state must be a table" }
        val visiting = Collections.newSetFromMap(IdentityHashMap<LuaValue, Boolean>())
        val encoded = requireNotNull(encodeTable(value.checktable()!!, visiting))
        return json.encodeToString(JsonElement.serializer(), encoded)
    }

    fun decode(encoded: String?): LuaValue =
        encoded?.let { decode(json.parseToJsonElement(it)) } ?: LuaValue.NIL

    private fun encodeValue(
        value: LuaValue,
        visiting: MutableSet<LuaValue>,
    ): JsonElement? = when {
        value.isnil() -> JsonNull
        value.isboolean() -> JsonPrimitive(value.toboolean())
        value.isnumber() -> value.todouble().takeIf(Double::isFinite)?.let(::JsonPrimitive)
        value.isstring() -> JsonPrimitive(value.tojstring())
        value.istable() -> value.checktable()?.let { encodeTable(it, visiting) }
        else -> null
    }

    private fun encodeTable(
        table: LuaTable,
        visiting: MutableSet<LuaValue>,
    ): JsonElement? {
        if (!visiting.add(table)) return null
        return try {
            val stringEntries = linkedMapOf<String, LuaValue>()
            val integerEntries = sortedMapOf<Int, LuaValue>()
            var key: LuaValue = LuaValue.NIL
            while (true) {
                val next = table.next(key)
                key = next.arg1()
                if (key.isnil()) break
                val value = next.arg(2)
                when {
                    key.isnumber() && key.todouble().isFinite() -> {
                        val number = key.todouble()
                        val integer = number.toInt()
                        if (integer > 0 && number == integer.toDouble()) {
                            integerEntries[integer] = value
                        }
                    }
                    key.isstring() -> stringEntries[key.tojstring()] = value
                }
            }

            if (stringEntries.isEmpty() && integerEntries.isNotEmpty() &&
                integerEntries.keys.toList() == (1..integerEntries.size).toList()
            ) {
                JsonArray(integerEntries.values.map { encodeValue(it, visiting) ?: JsonNull })
            } else {
                JsonObject(stringEntries.mapNotNull { (entryKey, entryValue) ->
                    encodeValue(entryValue, visiting)?.let { entryKey to it }
                }.toMap(linkedMapOf()))
            }
        } finally {
            visiting.remove(table)
        }
    }

    private fun decode(element: JsonElement): LuaValue = when (element) {
        JsonNull -> LuaValue.NIL
        is JsonArray -> LuaTable().also { table ->
            element.forEachIndexed { index, value -> table.rawset(index + 1, decode(value)) }
        }
        is JsonObject -> LuaTable().also { table ->
            element.forEach { (key, value) -> table.rawset(key, decode(value)) }
        }
        is JsonPrimitive -> when {
            element.isString -> LuaValue.valueOf(element.content)
            element.booleanOrNull != null -> LuaValue.valueOf(element.booleanOrNull!!)
            element.doubleOrNull != null -> LuaValue.valueOf(element.doubleOrNull!!)
            else -> LuaValue.NIL
        }
    }
}
