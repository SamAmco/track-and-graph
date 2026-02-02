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
import timber.log.Timber

open class GroupPathProvider(
    val groups: Collection<Group>,
) {
    protected val separator = "/"

    /**
     * A single path from root to a group, represented as ordered segment names.
     * e.g. for path "/a/b/c", segments would be ["a", "b", "c"]
     */
    private data class Path(val segments: List<String>) {
        val isEmpty get() = segments.isEmpty()
        val size get() = segments.size
        operator fun get(index: Int) = segments[index]
        operator fun plus(name: String) = Path(segments + name)
        fun joinToString(separator: String, prefix: String) =
            segments.joinToString(separator, prefix = prefix)
    }

    /**
     * All possible paths from root to a group.
     * A group with multiple parents will have multiple paths.
     */
    private data class GroupPaths(val paths: List<Path>) {
        companion object {
            val EMPTY = GroupPaths(listOf(Path(emptyList())))
        }

        val first get() = paths.firstOrNull() ?: Path(emptyList())
        val nonEmptyPaths get() = paths.filter { !it.isEmpty }
    }

    private val groupsById = groups.associateBy { it.id }

    /**
     * For each group, all possible paths from root to that group.
     * Computed lazily. Each group is computed independently to avoid
     * caching intermediate results from cycle detection.
     */
    private val groupPathsById: Map<Long, GroupPaths> by lazy {
        groups.associate { group ->
            group.id to computeAllPathsForGroup(group.id, mutableSetOf())
        }
    }

    /**
     * Recursively compute all possible paths for a group.
     * Each parent may itself have multiple paths, so we need to explore all of them.
     *
     * @param groupId The group to compute paths for
     * @param visiting Set of group IDs currently being visited (for cycle detection)
     */
    private fun computeAllPathsForGroup(
        groupId: Long,
        visiting: MutableSet<Long>
    ): GroupPaths {
        val group = groupsById[groupId] ?: return GroupPaths.EMPTY

        // Cycle detection - treat as reaching root (return empty path)
        if (groupId in visiting) {
            Timber.e("Cycle detected for group $groupId ${group.name}, visiting: $visiting")
            return GroupPaths.EMPTY
        }

        val parentIds = group.parentGroupIds
        if (parentIds.isEmpty()) {
            return GroupPaths.EMPTY
        }

        visiting.add(groupId)

        val allPaths = parentIds.flatMap { parentId ->
            val parentPaths = computeAllPathsForGroup(parentId, visiting)
            // Append this group's name to each of the parent's paths
            parentPaths.paths.map { it + group.name }
        }

        visiting.remove(groupId)

        return if (allPaths.isEmpty()) {
            GroupPaths(listOf(Path(listOf(group.name))))
        } else {
            GroupPaths(allPaths)
        }
    }

    /**
     * Get all possible paths for a group as lists of segments.
     * A group with multiple parents will have multiple paths.
     */
    protected fun getAllPathSegmentsForGroup(id: Long): List<List<String>> {
        return (groupPathsById[id] ?: GroupPaths.EMPTY).paths.map { it.segments }
    }

    /**
     * Get all possible paths for a group.
     */
    private fun getGroupPaths(id: Long): GroupPaths {
        return groupPathsById[id] ?: GroupPaths.EMPTY
    }

    fun getPathForGroup(id: Long): String {
        val groupPaths = getGroupPaths(id)
        val nonEmpty = groupPaths.nonEmptyPaths

        if (nonEmpty.isEmpty()) {
            return separator // Root group
        }

        if (nonEmpty.size == 1) {
            return nonEmpty.first().joinToString(separator, prefix = separator)
        }

        return computeCollapsedPathInternal(nonEmpty)
    }

    /**
     * Collapse multiple paths into a single display string using ellipsis.
     * e.g. [["a", "b", "x", "z"], ["a", "b", "y", "z"]] -> "/a/b/.../z"
     */
    protected fun computeCollapsedPath(paths: List<List<String>>): String {
        return computeCollapsedPathInternal(paths.map { Path(it) })
    }

    private fun computeCollapsedPathInternal(paths: List<Path>): String {
        if (paths.isEmpty()) return ""
        if (paths.size == 1) return paths.first().joinToString(separator, prefix = separator)

        // If all paths are identical, return without ellipsis
        if (paths.all { it.segments == paths.first().segments }) {
            return paths.first().joinToString(separator, prefix = separator)
        }

        val prefix = findLongestCommonPrefix(paths)
        val suffix = findLongestCommonSuffix(paths)

        val suffixStr = suffix.joinToString(separator, prefix = separator)

        return if (prefix.isEmpty()) {
            "...$suffixStr"
        } else {
            val prefixStr = prefix.joinToString(separator, prefix = separator)
            "$prefixStr$separator...$suffixStr"
        }
    }

    private fun findLongestCommonPrefix(paths: List<Path>): List<String> {
        if (paths.isEmpty()) return emptyList()

        val firstPath = paths.first()
        val minLength = paths.minOf { it.size }
        val result = mutableListOf<String>()

        for (i in 0 until minLength) {
            val segment = firstPath[i]
            if (paths.all { it[i] == segment }) {
                result.add(segment)
            } else {
                break
            }
        }

        return result
    }

    private fun findLongestCommonSuffix(paths: List<Path>): List<String> {
        if (paths.isEmpty()) return emptyList()

        val firstPath = paths.first()
        val minLength = paths.minOf { it.size }
        val result = mutableListOf<String>()

        for (i in 1..minLength) {
            val segment = firstPath[firstPath.size - i]
            if (paths.all { it[it.size - i] == segment }) {
                result.add(0, segment)
            } else {
                break
            }
        }

        return result
    }
}