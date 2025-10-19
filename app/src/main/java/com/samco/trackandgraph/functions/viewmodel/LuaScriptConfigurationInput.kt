/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.functions.viewmodel

import androidx.compose.runtime.MutableState
import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.lua.dto.EnumOption
import com.samco.trackandgraph.data.lua.dto.TranslatedString
import com.samco.trackandgraph.ui.compose.ui.SelectedTime
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModel
import org.threeten.bp.OffsetDateTime

/**
 * Sealed class representing different types of configuration inputs for Lua scripts.
 * Each type contains mutable state for UI interaction.
 */
sealed class LuaScriptConfigurationInput {
    abstract val name: TranslatedString?

    /**
     * Text input configuration with mutable state for text field value
     */
    data class Text(
        override val name: TranslatedString?,
        val value: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    ) : LuaScriptConfigurationInput()

    /**
     * Number input configuration with mutable state for numeric value
     */
    data class Number(
        override val name: TranslatedString?,
        val value: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    ) : LuaScriptConfigurationInput()

    /**
     * Checkbox input configuration with mutable state for boolean value
     */
    data class Checkbox(
        override val name: TranslatedString?,
        val value: MutableState<Boolean> = mutableStateOf(false)
    ) : LuaScriptConfigurationInput()

    /**
     * Enum input configuration with mutable state for selected option ID
     */
    data class Enum(
        override val name: TranslatedString?,
        val options: List<EnumOption>,
        val value: MutableState<String> = mutableStateOf("")
    ) : LuaScriptConfigurationInput()

    /**
     * Unsigned integer input configuration with mutable state for uint value
     */
    data class UInt(
        override val name: TranslatedString?,
        val value: MutableState<TextFieldValue> = mutableStateOf(TextFieldValue(""))
    ) : LuaScriptConfigurationInput()

    /**
     * Duration input configuration with DurationInputViewModel for hours/minutes/seconds
     */
    data class Duration(
        override val name: TranslatedString?,
        val viewModel: DurationInputViewModel
    ) : LuaScriptConfigurationInput()

    /**
     * Local time input configuration with mutable state for hour and minute
     */
    data class LocalTime(
        override val name: TranslatedString?,
        val time: MutableState<SelectedTime> = mutableStateOf(SelectedTime(0, 0))
    ) : LuaScriptConfigurationInput()

    /**
     * Instant (date/time) input configuration with mutable state for OffsetDateTime
     */
    data class Instant(
        override val name: TranslatedString?,
        val dateTime: MutableState<OffsetDateTime> = mutableStateOf(OffsetDateTime.now())
    ) : LuaScriptConfigurationInput()
}
