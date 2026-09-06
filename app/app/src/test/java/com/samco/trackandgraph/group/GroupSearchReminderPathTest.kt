/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.samco.trackandgraph.group

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.navigation.GroupDescentPath
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Assert.assertEquals
import org.junit.Before
import org.junit.Test
import org.mockito.kotlin.mock
import org.threeten.bp.LocalTime

@OptIn(ExperimentalCoroutinesApi::class)
class GroupSearchReminderPathTest {

    private val dispatcher = UnconfinedTestDispatcher()
    private lateinit var viewModel: GroupSearchViewModelImpl

    @Before
    fun setUp() {
        Dispatchers.setMain(dispatcher)
        viewModel = GroupSearchViewModelImpl(
            dataInteractor = mock(),
            gsiProvider = mock(),
            reminderViewDataFactory = mock(),
            defaultDispatcher = dispatcher,
        )
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `reminder path navigates to its placement`() {
        val graph = groupGraph(
            id = 0L,
            name = "Root",
            children = listOf(reminderNode(groupItemId = 30L)),
        )

        assertEquals(
            listOf(
                ResolvedPath(
                    descent = GroupDescentPath(emptyList(), groupItemId = 30L),
                    displayString = "/Reminder",
                )
            ),
            viewModel.buildResolvedPaths(graph)[ComponentKey(ComponentType.REMINDER, 5L)],
        )
    }

    @Test
    fun `reminder in symlinked owning group has every descent path`() {
        val owningGroup = groupGraph(
            id = 3L,
            name = "shared",
            children = listOf(reminderNode(groupItemId = 30L)),
        )
        val graph = groupGraph(
            id = 0L,
            name = "Root",
            children = listOf(
                groupNode(10L, groupGraph(1L, "a", listOf(groupNode(11L, owningGroup)))),
                groupNode(20L, groupGraph(2L, "b", listOf(groupNode(21L, owningGroup)))),
            ),
        )

        assertEquals(
            listOf(
                ResolvedPath(
                    descent = GroupDescentPath(listOf(1L, 3L), groupItemId = 30L),
                    displayString = "/a/shared/Reminder",
                ),
                ResolvedPath(
                    descent = GroupDescentPath(listOf(2L, 3L), groupItemId = 30L),
                    displayString = "/b/shared/Reminder",
                ),
            ),
            viewModel.buildResolvedPaths(graph)[ComponentKey(ComponentType.REMINDER, 5L)],
        )
    }

    private fun groupGraph(
        id: Long,
        name: String,
        children: List<GroupGraphItem> = emptyList(),
    ) = GroupGraph(
        group = Group(id = id, name = name, colorIndex = 0, unique = true),
        children = children,
    )

    private fun groupNode(groupItemId: Long, graph: GroupGraph) =
        GroupGraphItem.GroupNode(groupItemId, graph)

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
