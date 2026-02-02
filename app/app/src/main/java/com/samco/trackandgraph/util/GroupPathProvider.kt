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

package com.samco.trackandgraph.util

import com.samco.trackandgraph.data.database.dto.Group

open class GroupPathProvider(
    val groups: Collection<Group>,
) {
    protected val separator = "/"

    private val groupsById = groups.associateBy { it.id }

    private val groupPathSegments: Map<Long, List<String>> = groups
        .associate { group ->
            group.id to run {
                val chain = mutableListOf<Group>()
                var currentGroup = group
                do {
                    chain.add(currentGroup)
                    // For path calculation, use the first parent (primary parent)
                    val parentId = currentGroup.parentGroupIds.firstOrNull()
                    currentGroup = groupsById[parentId] ?: break

                    // It shouldn't be possible in the current version to create an infinite
                    // loop, but legacy versions may have a bug that allows this, so we must break
                    // out of the loop if we detect it.
                } while (!chain.contains(currentGroup))
                return@run chain
            }
        }
        .mapValues { (_, chain) ->
            chain
                //Iterate from root to leaf
                .asReversed()
                //Ignore the root group, as it has no name
                .filter { it.parentGroupIds.isNotEmpty() }
                .map { it.name }
        }

    protected fun getPathSegmentsForGroup(id: Long): List<String> = groupPathSegments[id] ?: emptyList()

    fun getPathForGroup(id: Long): String {
        val segments = groupPathSegments[id] ?: return ""
        return segments.joinToString(separator = separator, prefix = separator)
    }
}