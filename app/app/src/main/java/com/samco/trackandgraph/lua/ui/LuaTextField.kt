/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle

@Composable
internal fun LuaTextField(
    node: LuaValueHandle,
    modifier: Modifier,
    actionHandler: (List<LuaUiAction>) -> Unit,
) {
    val callback = node["on_value_change"]?.takeIf { it.asFunction() != null }
    val incoming = LuaTextFieldValue.fromLua(node["value"])
    var nativeValue by rememberSaveable(stateSaver = LuaTextFieldValue.Saver) {
        mutableStateOf(incoming.copy(baseRevision = null))
    }
    fun notifyLua() {
        node.action("on_value_change", nativeValue.snapshot())?.let { actionHandler(listOf(it)) }
    }
    LaunchedEffect(incoming) {
        nativeValue.replaceWith(incoming)?.let { nativeValue = it }
        // Acknowledge applied requests and resynchronise stale Lua state after queued edits or
        // rotation. Comparing public fields ignores the native IME composition range.
        if (nativeValue.needsAcknowledgement(incoming)) notifyLua()
    }
    OutlinedTextField(
        value = nativeValue.value,
        onValueChange = { value ->
            if (value != nativeValue.value) {
                nativeValue = nativeValue.edit(value)
                notifyLua()
            }
        },
        modifier = modifier,
        label = node["label"]?.let { label -> { Text(luaText(label)) } },
        singleLine = true,
        enabled = node["enabled"]?.asBoolean() != false && callback != null,
        isError = node["is_error"]?.asBoolean() == true,
        keyboardOptions = KeyboardOptions(keyboardType = when (node["keyboard_type"]?.asString()) {
            "NUMBER" -> KeyboardType.Number
            "DECIMAL" -> KeyboardType.Decimal
            else -> KeyboardType.Text
        }),
    )
}
