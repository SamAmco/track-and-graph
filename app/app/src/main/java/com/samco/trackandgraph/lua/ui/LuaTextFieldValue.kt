/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import androidx.compose.runtime.saveable.listSaver
import androidx.compose.ui.text.TextRange
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot

/**
 * A detached bridge value, also used as the immediately updated native editing state.
 * Native state has no [baseRevision]. Lua mutations carry the revision they read, so a delayed
 * render cannot replace newer typing. Selection uses zero-based UTF-16 offsets in both directions.
 * IME composition stays native: ordinary Lua echoes never replace it.
 */
internal data class LuaTextFieldValue(
    val value: TextFieldValue,
    val revision: Long,
    val baseRevision: Long? = null,
) {
    fun edit(value: TextFieldValue): LuaTextFieldValue {
        check(revision < MAX_REVISION) { "Text field revision exhausted" }
        return LuaTextFieldValue(value, revision + 1)
    }

    fun replaceWith(request: LuaTextFieldValue): LuaTextFieldValue? {
        if (request.baseRevision != revision || request.revision <= revision) return null
        return LuaTextFieldValue(
            // A text replacement commits any old IME composition. Cursor-only changes preserve it.
            request.value.copy(composition = value.composition.takeIf { value.text == request.value.text }),
            request.revision,
        )
    }

    fun needsAcknowledgement(incoming: LuaTextFieldValue): Boolean =
        incoming.baseRevision != null || incoming.revision != revision ||
            incoming.value.text != value.text || incoming.value.selection != value.selection

    /** Fresh plain data for Lua; acknowledgements clear pending replacement metadata. */
    fun snapshot(): LuaValueSnapshot = LuaValueSnapshot.TableValue(mapOf(
        "text" to LuaValueSnapshot.StringValue(value.text),
        "selection" to LuaValueSnapshot.TableValue(mapOf(
            "start" to LuaValueSnapshot.NumberValue(value.selection.start.toDouble()),
            "end" to LuaValueSnapshot.NumberValue(value.selection.end.toDouble()),
        )),
        "revision" to LuaValueSnapshot.NumberValue(revision.toDouble()),
    ))

    companion object {
        private const val MAX_REVISION = 9_007_199_254_740_991L

        // Save the native revision together with text/selection. On rotation it may be ahead of
        // Lua because an event was still waiting for the VM lock. Composition is IME-owned.
        val Saver = listSaver<LuaTextFieldValue, Any>(
            save = { listOf(it.value.text, it.value.selection.start, it.value.selection.end, it.revision) },
            restore = {
                LuaTextFieldValue(
                    TextFieldValue(it[0] as String, TextRange(it[1] as Int, it[2] as Int)),
                    it[3] as Long,
                )
            },
        )

        fun fromLua(value: LuaValueAccessor?): LuaTextFieldValue {
            val text = value?.get("text")?.asString().orEmpty()
            fun offset(key: String): Int = value?.get("selection")?.get(key)?.asDouble()
                ?.takeIf { it.isFinite() }
                ?.coerceIn(0.0, text.length.toDouble())?.toInt() ?: text.length
            return LuaTextFieldValue(
                TextFieldValue(text, TextRange(offset("start"), offset("end"))),
                revision = value?.get("revision").revision() ?: 0,
                baseRevision = value?.get("base_revision").revision(),
            )
        }

        private fun LuaValueAccessor?.revision(): Long? = this?.asDouble()
            ?.takeIf { it.isFinite() && it >= 0 && it <= MAX_REVISION && it % 1.0 == 0.0 }
            ?.toLong()
    }
}
