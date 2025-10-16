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
package com.samco.trackandgraph.data.lua.functionadapters

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import timber.log.Timber
import javax.inject.Inject

/**
 * Hydrates enum config options by replacing option IDs with their full translation tables
 */
internal class EnumHydrator @Inject constructor() {

    /**
     * Hydrate a resolved script's config array by replacing enum option IDs with translation tables
     * @param resolvedScript The resolved script (modified in place)
     * @param enumsTable The table of all enum translations from the catalog
     */
    fun hydrateEnums(resolvedScript: LuaValue, enumsTable: LuaTable?) {
        if (enumsTable == null) return

        val configArray = resolvedScript["config"]
        if (configArray.isnil() || !configArray.istable()) return

        var i = 1
        while (true) {
            val configItem = configArray[i]
            if (configItem.isnil()) break

            val typeValue = configItem["type"]
            if (typeValue.isstring() && typeValue.checkjstring() == "enum") {
                hydrateEnumConfigItem(configItem, enumsTable)
            }

            i++
        }
    }

    private fun hydrateEnumConfigItem(configItem: LuaValue, enumsTable: LuaTable) {
        val optionsArray = configItem["options"]
        if (!optionsArray.istable()) {
            Timber.w("Enum config item missing options array")
            return
        }

        val optionsTable = optionsArray.checktable()!!

        // Collect enum IDs from integer keys
        val enumIds = mutableListOf<String>()
        var i = 1
        while (true) {
            val optionId = optionsTable[i]
            if (optionId.isnil()) break

            if (optionId.isstring()) {
                enumIds.add(optionId.checkjstring()!!)
            }

            i++
        }

        // Remove all integer keys
        for (index in 1..enumIds.size) {
            optionsTable[index] = org.luaj.vm2.LuaValue.NIL
        }

        // Add string keys with translation tables
        for (enumId in enumIds) {
            val enumTranslations = enumsTable[enumId]

            if (!enumTranslations.isnil() && enumTranslations.istable()) {
                optionsTable[enumId] = enumTranslations
            } else {
                Timber.w("Enum option '$enumId' not found in enums table")
            }
        }
    }
}
