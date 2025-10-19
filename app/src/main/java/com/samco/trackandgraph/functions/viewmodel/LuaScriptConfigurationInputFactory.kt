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

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import com.samco.trackandgraph.data.lua.dto.LuaFunctionConfigSpec
import com.samco.trackandgraph.ui.compose.ui.SelectedTime
import com.samco.trackandgraph.ui.viewmodels.DurationInputViewModelImpl
import javax.inject.Inject

/**
 * Factory class responsible for creating LuaScriptConfigurationInput instances
 * with proper default value handling and type compatibility checking.
 */
internal class LuaScriptConfigurationInputFactory @Inject constructor() {

    /**
     * Creates a configuration input from a config spec, optionally restoring from saved value.
     * Handles default values when no saved value is available or compatible.
     */
    fun createConfigurationInput(
        config: LuaFunctionConfigSpec,
        savedValue: LuaScriptConfigurationValue? = null
    ): LuaScriptConfigurationInput {
        return when (config) {
            is LuaFunctionConfigSpec.Text -> createTextInput(config, savedValue)
            is LuaFunctionConfigSpec.Number -> createNumberInput(config, savedValue)
            is LuaFunctionConfigSpec.Checkbox -> createCheckboxInput(config, savedValue)
            is LuaFunctionConfigSpec.Enum -> createEnumInput(config, savedValue)
            is LuaFunctionConfigSpec.UInt -> createUIntInput(config, savedValue)
            is LuaFunctionConfigSpec.Duration -> createDurationInput(config, savedValue)
            is LuaFunctionConfigSpec.LocalTime -> createLocalTimeInput(config, savedValue)
        }
    }

    /**
     * Checks if an existing input is compatible with a new config spec type.
     */
    private fun isCompatibleType(
        existingInput: LuaScriptConfigurationInput?,
        config: LuaFunctionConfigSpec
    ): Boolean {
        return when (config) {
            is LuaFunctionConfigSpec.Text -> existingInput is LuaScriptConfigurationInput.Text
            is LuaFunctionConfigSpec.Number -> existingInput is LuaScriptConfigurationInput.Number
            is LuaFunctionConfigSpec.Checkbox -> existingInput is LuaScriptConfigurationInput.Checkbox
            is LuaFunctionConfigSpec.Enum -> existingInput is LuaScriptConfigurationInput.Enum
            is LuaFunctionConfigSpec.UInt -> existingInput is LuaScriptConfigurationInput.UInt
            is LuaFunctionConfigSpec.Duration -> existingInput is LuaScriptConfigurationInput.Duration
            is LuaFunctionConfigSpec.LocalTime -> existingInput is LuaScriptConfigurationInput.LocalTime
        }
    }

    /**
     * Creates or recovers a configuration input, preserving existing values when type-compatible.
     */
    fun createOrRecoverInput(
        config: LuaFunctionConfigSpec,
        existingInput: LuaScriptConfigurationInput?,
        savedValue: LuaScriptConfigurationValue? = null
    ): LuaScriptConfigurationInput {
        return if (isCompatibleType(existingInput, config)) {
            existingInput!!
        } else {
            createConfigurationInput(config, savedValue)
        }
    }

    private fun createTextInput(
        config: LuaFunctionConfigSpec.Text,
        savedValue: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.Text {
        val textValue = savedValue as? LuaScriptConfigurationValue.Text
        val initialValue = textValue?.value ?: config.defaultValue ?: ""
        
        return LuaScriptConfigurationInput.Text(
            name = config.name,
            value = mutableStateOf(TextFieldValue(initialValue))
        )
    }

    private fun createNumberInput(
        config: LuaFunctionConfigSpec.Number,
        savedValue: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.Number {
        val numberValue = savedValue as? LuaScriptConfigurationValue.Number
        val initialValue = numberValue?.value ?: config.defaultValue ?: 1.0
        
        return LuaScriptConfigurationInput.Number(
            name = config.name,
            value = mutableStateOf(TextFieldValue(initialValue.toString()))
        )
    }

    private fun createCheckboxInput(
        config: LuaFunctionConfigSpec.Checkbox,
        savedValue: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.Checkbox {
        val checkboxValue = savedValue as? LuaScriptConfigurationValue.Checkbox
        val initialValue = checkboxValue?.value ?: config.defaultValue ?: false

        return LuaScriptConfigurationInput.Checkbox(
            name = config.name,
            value = mutableStateOf(initialValue)
        )
    }

    private fun createEnumInput(
        config: LuaFunctionConfigSpec.Enum,
        savedValue: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.Enum {
        val enumValue = savedValue as? LuaScriptConfigurationValue.Enum
        val initialValue = enumValue?.value ?: config.defaultValue ?: config.options.firstOrNull()?.id ?: ""

        return LuaScriptConfigurationInput.Enum(
            name = config.name,
            options = config.options,
            value = mutableStateOf(initialValue)
        )
    }

    private fun createUIntInput(
        config: LuaFunctionConfigSpec.UInt,
        savedValue: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.UInt {
        val uintValue = savedValue as? LuaScriptConfigurationValue.UInt
        val initialValue = uintValue?.value ?: config.defaultValue ?: 1

        return LuaScriptConfigurationInput.UInt(
            name = config.name,
            value = mutableStateOf(TextFieldValue(initialValue.toString()))
        )
    }

    private fun createDurationInput(
        config: LuaFunctionConfigSpec.Duration,
        savedValue: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.Duration {
        val durationValue = savedValue as? LuaScriptConfigurationValue.Duration
        // Both savedValue and config.defaultValue are in seconds
        val seconds = durationValue?.seconds ?: config.defaultValueSeconds ?: 0.0

        val viewModel = DurationInputViewModelImpl()
        viewModel.setDurationFromDouble(seconds)  // ViewModel uses seconds

        return LuaScriptConfigurationInput.Duration(
            name = config.name,
            viewModel = viewModel
        )
    }

    private fun createLocalTimeInput(
        config: LuaFunctionConfigSpec.LocalTime,
        savedValue: LuaScriptConfigurationValue?
    ): LuaScriptConfigurationInput.LocalTime {
        val localTimeValue = savedValue as? LuaScriptConfigurationValue.LocalTime
        // Both savedValue and config.defaultValue are in minutes since midnight (0-1439)
        val minutesSinceMidnight = localTimeValue?.minutes ?: config.defaultValueMinutes ?: 720 // Default 12:00

        val hour = minutesSinceMidnight / 60
        val minute = minutesSinceMidnight % 60

        return LuaScriptConfigurationInput.LocalTime(
            name = config.name,
            time = mutableStateOf(SelectedTime(hour, minute))
        )
    }
}
