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
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot

@Composable
internal fun LuaTextField(
    node: LuaValueHandle,
    modifier: Modifier,
    actionHandler: (List<LuaUiAction>) -> Unit,
) {
    val callback = node["on_value_change"]?.takeIf { it.asFunction() != null }
    OutlinedTextField(
        value = node["value"]?.asString().orEmpty(),
        onValueChange = { value ->
            node.action("on_value_change", LuaValueSnapshot.StringValue(value))
                ?.let { actionHandler(listOf(it)) }
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
