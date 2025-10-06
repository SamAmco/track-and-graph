package com.samco.trackandgraph.data.lua.apiimpl

import com.samco.trackandgraph.data.database.dto.LuaScriptConfigurationValue
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject

class ConfigurationValueParser @Inject constructor() {
    fun parseConfigurationValues(configuration: List<LuaScriptConfigurationValue>): LuaTable {
        val table = LuaTable()
        for (value in configuration) {
            table[value.id] = parseConfigurationValue(value)
        }
        return table
    }

    private fun parseConfigurationValue(value: LuaScriptConfigurationValue): LuaValue {
        return when (value) {
            is LuaScriptConfigurationValue.Number -> parseNumber(value)
            is LuaScriptConfigurationValue.Text -> parseText(value)
            is LuaScriptConfigurationValue.Checkbox -> parseCheckbox(value)
        }
    }

    private fun parseNumber(value: LuaScriptConfigurationValue.Number): LuaValue {
        return LuaValue.valueOf(value.value)
    }

    private fun parseText(value: LuaScriptConfigurationValue.Text): LuaValue {
        return LuaValue.valueOf(value.value)
    }

    private fun parseCheckbox(value: LuaScriptConfigurationValue.Checkbox): LuaValue {
        return LuaValue.valueOf(value.value)
    }
}