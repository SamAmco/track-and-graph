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

internal class FakeGroupItemDao : GroupItemDao {
    private val items = mutableListOf<GroupItem>()
    private var nextId = 1L

    override fun insertGroupItem(groupItem: GroupItem): Long {
        val id = nextId++
        items.add(groupItem.copy(id = id))
        return id
    }

    override fun updateGroupItem(groupItem: GroupItem) {
        val index = items.indexOfFirst { it.id == groupItem.id }
        if (index >= 0) items[index] = groupItem
    }

    override fun updateGroupItems(groupItems: List<GroupItem>) {
        groupItems.forEach { updateGroupItem(it) }
    }

    override fun deleteGroupItem(id: Long) {
        items.removeAll { it.id == id }
    }

    override fun deleteGroupItemsByChild(childId: Long, type: GroupItemType) {
        items.removeAll { it.childId == childId && it.type == type }
    }

    override fun getGroupItemsForChild(childId: Long, type: GroupItemType): List<GroupItem> {
        return items.filter { it.childId == childId && it.type == type }
    }

    override fun getGroupItemsForGroup(groupId: Long): List<GroupItem> {
        return items.filter { it.groupId == groupId }.sortedBy { it.displayIndex }
    }

    override fun getGroupItemsWithNoGroup(): List<GroupItem> {
        return items.filter { it.groupId == null }.sortedBy { it.displayIndex }
    }

    override fun getGroupItemsWithNoGroupByType(type: GroupItemType): List<GroupItem> {
        return items.filter { it.groupId == null && it.type == type }.sortedBy { it.displayIndex }
    }

    override fun getGroupItemsByType(groupId: Long, type: GroupItemType): List<GroupItem> {
        return items.filter { it.groupId == groupId && it.type == type }.sortedBy { it.displayIndex }
    }

    override fun getGroupItemsByTypeNullable(groupId: Long?, type: GroupItemType): List<GroupItem> {
        return items.filter { it.groupId == groupId && it.type == type }.sortedBy { it.displayIndex }
    }

    override fun getMaxDisplayIndexForGroup(groupId: Long): Int? {
        return items.filter { it.groupId == groupId }.maxOfOrNull { it.displayIndex }
    }

    override fun getMaxDisplayIndexForNullGroup(): Int? {
        return items.filter { it.groupId == null }.maxOfOrNull { it.displayIndex }
    }

    override fun shiftDisplayIndexesDown(groupId: Long) {
        items.replaceAll {
            if (it.groupId == groupId) it.copy(displayIndex = it.displayIndex + 1)
            else it
        }
    }

    override fun shiftDisplayIndexesDownForNullGroup() {
        items.replaceAll {
            if (it.groupId == null) it.copy(displayIndex = it.displayIndex + 1)
            else it
        }
    }

    override fun getGroupItem(groupId: Long, childId: Long, type: GroupItemType): GroupItem? {
        return items.find { it.groupId == groupId && it.childId == childId && it.type == type }
    }

    override fun getGroupItemWithNullGroup(childId: Long, type: GroupItemType): GroupItem? {
        return items.find { it.groupId == null && it.childId == childId && it.type == type }
    }

    // Test helper
    fun clear() {
        items.clear()
        nextId = 1L
    }
}
