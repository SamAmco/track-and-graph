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
            is LuaScriptConfigurationValue.Number -> LuaValue.valueOf(value.value)
            is LuaScriptConfigurationValue.Text -> LuaValue.valueOf(value.value)
            is LuaScriptConfigurationValue.Checkbox -> LuaValue.valueOf(value.value)
            is LuaScriptConfigurationValue.Enum -> LuaValue.valueOf(value.value)
            is LuaScriptConfigurationValue.UInt -> LuaValue.valueOf(value.value)
            is LuaScriptConfigurationValue.Duration -> LuaValue.valueOf(value.value)
        }
    }
}