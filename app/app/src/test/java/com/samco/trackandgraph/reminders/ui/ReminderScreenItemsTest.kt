/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.reminders.ui

import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.reminders.reminderFixture
import org.junit.Assert.assertEquals
import org.junit.Test

class ReminderScreenItemsTest {

    @Test
    fun `reminder without a global placement is omitted`() {
        assertEquals(
            emptyList<ReminderScreenItem>(),
            buildReminderScreenItems(
                reminders = listOf(reminderFixture),
                globalIndices = emptyList(),
            ),
        )
    }

    @Test
    fun `reminders use their global placement identity and order`() {
        val secondReminder = reminderFixture.copy(id = 2L)

        assertEquals(
            listOf(
                ReminderScreenItem(secondReminder, groupItemId = 20L),
                ReminderScreenItem(reminderFixture, groupItemId = 10L),
            ),
            buildReminderScreenItems(
                reminders = listOf(reminderFixture, secondReminder),
                globalIndices = listOf(
                    placement(reminderId = 1L, groupItemId = 10L, displayIndex = 3),
                    placement(reminderId = 2L, groupItemId = 20L, displayIndex = 1),
                ),
            ),
        )
    }

    private fun placement(
        reminderId: Long,
        groupItemId: Long,
        displayIndex: Int,
    ) = GroupChildDisplayIndex(
        groupItemId = groupItemId,
        type = GroupChildType.REMINDER,
        id = reminderId,
        displayIndex = displayIndex,
    )
}
