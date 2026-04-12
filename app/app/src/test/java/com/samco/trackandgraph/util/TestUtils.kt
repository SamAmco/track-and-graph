/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.util

import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType

data class TestGroup(
    val name: String,
    val id: Long,
    val parentIds: Set<Long>,
)

data class TestFeature(
    val featureId: Long,
    val name: String,
    val groupIds: Set<Long>,
)

fun group(name: String = "", id: Long = 0, parentId: Long? = null) =
    TestGroup(name, id, if (parentId == null) emptySet() else setOf(parentId))

fun group(name: String, id: Long) =
    TestGroup(name, id, setOf(0L))

fun group(name: String, id: Long, parentIds: Set<Long>) =
    TestGroup(name, id, parentIds)

fun feature(featureId: Long, name: String, groupId: Long) =
    TestFeature(featureId, name, setOf(groupId))

fun feature(featureId: Long, name: String, groupIds: Set<Long>) =
    TestFeature(featureId, name, groupIds)

/**
 * Build a GroupGraph from flat group and feature descriptions.
 * Groups with no parents are treated as roots (there should be exactly one).
 * A group or feature can appear under multiple parents, producing multiple paths in the tree.
 */
fun buildGroupGraph(
    groups: List<TestGroup>,
    features: List<TestFeature> = emptyList(),
): GroupGraph {
    val groupsById = groups.associateBy { it.id }

    // Find root (group with no parents). If none (cycle), pick the first group.
    val roots = groups.filter { it.parentIds.isEmpty() }
    val root = when {
        roots.size == 1 -> roots.first()
        roots.isEmpty() -> groups.first().let { TestGroup(it.name, it.id, emptySet()) }
        else -> error("Expected at most one root group, found ${roots.size}")
    }

    // Build child-parent index: for each group, which groups are its children
    val childGroupsByParent = mutableMapOf<Long, MutableList<Long>>()
    for (g in groups) {
        for (parentId in g.parentIds) {
            childGroupsByParent.getOrPut(parentId) { mutableListOf() }.add(g.id)
        }
    }

    // Build feature-parent index: for each group, which features are in it
    val featuresByGroup = mutableMapOf<Long, MutableList<TestFeature>>()
    for (f in features) {
        for (groupId in f.groupIds) {
            featuresByGroup.getOrPut(groupId) { mutableListOf() }.add(f)
        }
    }

    var nextGroupItemId = 1000L

    fun buildNode(groupId: Long, visited: MutableSet<Long>): GroupGraph {
        val g = groupsById[groupId]!!
        val children = mutableListOf<GroupGraphItem>()

        // Cycle detection
        if (groupId in visited) {
            return GroupGraph(Group(g.id, g.name, 0, unique = true), emptyList())
        }
        visited.add(groupId)

        // Add child groups
        for (childId in childGroupsByParent[groupId] ?: emptyList()) {
            children.add(GroupGraphItem.GroupNode(nextGroupItemId++, buildNode(childId, visited.toMutableSet())))
        }

        // Add features as trackers
        for (f in featuresByGroup[groupId] ?: emptyList()) {
            children.add(
                GroupGraphItem.TrackerNode(
                    groupItemId = nextGroupItemId++,
                    tracker = Tracker(
                        id = f.featureId,
                        name = f.name,
                        featureId = f.featureId,
                        description = "",
                        dataType = DataType.CONTINUOUS,
                        hasDefaultValue = false,
                        defaultValue = 0.0,
                        defaultLabel = "",
                        suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                        suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
                    )
                )
            )
        }

        visited.remove(groupId)
        return GroupGraph(Group(g.id, g.name, 0, unique = true), children)
    }

    return buildNode(root.id, mutableSetOf())
}
