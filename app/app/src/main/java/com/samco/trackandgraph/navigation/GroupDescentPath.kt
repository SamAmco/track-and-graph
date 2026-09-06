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
package com.samco.trackandgraph.navigation

import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem

/**
 * A descent from the user's current `GroupScreen` down to a destination group.
 *
 * [groupIds] lists the groups to open in order, each nested inside the previous. The current
 * location (the anchor) is excluded; the innermost id is the group the user lands in.
 *
 * If [groupItemId] is non-null, the destination group will scroll to the placement with
 * that id once it loads.
 */
data class GroupDescentPath(
    val groupIds: List<Long>,
    val groupItemId: Long?,
)

/** Finds the first root-relative descent to a grouped reminder in display order. */
internal fun GroupGraph.findFirstDescentToReminder(reminderId: Long): GroupDescentPath? =
    findFirstDescentToReminder(
        reminderId = reminderId,
        groupIds = emptyList(),
        visitedGroupIds = mutableSetOf(group.id),
    )

private fun GroupGraph.findFirstDescentToReminder(
    reminderId: Long,
    groupIds: List<Long>,
    visitedGroupIds: MutableSet<Long>,
): GroupDescentPath? {
    for (child in children) {
        if (child is GroupGraphItem.ReminderNode && child.reminder.id == reminderId) {
            return GroupDescentPath(groupIds = groupIds, groupItemId = child.groupItemId)
        }

        if (child is GroupGraphItem.GroupNode) {
            val childGroupId = child.groupGraph.group.id
            if (visitedGroupIds.add(childGroupId)) {
                val result = child.groupGraph.findFirstDescentToReminder(
                    reminderId = reminderId,
                    groupIds = groupIds + childGroupId,
                    visitedGroupIds = visitedGroupIds,
                )
                visitedGroupIds.remove(childGroupId)
                if (result != null) return result
            }
        }
    }
    return null
}
