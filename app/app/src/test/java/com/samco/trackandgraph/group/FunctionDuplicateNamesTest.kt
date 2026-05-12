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

import org.junit.Assert.assertEquals
import org.junit.Test

class FunctionDuplicateNamesTest {

    @Test
    fun `nextDuplicateName appends first suffix when no duplicate names exist`() {
        val result = nextDuplicateName(
            originalName = "Average",
            existingNames = setOf("Average"),
            formatName = duplicateNameFormat,
        )

        assertEquals("Average (1)", result)
    }

    @Test
    fun `nextDuplicateName uses the lowest unused suffix`() {
        val result = nextDuplicateName(
            originalName = "Average",
            existingNames = setOf("Average", "Average (1)", "Average (3)"),
            formatName = duplicateNameFormat,
        )

        assertEquals("Average (2)", result)
    }

    @Test
    fun `nextDuplicateName duplicates an existing duplicate from the original base name`() {
        val result = nextDuplicateName(
            originalName = "Average (1)",
            existingNames = setOf("Average", "Average (1)"),
            formatName = duplicateNameFormat,
        )

        assertEquals("Average (2)", result)
    }

    @Test
    fun `nextDuplicateName respects the supplied format function`() {
        val result = nextDuplicateName(
            originalName = "Average",
            existingNames = setOf("Average", "Copy 1: Average"),
            formatName = { baseName, suffix -> "Copy $suffix: $baseName" },
        )

        assertEquals("Copy 2: Average", result)
    }

    private val duplicateNameFormat: (String, Int) -> String = { baseName, suffix ->
        "$baseName ($suffix)"
    }
}
