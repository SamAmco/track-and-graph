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
