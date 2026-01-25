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

package com.samco.trackandgraph.data.database.dto

/**
 * Represents the type of item in a layout.
 * - TRACKER: A tracker (stored in features_table with a corresponding row in trackers_table)
 * - FUNCTION: A function (stored in features_table with a corresponding row in functions_table)
 * - GROUP: A subgroup (stored in groups_table)
 * - GRAPH: A graph or stat (stored in graphs_and_stats_table2)
 * - REMINDER: A reminder (stored in reminders_table)
 */
enum class LayoutItemType {
    TRACKER,
    FUNCTION,
    GROUP,
    GRAPH,
    REMINDER
}

/**
 * Represents the position of an item within a group's layout.
 * This decouples display ordering from the underlying entity tables.
 *
 * @property id The unique identifier for this layout item
 * @property groupId The group this item belongs to. Can be -1 for items that don't belong to a
 *                   specific group (e.g., reminders in the reminders screen)
 * @property type The type of item (tracker, function, group, graph, or reminder)
 * @property itemId The ID of the referenced item in its respective table
 * @property displayIndex The position of this item within its group (0-indexed, lower = earlier)
 */
data class LayoutItem(
    val id: Long,
    val groupId: Long,
    val type: LayoutItemType,
    val itemId: Long,
    val displayIndex: Int
) {
    internal fun toEntity() = com.samco.trackandgraph.data.database.entity.LayoutItem(
        id = id,
        groupId = groupId,
        type = type,
        itemId = itemId,
        displayIndex = displayIndex
    )
}
