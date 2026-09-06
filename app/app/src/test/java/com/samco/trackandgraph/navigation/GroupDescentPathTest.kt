/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.samco.trackandgraph.navigation

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.threeten.bp.LocalTime

class GroupDescentPathTest {

    @Test
    fun `findFirstDescentToReminder returns empty descent for reminder in root`() {
        val graph = groupGraph(
            id = 0L,
            children = listOf(reminderNode(groupItemId = 30L)),
        )

        assertEquals(
            GroupDescentPath(groupIds = emptyList(), groupItemId = 30L),
            graph.findFirstDescentToReminder(5L),
        )
    }

    @Test
    fun `findFirstDescentToReminder returns path to nested reminder placement`() {
        val graph = groupGraph(
            id = 0L,
            children = listOf(
                GroupGraphItem.GroupNode(
                    groupItemId = 10L,
                    groupGraph = groupGraph(
                        id = 1L,
                        children = listOf(reminderNode(groupItemId = 30L)),
                    ),
                )
            ),
        )

        assertEquals(
            GroupDescentPath(groupIds = listOf(1L), groupItemId = 30L),
            graph.findFirstDescentToReminder(5L),
        )
    }

    @Test
    fun `findFirstDescentToReminder uses first path to symlinked owning group`() {
        val owningGroup = groupGraph(
            id = 3L,
            children = listOf(reminderNode(groupItemId = 30L)),
        )
        val graph = groupGraph(
            id = 0L,
            children = listOf(
                GroupGraphItem.GroupNode(
                    groupItemId = 10L,
                    groupGraph = groupGraph(
                        id = 1L,
                        children = listOf(
                            GroupGraphItem.GroupNode(11L, owningGroup),
                        ),
                    ),
                ),
                GroupGraphItem.GroupNode(
                    groupItemId = 20L,
                    groupGraph = groupGraph(
                        id = 2L,
                        children = listOf(
                            GroupGraphItem.GroupNode(21L, owningGroup),
                        ),
                    ),
                ),
            ),
        )

        assertEquals(
            GroupDescentPath(groupIds = listOf(1L, 3L), groupItemId = 30L),
            graph.findFirstDescentToReminder(5L),
        )
    }

    @Test
    fun `findFirstDescentToReminder returns null for missing or global-only reminder`() {
        assertNull(groupGraph(id = 0L).findFirstDescentToReminder(99L))
    }

    private fun groupGraph(
        id: Long,
        children: List<GroupGraphItem> = emptyList(),
    ) = GroupGraph(
        group = Group(id = id, name = "Group $id", colorIndex = 0, unique = true),
        children = children,
    )

    private fun reminderNode(groupItemId: Long) = GroupGraphItem.ReminderNode(
        groupItemId = groupItemId,
        reminder = Reminder(
            id = 5L,
            reminderName = "Reminder",
            featureId = null,
            params = ReminderParams.WeekDayParams(
                time = LocalTime.NOON,
                checkedDays = CheckedDays.none(),
            ),
            unique = true,
        ),
    )
}
