/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import com.samco.trackandgraph.data.lua.dto.LuaValueData
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

/** Converts immutable Lua-shaped data to and from its persisted JSON representation. */
internal object LuaStateJsonCodec {
    private val json = Json

    fun decodeConfiguration(encoded: String?): Lazy<LuaValueData>? = encoded?.let {
        lazy { decode(it) }
    }

    fun encode(configuration: Lazy<LuaValueData>?): String? =
        configuration?.value?.let(::encode)

    fun encode(value: LuaValueData): String = json.encodeToString(
        JsonElement.serializer(),
        encodeValue(value) ?: JsonNull,
    )

    fun decode(encoded: String): LuaValueData = decode(Json.parseToJsonElement(encoded))

    fun snapshot(value: LuaValue): LuaValueData? {
        if (value.isnil()) return null
        require(value.istable()) { "Lua UI state must be a table or nil" }
        val visiting = Collections.newSetFromMap(IdentityHashMap<LuaValue, Boolean>())
        return requireNotNull(snapshotTable(value.checktable()!!, visiting))
    }

    private fun encodeValue(value: LuaValueData): JsonElement? = when (value) {
        LuaValueData.Nil -> JsonNull
        is LuaValueData.BooleanValue -> JsonPrimitive(value.value)
        is LuaValueData.NumberValue -> value.value.takeIf(Double::isFinite)?.let(::JsonPrimitive)
        is LuaValueData.StringValue -> JsonPrimitive(value.value)
        is LuaValueData.ArrayValue -> JsonArray(value.values.map { encodeValue(it) ?: JsonNull })
        is LuaValueData.TableValue -> JsonObject(
            value.values.mapNotNull { (key, entry) -> encodeValue(entry)?.let { key to it } }
                .toMap(linkedMapOf())
        )
    }

    private fun decode(element: JsonElement): LuaValueData = when (element) {
        JsonNull -> LuaValueData.Nil
        is JsonArray -> LuaValueData.ArrayValue(element.map(::decode))
        is JsonObject -> LuaValueData.TableValue(element.mapValues { decode(it.value) })
        is JsonPrimitive -> when {
            element.isString -> LuaValueData.StringValue(element.content)
            element.booleanOrNull != null -> LuaValueData.BooleanValue(element.booleanOrNull!!)
            element.doubleOrNull != null -> LuaValueData.NumberValue(element.doubleOrNull!!)
            else -> LuaValueData.Nil
        }
    }

    private fun snapshotValue(
        value: LuaValue,
        visiting: MutableSet<LuaValue>,
    ): LuaValueData? = when {
        value.isnil() -> LuaValueData.Nil
        value.isboolean() -> LuaValueData.BooleanValue(value.toboolean())
        value.isnumber() -> value.todouble().takeIf(Double::isFinite)
            ?.let(LuaValueData::NumberValue)
        value.isstring() -> LuaValueData.StringValue(value.tojstring())
        value.istable() -> snapshotTable(value.checktable()!!, visiting)
        else -> null
    }

    private fun snapshotTable(
        table: LuaTable,
        visiting: MutableSet<LuaValue>,
    ): LuaValueData? {
        if (!visiting.add(table)) return null
        return try {
            val strings = linkedMapOf<String, LuaValue>()
            val integers = sortedMapOf<Int, LuaValue>()
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
                        if (integer > 0 && number == integer.toDouble()) integers[integer] = value
                    }
                    key.isstring() -> strings[key.tojstring()] = value
                }
            }

            if (strings.isEmpty() && integers.isNotEmpty() &&
                integers.keys.toList() == (1..integers.size).toList()
            ) {
                LuaValueData.ArrayValue(
                    integers.values.map { snapshotValue(it, visiting) ?: LuaValueData.Nil }
                )
            } else {
                LuaValueData.TableValue(
                    strings.mapNotNull { (entryKey, entryValue) ->
                        snapshotValue(entryValue, visiting)?.let { entryKey to it }
                    }.toMap(linkedMapOf())
                )
            }
        } finally {
            visiting.remove(table)
        }
    }
}
