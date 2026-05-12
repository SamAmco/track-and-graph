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
package com.samco.trackandgraph.group

internal fun nextDuplicateName(
    originalName: String,
    existingNames: Set<String>,
    formatName: (String, Int) -> String,
): String {
    val baseName = existingNames.firstNotNullOfOrNull { existingName ->
        (1..existingNames.size).firstOrNull { suffix ->
            formatName(existingName, suffix) == originalName
        }?.let { existingName }
    } ?: originalName

    var suffix = 1
    while (true) {
        val candidate = formatName(baseName, suffix)
        if (candidate !in existingNames && candidate != originalName) return candidate
        suffix++
    }
}
