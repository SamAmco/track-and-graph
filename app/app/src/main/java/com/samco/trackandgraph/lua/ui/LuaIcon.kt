/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import androidx.annotation.DrawableRes
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LocalContentColor
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.painter.Painter
import androidx.compose.ui.graphics.vector.rememberVectorPainter
import androidx.compose.ui.res.painterResource
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.ui.R as UiR

@Composable
internal fun LuaIcon(node: LuaValueHandle, modifier: Modifier) {
    val painter = luaIconPainter(node["icon"]?.asString()) ?: return
    Icon(
        painter = painter,
        contentDescription = node["content_description"]?.let { luaText(it) },
        modifier = modifier,
        tint = luaColor(node["tint"]) ?: LocalContentColor.current,
    )
}

@Composable
internal fun LuaIconButton(
    node: LuaValueHandle,
    modifier: Modifier,
    actionHandler: (List<LuaUiAction>) -> Unit,
    renderNode: @Composable (LuaValueHandle) -> Unit,
) {
    val action = node.action("on_click")
    IconButton(
        onClick = { action?.let { actionHandler(listOf(it)) } },
        modifier = modifier,
        enabled = node["enabled"]?.asBoolean() != false && action != null,
    ) {
        node["child"]?.let { renderNode(it) }
    }
}

@Composable
private fun luaIconPainter(token: String?): Painter? = when (token) {
    "ICON_BACK" -> rememberVectorPainter(Icons.AutoMirrored.Filled.ArrowBack)
    "ICON_MENU" -> rememberVectorPainter(Icons.Filled.Menu)
    else -> luaIconDrawableResource(token)?.let { painterResource(it) }
}

@DrawableRes
internal fun luaIconDrawableResource(token: String?): Int? = when (token) {
    "ICON_ADD" -> R.drawable.add_icon
    "ICON_ADD_RECORD" -> R.drawable.ic_add_record
    "ICON_BACKUP_RESTORE" -> R.drawable.backup_restore_icon
    "ICON_CHECK_CIRCLE" -> R.drawable.check_circle_24px
    "ICON_CLOSE" -> R.drawable.close
    "ICON_COPY" -> R.drawable.content_copy_24px
    "ICON_DELETE" -> R.drawable.delete_icon
    "ICON_DESELECT_ALL" -> R.drawable.deselect_24px
    "ICON_DRAG_HANDLE" -> R.drawable.ic_scroll_grabber
    "ICON_EDIT" -> R.drawable.edit_icon
    "ICON_EXPAND_LESS" -> R.drawable.up_arrow
    "ICON_EXPAND_MORE" -> R.drawable.down_arrow
    "ICON_GRAPH" -> R.drawable.chart_data
    "ICON_GROUP" -> R.drawable.folder_24px
    "ICON_HELP" -> R.drawable.faq_icon
    "ICON_HOME" -> R.drawable.home_menu_icon
    "ICON_IMPORT_EXPORT" -> R.drawable.swap_vert_icon
    "ICON_INFO" -> R.drawable.about_icon
    "ICON_LANGUAGE" -> R.drawable.language_icon
    "ICON_LINK" -> R.drawable.link_2_24px
    "ICON_LOCK" -> R.drawable.ic_lock
    "ICON_LUA" -> R.drawable.lua_icon
    "ICON_MORE" -> R.drawable.list_menu_icon
    "ICON_MOVE" -> R.drawable.move_item_24px
    "ICON_OPEN_FOLDER" -> R.drawable.folder_open
    "ICON_PASTE" -> R.drawable.content_paste
    "ICON_PLAY" -> R.drawable.ic_play_timer
    "ICON_REFRESH" -> R.drawable.refresh
    "ICON_REMINDER" -> R.drawable.reminders_icon
    "ICON_SEARCH" -> R.drawable.search_icon
    "ICON_SELECT_ALL" -> R.drawable.select_all_24px
    "ICON_STOP" -> R.drawable.ic_stop_timer
    "ICON_TRACKER" -> R.drawable.add_box
    "ICON_UNLOCK" -> R.drawable.ic_lock_open
    "ICON_VISIBILITY" -> UiR.drawable.visibility_24px
    "ICON_VISIBILITY_OFF" -> UiR.drawable.visibility_off_24px
    "ICON_WARNING" -> R.drawable.warning_icon
    else -> null
}
