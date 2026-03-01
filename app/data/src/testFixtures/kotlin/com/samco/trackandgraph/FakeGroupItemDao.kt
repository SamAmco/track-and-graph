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

package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.GroupItemDao
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType

/**
 * A fake in-memory implementation of [GroupItemDao] for testing purposes.
 * This allows tests to run without a real database and without mocking frameworks.
 */
internal class FakeGroupItemDao : GroupItemDao {

    private var nextId = 1L
    private val items = mutableMapOf<Long, GroupItem>()

    // =========================================================================
    // Test helper methods
    // =========================================================================

    fun clear() {
        items.clear()
        nextId = 1L
    }

    fun getAll(): List<GroupItem> = items.values.toList()

    // =========================================================================
    // GroupItemDao implementation
    // =========================================================================

    override fun insertGroupItem(groupItem: GroupItem): Long {
        val id = if (groupItem.id == 0L) nextId++ else groupItem.id
        items[id] = groupItem.copy(id = id)
        return id
    }

    override fun updateGroupItem(groupItem: GroupItem) {
        items[groupItem.id] = groupItem
    }

    override fun updateGroupItems(groupItems: List<GroupItem>) {
        groupItems.forEach { items[it.id] = it }
    }

    override fun deleteGroupItem(id: Long) {
        items.remove(id)
    }

    override fun deleteGroupItemsByChild(childId: Long, type: GroupItemType) {
        items.entries.removeIf { it.value.childId == childId && it.value.type == type }
    }

    override fun getGroupItemsForChild(childId: Long, type: GroupItemType): List<GroupItem> {
        return items.values.filter { it.childId == childId && it.type == type }
    }

    override fun getGroupItemsForGroup(groupId: Long): List<GroupItem> {
        return items.values.filter { it.groupId == groupId }
    }

    override fun getGroupItemsWithNoGroup(): List<GroupItem> {
        return items.values.filter { it.groupId == null }
    }

    override fun getGroupItemsByType(type: GroupItemType): List<GroupItem> {
        return items.values.filter { it.type == type }
    }

    override fun getGroupItemsByType(groupId: Long, type: GroupItemType): List<GroupItem> {
        return items.values.filter { it.groupId == groupId && it.type == type }
    }

    override fun shiftDisplayIndexesDown(groupId: Long) {
        items.entries.filter { it.value.groupId == groupId }.forEach { entry ->
            items[entry.key] = entry.value.copy(displayIndex = entry.value.displayIndex + 1)
        }
    }

    override fun shiftDisplayIndexesDownAfter(groupId: Long, afterIndex: Int) {
        items.entries
            .filter { it.value.groupId == groupId && it.value.displayIndex > afterIndex }
            .forEach { entry ->
                items[entry.key] = entry.value.copy(displayIndex = entry.value.displayIndex + 1)
            }
    }

    override fun shiftDisplayIndexesDownForNullGroup() {
        items.entries.filter { it.value.groupId == null }.forEach { entry ->
            items[entry.key] = entry.value.copy(displayIndex = entry.value.displayIndex + 1)
        }
    }

    override fun shiftDisplayIndexesDownAfterForNullGroup(afterIndex: Int) {
        items.entries
            .filter { it.value.groupId == null && it.value.displayIndex > afterIndex }
            .forEach { entry ->
                items[entry.key] = entry.value.copy(displayIndex = entry.value.displayIndex + 1)
            }
    }

    override fun getGroupItem(groupId: Long, childId: Long, type: GroupItemType): GroupItem? {
        return items.values.find {
            it.groupId == groupId && it.childId == childId && it.type == type
        }
    }

    override fun getAllGroupItems(): List<GroupItem> = items.values.toList()
}
