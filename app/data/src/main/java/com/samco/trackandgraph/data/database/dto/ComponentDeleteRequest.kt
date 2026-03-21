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
 * Request object for deleting any component (tracker, function, graph, group, or reminder).
 *
 * @param groupItemId The specific GroupItem placement to act on. The component's identity
 *   (type and ID) is derived from this GroupItem.
 * @param deleteEverywhere If true, deletes the component and all its placements.
 *   If false and the component has multiple placements, only the placement identified
 *   by [groupItemId] is removed. If false and the component is unique (only one placement),
 *   the component itself is still deleted — there is no distinction between "remove placement"
 *   and "delete everywhere" when only one placement exists.
 */
data class ComponentDeleteRequest(
    val groupItemId: Long,
    val deleteEverywhere: Boolean = false,
)
