/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle

@Composable
internal fun LuaDropdownMenu(
    node: LuaValueHandle,
    modifier: Modifier,
    actionHandler: (List<LuaUiAction>) -> Unit,
    renderNode: @Composable (LuaValueHandle) -> Unit,
) {
    DropdownMenu(
        expanded = node["expanded"]?.asBoolean() == true,
        onDismissRequest = {
            node.action("on_dismiss_request")?.let { actionHandler(listOf(it)) }
        },
        modifier = modifier,
    ) {
        node["children"].toHandleList().forEach { renderNode(it) }
    }
}

@Composable
internal fun LuaDropdownMenuItem(
    node: LuaValueHandle,
    modifier: Modifier,
    actionHandler: (List<LuaUiAction>) -> Unit,
    renderNode: @Composable (LuaValueHandle) -> Unit,
) {
    DropdownMenuItem(
        text = { node["child"]?.let { renderNode(it) } },
        onClick = {
            node.action("on_click")?.let { actionHandler(listOf(it)) }
        },
        modifier = modifier,
        enabled = node["enabled"]?.asBoolean() != false,
    )
}
