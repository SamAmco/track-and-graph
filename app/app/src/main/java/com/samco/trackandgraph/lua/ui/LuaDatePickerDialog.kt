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
import com.samco.trackandgraph.data.lua.dto.LuaValueHandle
import com.samco.trackandgraph.data.lua.dto.LuaValueSnapshot
import com.samco.trackandgraph.ui.ui.DatePickerDialogContent
import org.threeten.bp.OffsetDateTime

@Composable
internal fun LuaDatePickerDialog(
    node: LuaValueHandle,
    actionHandler: (List<LuaUiAction>) -> Unit,
) {
    if (node["visible"]?.asBoolean() != true) return
    val today = OffsetDateTime.now()
    val date = node["initial_date"]
    val initial = runCatching {
        today.withYear(date?.get("year")?.asDouble()?.toInt() ?: today.year)
            .withMonth(date?.get("month")?.asDouble()?.toInt() ?: today.monthValue)
            .withDayOfMonth(date?.get("day")?.asDouble()?.toInt() ?: today.dayOfMonth)
    }.getOrDefault(today)
    var selectionAction: LuaUiAction? = null
    DatePickerDialogContent(
        initialDateTime = initial,
        allowPastDates = node["allow_past_dates"]?.asBoolean() != false,
        onDismissRequest = {
            val actions = listOfNotNull(selectionAction, node.action("on_dismiss_request"))
            if (actions.isNotEmpty()) actionHandler(actions)
        },
        onDateSelected = { selected ->
            val value = LuaValueSnapshot.TableValue(mapOf(
                "year" to LuaValueSnapshot.NumberValue(selected.year.toDouble()),
                "month" to LuaValueSnapshot.NumberValue(selected.monthValue.toDouble()),
                "day" to LuaValueSnapshot.NumberValue(selected.dayOfMonth.toDouble()),
            ))
            // The native dialog dismisses synchronously after selection. Dispatch both Lua
            // handlers together so there is one mutation/rerender under the session's VM lock.
            selectionAction = node.action("on_date_selected", value)
        },
    )
}
