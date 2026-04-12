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
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.FunctionGraph
import com.samco.trackandgraph.data.database.dto.FunctionGraphNode
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.database.dto.NodeDependency
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class ComponentPathProviderTest {

    // ── Helper functions ──

    private var groupItemIdCounter = 1000L
    private fun giid() = groupItemIdCounter++

    private fun testGroup(id: Long, name: String) = Group(id, name, 0, unique = true)

    private fun testTracker(id: Long, name: String) = Tracker(
        id = id,
        name = name,
        featureId = id,
        description = "",
        dataType = DataType.CONTINUOUS,
        hasDefaultValue = false,
        defaultValue = 0.0,
        defaultLabel = "",
        suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
        suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
    )

    private fun testFunction(id: Long, name: String) = Function(
        id = id,
        featureId = id + 1000,
        name = name,
        description = "",
        functionGraph = FunctionGraph(
            nodes = emptyList(),
            outputNode = FunctionGraphNode.OutputNode(0f, 0f, 0, emptyList()),
            isDuration = false,
        ),
        inputFeatureIds = emptyList(),
        unique = true,
    )

    private fun testGraphOrStat(id: Long, name: String) = GraphOrStat(
        id = id,
        name = name,
        type = GraphStatType.LINE_GRAPH,
        unique = true,
    )

    private fun emptyGroupGraph(id: Long = 0, name: String = "Root") = GroupGraph(
        group = testGroup(id, name),
        children = emptyList(),
    )

    // ── Root group tests ──

    @Test
    fun `root group path is slash`() {
        val provider = ComponentPathProvider(emptyGroupGraph())

        assertEquals(listOf("/"), provider.getAllPathsForGroup(0))
    }

    @Test
    fun `root group with no matching id returns empty list`() {
        val provider = ComponentPathProvider(emptyGroupGraph())

        assertEquals(emptyList<String>(), provider.getAllPathsForGroup(999))
    }

    // ── Single-level children ──

    @Test
    fun `tracker in root group`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.TrackerNode(giid(), testTracker(1, "Steps"))
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/Steps"), provider.getAllPathsForTracker(1))
    }

    @Test
    fun `function in root group`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.FunctionNode(giid(), testFunction(1, "MyFunc"))
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/MyFunc"), provider.getAllPathsForFunction(1))
    }

    @Test
    fun `graph in root group`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GraphNode(giid(), testGraphOrStat(1, "MyGraph"))
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/MyGraph"), provider.getAllPathsForGraph(1))
    }

    // ── Nested hierarchy ──

    @Test
    fun `tracker in nested group`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "Health"),
                        children = listOf(
                            GroupGraphItem.GroupNode(giid(), 
                                GroupGraph(
                                    group = testGroup(2, "Exercise"),
                                    children = listOf(
                                        GroupGraphItem.TrackerNode(giid(), testTracker(10, "Steps"))
                                    ),
                                )
                            )
                        ),
                    )
                )
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/Health/Exercise/Steps"), provider.getAllPathsForTracker(10))
        assertEquals(listOf("/Health/Exercise"), provider.getAllPathsForGroup(2))
        assertEquals(listOf("/Health"), provider.getAllPathsForGroup(1))
    }

    @Test
    fun `nested group path`() {
        val groups = listOf(
            group(parentId = null),
            group("group1", 1),
            group("group1child1", 2, 1),
            group("group1child2", 3, 1),
            group("group2", 4),
            group("group2child", 5, 4),
        )

        val provider = ComponentPathProvider(buildGroupGraph(groups))

        assertEquals(listOf("/"), provider.getAllPathsForGroup(0))
        assertEquals(listOf("/group1"), provider.getAllPathsForGroup(1))
        assertEquals(listOf("/group1/group1child1"), provider.getAllPathsForGroup(2))
        assertEquals(listOf("/group1/group1child2"), provider.getAllPathsForGroup(3))
        assertEquals(listOf("/group2"), provider.getAllPathsForGroup(4))
        assertEquals(listOf("/group2/group2child"), provider.getAllPathsForGroup(5))
    }

    // ── Symlinks (DAG: same node appears in multiple parents) ──

    @Test
    fun `group in two parents returns both paths`() {
        // Root
        // ├── a (1)
        // │   └── shared (3)
        // └── b (2)
        //     └── shared (3)
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 0),
            group("shared", 3, parentIds = setOf(1L, 2L)),
        )

        val provider = ComponentPathProvider(buildGroupGraph(groups))

        val paths = provider.getAllPathsForGroup(3)
        assertEquals(2, paths.size)
        assert(paths.contains("/a/shared")) { "Expected /a/shared in $paths" }
        assert(paths.contains("/b/shared")) { "Expected /b/shared in $paths" }
    }

    @Test
    fun `tracker in group with two parents returns both paths`() {
        // Root
        // ├── a (1)
        // │   └── shared (3)
        // │       └── tracker (10)
        // └── b (2)
        //     └── shared (3)
        //         └── tracker (10)
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 0),
            group("shared", 3, parentIds = setOf(1L, 2L)),
        )

        val features = listOf(feature(10L, "tracker", 3))

        val provider = ComponentPathProvider(buildGroupGraph(groups, features))

        val paths = provider.getAllPathsForTracker(10)
        assertEquals(2, paths.size)
        assert(paths.contains("/a/shared/tracker")) { "Expected /a/shared/tracker in $paths" }
        assert(paths.contains("/b/shared/tracker")) { "Expected /b/shared/tracker in $paths" }
    }

    @Test
    fun `group in three parents returns three paths`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 0),
            group("c", 3, 0),
            group("shared", 4, parentIds = setOf(1L, 2L, 3L)),
        )

        val provider = ComponentPathProvider(buildGroupGraph(groups))

        val paths = provider.getAllPathsForGroup(4)
        assertEquals(3, paths.size)
        assert(paths.contains("/a/shared"))
        assert(paths.contains("/b/shared"))
        assert(paths.contains("/c/shared"))
    }

    @Test
    fun `deeply nested symlink returns all paths with full depth`() {
        // Root
        // ├── a (1)
        // │   └── x (3)
        // │       └── shared (5)
        // └── b (2)
        //     └── y (4)
        //         └── shared (5)
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 0),
            group("x", 3, 1),
            group("y", 4, 2),
            group("shared", 5, parentIds = setOf(3L, 4L)),
        )

        val provider = ComponentPathProvider(buildGroupGraph(groups))

        val paths = provider.getAllPathsForGroup(5)
        assertEquals(2, paths.size)
        assert(paths.contains("/a/x/shared"))
        assert(paths.contains("/b/y/shared"))
    }

    @Test
    fun `parent group with multiple parents propagates all paths to children`() {
        // Root
        // ├── c (1)
        // │   └── b (3)
        // │       └── child (4)
        // └── d (2)
        //     └── b (3)
        //         └── child (4)
        val groups = listOf(
            group(parentId = null),
            group("c", 1, 0),
            group("d", 2, 0),
            group("b", 3, parentIds = setOf(1L, 2L)),
            group("child", 4, 3),
        )

        val provider = ComponentPathProvider(buildGroupGraph(groups))

        val paths = provider.getAllPathsForGroup(4)
        assertEquals(2, paths.size)
        assert(paths.contains("/c/b/child"))
        assert(paths.contains("/d/b/child"))
    }

    // ── All component types together ──

    @Test
    fun `all component types are indexed correctly`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "Sub"),
                        children = listOf(
                            GroupGraphItem.TrackerNode(giid(), testTracker(10, "MyTracker")),
                            GroupGraphItem.FunctionNode(giid(), testFunction(20, "MyFunc")),
                            GroupGraphItem.GraphNode(giid(), testGraphOrStat(30, "MyGraph")),
                        ),
                    )
                )
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/Sub/MyTracker"), provider.getAllPathsForTracker(10))
        assertEquals(listOf("/Sub/MyFunc"), provider.getAllPathsForFunction(20))
        assertEquals(listOf("/Sub/MyGraph"), provider.getAllPathsForGraph(30))
        assertEquals(listOf("/Sub"), provider.getAllPathsForGroup(1))
    }

    // ── Empty / missing ID queries ──

    @Test
    fun `querying non-existent tracker returns empty list`() {
        val provider = ComponentPathProvider(emptyGroupGraph())

        assertEquals(emptyList<String>(), provider.getAllPathsForTracker(999))
    }

    @Test
    fun `querying non-existent function returns empty list`() {
        val provider = ComponentPathProvider(emptyGroupGraph())

        assertEquals(emptyList<String>(), provider.getAllPathsForFunction(999))
    }

    @Test
    fun `querying non-existent graph returns empty list`() {
        val provider = ComponentPathProvider(emptyGroupGraph())

        assertEquals(emptyList<String>(), provider.getAllPathsForGraph(999))
    }

    @Test
    fun `root with no children has only root group path`() {
        val provider = ComponentPathProvider(emptyGroupGraph())

        assertEquals(listOf("/"), provider.getAllPathsForGroup(0))
        assertEquals(emptyList<String>(), provider.getAllPathsForTracker(0))
        assertEquals(emptyList<String>(), provider.getAllPathsForFunction(0))
        assertEquals(emptyList<String>(), provider.getAllPathsForGraph(0))
    }

    // ── Edge cases ──

    @Test
    fun `component with special characters in name`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.TrackerNode(giid(), testTracker(1, "Weight (kg)")),
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(2, "My/Folder"),
                        children = listOf(
                            GroupGraphItem.TrackerNode(giid(), testTracker(3, "A & B")),
                        ),
                    )
                ),
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/Weight (kg)"), provider.getAllPathsForTracker(1))
        assertEquals(listOf("/My/Folder/A & B"), provider.getAllPathsForTracker(3))
    }

    @Test
    fun `component with empty name`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.TrackerNode(giid(), testTracker(1, ""))
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/"), provider.getAllPathsForTracker(1))
    }

    @Test
    fun `multiple trackers in same group get independent paths`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "Health"),
                        children = listOf(
                            GroupGraphItem.TrackerNode(giid(), testTracker(10, "Steps")),
                            GroupGraphItem.TrackerNode(giid(), testTracker(11, "Weight")),
                            GroupGraphItem.TrackerNode(giid(), testTracker(12, "Sleep")),
                        ),
                    )
                )
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/Health/Steps"), provider.getAllPathsForTracker(10))
        assertEquals(listOf("/Health/Weight"), provider.getAllPathsForTracker(11))
        assertEquals(listOf("/Health/Sleep"), provider.getAllPathsForTracker(12))
    }

    @Test
    fun `deeply nested hierarchy produces correct full paths`() {
        val d = GroupGraph(
            group = testGroup(4, "d"),
            children = listOf(GroupGraphItem.TrackerNode(giid(), testTracker(10, "deep"))),
        )
        val c = GroupGraph(
            group = testGroup(3, "c"),
            children = listOf(GroupGraphItem.GroupNode(giid(), d)),
        )
        val b = GroupGraph(
            group = testGroup(2, "b"),
            children = listOf(GroupGraphItem.GroupNode(giid(), c)),
        )
        val a = GroupGraph(
            group = testGroup(1, "a"),
            children = listOf(GroupGraphItem.GroupNode(giid(), b)),
        )
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(GroupGraphItem.GroupNode(giid(), a)),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/a/b/c/d"), provider.getAllPathsForGroup(4))
        assertEquals(listOf("/a/b/c/d/deep"), provider.getAllPathsForTracker(10))
    }

    @Test
    fun `same group appearing as sibling groups with same name`() {
        // Two different groups with same name under root
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "same"),
                        children = emptyList(),
                    )
                ),
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(2, "same"),
                        children = emptyList(),
                    )
                ),
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/same"), provider.getAllPathsForGroup(1))
        assertEquals(listOf("/same"), provider.getAllPathsForGroup(2))
    }

    @Test
    fun `graph and function with same id are tracked independently`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.FunctionNode(giid(), testFunction(1, "Func")),
                GroupGraphItem.GraphNode(giid(), testGraphOrStat(1, "Graph")),
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/Func"), provider.getAllPathsForFunction(1))
        assertEquals(listOf("/Graph"), provider.getAllPathsForGraph(1))
    }

    @Test
    fun `tracker and function with same id are tracked independently`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.TrackerNode(giid(), testTracker(1, "Tracker")),
                GroupGraphItem.FunctionNode(giid(), testFunction(1, "Func")),
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/Tracker"), provider.getAllPathsForTracker(1))
        assertEquals(listOf("/Func"), provider.getAllPathsForFunction(1))
    }

    @Test
    fun `symlinked group with mixed component types propagates all paths`() {
        // Root
        // ├── a (1)
        // │   └── shared (3) -- contains tracker, function, graph
        // └── b (2)
        //     └── shared (3)
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 0),
            group("shared", 3, parentIds = setOf(1L, 2L)),
        )

        // Build using buildGroupGraph for groups, then manually add function/graph nodes
        // Since buildGroupGraph only supports trackers, build the graph manually
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "a"),
                        children = listOf(
                            GroupGraphItem.GroupNode(giid(), 
                                GroupGraph(
                                    group = testGroup(3, "shared"),
                                    children = listOf(
                                        GroupGraphItem.TrackerNode(giid(), testTracker(10, "t1")),
                                        GroupGraphItem.FunctionNode(giid(), testFunction(20, "f1")),
                                        GroupGraphItem.GraphNode(giid(), testGraphOrStat(30, "g1")),
                                    ),
                                )
                            )
                        ),
                    )
                ),
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(2, "b"),
                        children = listOf(
                            GroupGraphItem.GroupNode(giid(), 
                                GroupGraph(
                                    group = testGroup(3, "shared"),
                                    children = listOf(
                                        GroupGraphItem.TrackerNode(giid(), testTracker(10, "t1")),
                                        GroupGraphItem.FunctionNode(giid(), testFunction(20, "f1")),
                                        GroupGraphItem.GraphNode(giid(), testGraphOrStat(30, "g1")),
                                    ),
                                )
                            )
                        ),
                    )
                ),
            ),
        )
        val provider = ComponentPathProvider(graph)

        val trackerPaths = provider.getAllPathsForTracker(10)
        assertEquals(2, trackerPaths.size)
        assert(trackerPaths.contains("/a/shared/t1"))
        assert(trackerPaths.contains("/b/shared/t1"))

        val funcPaths = provider.getAllPathsForFunction(20)
        assertEquals(2, funcPaths.size)
        assert(funcPaths.contains("/a/shared/f1"))
        assert(funcPaths.contains("/b/shared/f1"))

        val graphPaths = provider.getAllPathsForGraph(30)
        assertEquals(2, graphPaths.size)
        assert(graphPaths.contains("/a/shared/g1"))
        assert(graphPaths.contains("/b/shared/g1"))
    }

    // ── Same-group duplicate placements ──

    @Test
    fun `tracker placed twice in same group produces single deduplicated path`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "Health"),
                        children = listOf(
                            GroupGraphItem.TrackerNode(giid(), testTracker(10, "Steps")),
                            GroupGraphItem.TrackerNode(giid(), testTracker(10, "Steps")),
                        ),
                    )
                )
            ),
        )
        val provider = ComponentPathProvider(graph)

        val paths = provider.getAllPathsForTracker(10)
        assertEquals(listOf("/Health/Steps"), paths)
    }

    @Test
    fun `group placed twice in same parent produces single deduplicated path`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "Sub"),
                        children = emptyList(),
                    )
                ),
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "Sub"),
                        children = emptyList(),
                    )
                ),
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/Sub"), provider.getAllPathsForGroup(1))
    }

    @Test
    fun `function placed twice in same group produces single deduplicated path`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.FunctionNode(giid(), testFunction(1, "MyFunc")),
                GroupGraphItem.FunctionNode(giid(), testFunction(1, "MyFunc")),
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/MyFunc"), provider.getAllPathsForFunction(1))
    }

    @Test
    fun `graph placed twice in same group produces single deduplicated path`() {
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GraphNode(giid(), testGraphOrStat(1, "MyGraph")),
                GroupGraphItem.GraphNode(giid(), testGraphOrStat(1, "MyGraph")),
            ),
        )
        val provider = ComponentPathProvider(graph)

        assertEquals(listOf("/MyGraph"), provider.getAllPathsForGraph(1))
    }

    @Test
    fun `tracker in two different groups plus duplicated in one still shows only distinct paths`() {
        // Root
        // ├── a (1)
        // │   ├── Steps (10)
        // │   └── Steps (10) ← same-group duplicate
        // └── b (2)
        //     └── Steps (10)
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = listOf(
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(1, "a"),
                        children = listOf(
                            GroupGraphItem.TrackerNode(giid(), testTracker(10, "Steps")),
                            GroupGraphItem.TrackerNode(giid(), testTracker(10, "Steps")),
                        ),
                    )
                ),
                GroupGraphItem.GroupNode(giid(), 
                    GroupGraph(
                        group = testGroup(2, "b"),
                        children = listOf(
                            GroupGraphItem.TrackerNode(giid(), testTracker(10, "Steps")),
                        ),
                    )
                ),
            ),
        )
        val provider = ComponentPathProvider(graph)

        val paths = provider.getAllPathsForTracker(10)
        assertEquals(2, paths.size)
        assert(paths.contains("/a/Steps")) { "Expected /a/Steps in $paths" }
        assert(paths.contains("/b/Steps")) { "Expected /b/Steps in $paths" }
    }

    @Test
    fun `large flat group with many children`() {
        val children = (1..20L).map { i ->
            GroupGraphItem.TrackerNode(giid(), testTracker(i, "tracker$i"))
        }
        val graph = GroupGraph(
            group = testGroup(0, "Root"),
            children = children,
        )
        val provider = ComponentPathProvider(graph)

        for (i in 1..20L) {
            assertEquals(listOf("/tracker$i"), provider.getAllPathsForTracker(i))
        }
    }
}
