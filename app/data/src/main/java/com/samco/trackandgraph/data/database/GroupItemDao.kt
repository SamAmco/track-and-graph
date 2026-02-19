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

package com.samco.trackandgraph.data.database

import androidx.room.Dao
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.samco.trackandgraph.data.database.entity.GroupItem
import com.samco.trackandgraph.data.database.entity.GroupItemType

@Dao
internal interface GroupItemDao {

    @Insert
    fun insertGroupItem(groupItem: GroupItem): Long

    @Update
    fun updateGroupItem(groupItem: GroupItem)

    @Update
    fun updateGroupItems(groupItems: List<GroupItem>)

    @Query("DELETE FROM group_items_table WHERE id = :id")
    fun deleteGroupItem(id: Long)

    @Query("DELETE FROM group_items_table WHERE child_id = :childId AND type = :type")
    fun deleteGroupItemsByChild(childId: Long, type: GroupItemType)

    @Query("SELECT * FROM group_items_table WHERE child_id = :childId AND type = :type")
    fun getGroupItemsForChild(childId: Long, type: GroupItemType): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE group_id = :groupId ORDER BY display_index ASC")
    fun getGroupItemsForGroup(groupId: Long): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE group_id IS NULL ORDER BY display_index ASC")
    fun getGroupItemsWithNoGroup(): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE group_id IS NULL AND type = :type ORDER BY display_index ASC")
    fun getGroupItemsWithNoGroupByType(type: GroupItemType): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE group_id = :groupId AND type = :type ORDER BY display_index ASC")
    fun getGroupItemsByType(groupId: Long, type: GroupItemType): List<GroupItem>

    @Query("SELECT * FROM group_items_table WHERE (group_id = :groupId OR (:groupId IS NULL AND group_id IS NULL)) AND type = :type ORDER BY display_index ASC")
    fun getGroupItemsByTypeNullable(groupId: Long?, type: GroupItemType): List<GroupItem>

    @Query("SELECT MAX(display_index) FROM group_items_table WHERE group_id = :groupId")
    fun getMaxDisplayIndexForGroup(groupId: Long): Int?

    @Query("SELECT MAX(display_index) FROM group_items_table WHERE group_id IS NULL")
    fun getMaxDisplayIndexForNullGroup(): Int?

    @Query("UPDATE group_items_table SET display_index = display_index + 1 WHERE group_id = :groupId")
    fun shiftDisplayIndexesDown(groupId: Long)

    @Query("UPDATE group_items_table SET display_index = display_index + 1 WHERE group_id IS NULL")
    fun shiftDisplayIndexesDownForNullGroup()

    @Query("SELECT * FROM group_items_table WHERE group_id = :groupId AND child_id = :childId AND type = :type LIMIT 1")
    fun getGroupItem(groupId: Long, childId: Long, type: GroupItemType): GroupItem?

    @Query("SELECT * FROM group_items_table WHERE group_id IS NULL AND child_id = :childId AND type = :type LIMIT 1")
    fun getGroupItemWithNullGroup(childId: Long, type: GroupItemType): GroupItem?
}
