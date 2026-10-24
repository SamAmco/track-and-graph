/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import androidx.compose.runtime.Composable
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import com.samco.trackandgraph.data.lua.dto.LuaValueAccessor
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.selectitemdialog.HiddenItem
import com.samco.trackandgraph.selectitemdialog.SelectItemDialog
import com.samco.trackandgraph.selectitemdialog.SelectItemDialogViewModelImpl
import com.samco.trackandgraph.selectitemdialog.SelectableItemType
import com.samco.trackandgraph.selectitemdialog.SelectedItemInfo

@Composable
internal fun LuaSelectItemDialog(
    node: LuaValueHandle,
    actionHandler: (List<LuaUiAction>) -> Unit,
) {
    if (node["visible"]?.asBoolean() != true) return

    val selectableTypes = node["selectable_types"].toSelectableItemTypes()
    if (selectableTypes.isEmpty()) return

    val pickerScope = node["picker_scope"]?.asString().orEmpty().ifBlank { "default" }
    val viewModel = hiltViewModel<SelectItemDialogViewModelImpl>(
        key = "lua-select-item:$pickerScope",
    )
    val resetOnClose = node["reset_on_close"]?.asBoolean() == true
    val dismiss = {
        node.action("on_dismiss_request")?.let { actionHandler(listOf(it)) }
        if (resetOnClose) viewModel.reset()
    }

    SelectItemDialog(
        title = luaText(node["title"]),
        selectableTypes = selectableTypes,
        hiddenItems = node["hidden_items"].toHiddenItems(),
        disabledItems = node["disabled_items"].toHiddenItems(),
        onItemSelected = { selection ->
            val actions = node.selectionActions(selection).toMutableList()
            node.action("on_dismiss_request")?.let(actions::add)
            if (actions.isNotEmpty()) actionHandler(actions)
            if (resetOnClose) viewModel.reset()
        },
        onDismissRequest = dismiss,
        resetOnClose = false,
        dismissAfterSelection = false,
        viewModel = viewModel,
    )
}

private fun LuaValueAccessor?.toSelectableItemTypes(): Set<SelectableItemType> =
    toAccessorList().mapNotNull { value ->
        value.asString()?.let { runCatching { SelectableItemType.valueOf(it) }.getOrNull() }
    }.toSet()

private fun LuaValueAccessor?.toHiddenItems(): Set<HiddenItem> =
    toAccessorList().mapNotNull { value ->
        val type = value["type"]?.asString()
            ?.let { runCatching { SelectableItemType.valueOf(it) }.getOrNull() }
            ?: return@mapNotNull null
        val id = value["id"]?.asDouble()?.takeIf { it.isFinite() }?.toLong()
            ?: return@mapNotNull null
        HiddenItem(type, id)
    }.toSet()

private fun LuaValueAccessor?.toAccessorList(): List<LuaValueAccessor> =
    (1..(this?.arraySize ?: 0)).mapNotNull { this?.get(it) }

private fun LuaValueHandle.action(
    callbackName: String,
    argument: LuaValueAccessor? = null,
): LuaUiAction? = this[callbackName]?.asFunction()?.let { callback ->
    LuaUiAction(callback, listOfNotNull(argument))
}

private fun LuaValueHandle.selectionActions(selection: SelectedItemInfo): List<LuaUiAction> = when (selection) {
    is SelectedItemInfo.Group -> listOfNotNull(action(
        "on_group_selected",
        selectionParams(
            id = selection.id,
            type = "GROUP",
            name = selection.name,
            path = selection.path,
        ),
    ))
    is SelectedItemInfo.Tracker -> listOfNotNull(
        action(
            "on_tracker_selected",
            selectionParams(
                id = selection.trackerId,
                type = "TRACKER",
                name = selection.name,
                featureId = selection.featureId,
                path = selection.featurePath,
            ),
        ),
        action(
            "on_feature_selected",
            selectionParams(
                id = selection.featureId,
                type = "FEATURE",
                name = selection.name,
                sourceId = selection.trackerId,
                sourceType = "TRACKER",
                path = selection.featurePath,
            ),
        ),
    )
    is SelectedItemInfo.Graph -> listOfNotNull(action(
        "on_graph_selected",
        selectionParams(selection.id, "GRAPH", selection.name),
    ))
    is SelectedItemInfo.Function -> listOfNotNull(
        action(
            "on_function_selected",
            selectionParams(
                id = selection.functionId,
                type = "FUNCTION",
                name = selection.name,
                featureId = selection.featureId,
                path = selection.featurePath,
            ),
        ),
        action(
            "on_feature_selected",
            selectionParams(
                id = selection.featureId,
                type = "FEATURE",
                name = selection.name,
                sourceId = selection.functionId,
                sourceType = "FUNCTION",
                path = selection.featurePath,
            ),
        ),
    )
}

private fun selectionParams(
    id: Long,
    type: String,
    name: String,
    featureId: Long? = null,
    sourceId: Long? = null,
    sourceType: String? = null,
    path: String? = null,
): LuaValueSnapshot = LuaValueSnapshot.TableValue(buildMap {
    put("id", LuaValueSnapshot.NumberValue(id.toDouble()))
    put("type", LuaValueSnapshot.StringValue(type))
    put("name", LuaValueSnapshot.StringValue(name))
    featureId?.let { put("feature_id", LuaValueSnapshot.NumberValue(it.toDouble())) }
    sourceId?.let { put("source_id", LuaValueSnapshot.NumberValue(it.toDouble())) }
    sourceType?.let { put("source_type", LuaValueSnapshot.StringValue(it)) }
    path?.let { put("path", LuaValueSnapshot.StringValue(it)) }
})
