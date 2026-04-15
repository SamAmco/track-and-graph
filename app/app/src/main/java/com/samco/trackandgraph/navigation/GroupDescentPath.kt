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

/**
 * A descent from the user's current `GroupScreen` to a specific placement of a component
 * nested below it.
 *
 * [groupIds] lists the groups to open in order, each nested inside the previous. Both the
 * current location (the anchor) and the final component are excluded — the component is
 * identified by [groupItemId]. Empty [groupIds] means the target is a direct child of the
 * current group.
 */
data class GroupDescentPath(
    val groupIds: List<Long>,
    val groupItemId: Long,
)
