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

package com.samco.trackandgraph.ui

import com.samco.trackandgraph.database.entity.Group

open class GroupPathProvider(
    val groups: List<Group>
) {
    private val separator = "/"

    private val groupsById = groups.map { it.id to it }.toMap()

    protected val groupPaths = groups.map { group ->
        group.id to run {
            if (group.parentGroupId == null) return@run separator
            var path = ""
            var currentGroup = group
            while (true) {
                val parentId = currentGroup.parentGroupId ?: break
                path = separator + currentGroup.name + path
                currentGroup = groupsById[parentId] ?: break
            }
            return@run path
        }
    }.toMap()

    fun getPathForGroup(id: Long): String = groupPaths[id] ?: ""
}