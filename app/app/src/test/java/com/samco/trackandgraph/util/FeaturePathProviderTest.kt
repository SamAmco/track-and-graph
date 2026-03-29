package com.samco.trackandgraph.util

import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupGraph
import com.samco.trackandgraph.data.database.dto.GroupGraphItem
import com.samco.trackandgraph.data.database.dto.Tracker
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import org.junit.Assert.assertEquals
import org.junit.Test

class FeaturePathProviderTest {

    // ── Feature path tests ──

    @Test
    fun test_feature_path_provider() {
        val groups = listOf(
            group(parentId = null),
            group("group1", 1),
            group("group1child1", 2, 1),
            group("group1child2", 3, 1),
        )

        val features = listOf(
            feature(0L, "Test", 0),
            feature(1L, "Test2", 1),
            feature(2L, "Test3", 2),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups, features))

        assertEquals("/Test", provider.getPathForFeature(0))
        assertEquals("/group1/Test2", provider.getPathForFeature(1))
        assertEquals("/group1/group1child1/Test3", provider.getPathForFeature(2))
    }

    @Test
    fun `feature path provider sorted feature map should be sorted alphabetically`() {
        val groups = listOf(
            group(parentId = null),
            group("Apple", 2, 0),
            group("Banana", 1, 0),
            group("Zebra", 3, 0),
        )

        val features = listOf(
            feature(0L, "Zebra", 0),
            feature(1L, "Apple", 1),
            feature(2L, "Banana", 2),
            feature(3L, "Carrot", 3),
            feature(4L, "Banana", 0),
        )

        val sorted = FeaturePathProvider(buildGroupGraph(groups, features)).sortedFeatureMap()

        assertEquals(
            listOf(
                2L to "/Apple/Banana",
                4L to "/Banana",
                1L to "/Banana/Apple",
                0L to "/Zebra",
                3L to "/Zebra/Carrot",
            ),
            sorted.map { it.key to it.value }
        )
    }

    @Test
    fun `a feature in a nested group with no other features in it gives a full path`() {
        val groups = listOf(
            group(parentId = null),
            group("Apple", 1, 0),
            group("Banana", 2, 1),
        )

        val features = listOf(feature(1L, "Carrot", 2))

        val sorted = FeaturePathProvider(buildGroupGraph(groups, features)).sortedFeatureMap()

        assertEquals(
            listOf(1L to "/Apple/Banana/Carrot"),
            sorted.map { it.key to it.value }
        )
    }

    @Test
    fun `feature in two groups with common prefix shows collapsed path`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
            group("c", 3, 1),
        )

        val features = listOf(
            feature(1L, "feat", setOf(2L, 3L))
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups, features))

        assertEquals("/a/.../feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in two groups with no common prefix shows ellipsis after root`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
            group("c", 3, 0),
            group("d", 4, 3),
        )

        val features = listOf(
            feature(1L, "feat", setOf(2L, 4L))
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups, features))

        assertEquals(".../feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in multiple groups with longer common prefix`() {
        val groups = listOf(
            group(parentId = null),
            group("path", 1, 0),
            group("to", 2, 1),
            group("ga", 3, 2),
            group("gb", 4, 2),
        )

        val features = listOf(
            feature(1L, "feat", setOf(3L, 4L))
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups, features))

        assertEquals("/path/to/.../feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in three groups`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("x", 2, 1),
            group("y", 3, 1),
            group("z", 4, 1),
        )

        val features = listOf(
            feature(1L, "feat", setOf(2L, 3L, 4L))
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups, features))

        assertEquals("/a/.../feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in groups with identical paths returns single path without ellipsis`() {
        val groups = listOf(
            group(parentId = null),
            group("same", 1, 0),
            group("same", 2, 0),
        )

        val features = listOf(
            feature(1L, "feat", setOf(1L, 2L))
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups, features))

        assertEquals("/same/feat", provider.getPathForFeature(1L))
    }

    @Test
    fun `feature in single group whose parent has multiple parents shows collapsed path`() {
        // Root (0)
        // ├── c (1)
        // │   └── b (3)
        // │       └── feat
        // └── d (2)
        //     └── b (3, same b in both c and d)
        //         └── feat
        val groups = listOf(
            group(parentId = null),
            group("c", 1, 0),
            group("d", 2, 0),
            group("b", 3, parentIds = setOf(1L, 2L)),
        )

        val features = listOf(
            feature(1L, "feat", setOf(3L))
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups, features))

        assertEquals(".../b/feat", provider.getPathForFeature(1L))
    }

    // ── Group path tests ──

    @Test
    fun `test group path provider`() {
        val groups = listOf(
            group(parentId = null),
            group("group1", 1),
            group("group1child1", 2, 1),
            group("group1child2", 3, 1),
            group("group2", 4),
            group("group2child", 5, 4),
            group("group2childChild", 6, 5),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        assertEquals("/", provider.getPathForGroup(0))
        assertEquals("/group1", provider.getPathForGroup(1))
        assertEquals("/group1/group1child1", provider.getPathForGroup(2))
        assertEquals("/group1/group1child2", provider.getPathForGroup(3))
        assertEquals("/group2", provider.getPathForGroup(4))
        assertEquals("/group2/group2child", provider.getPathForGroup(5))
        assertEquals("/group2/group2child/group2childChild", provider.getPathForGroup(6))
    }

    @Test
    fun `test group paths with accidental cycle`() {
        // At one point it was possible to move groups into their own children creating a cycle.
        // With GroupGraph, the tree builder breaks the cycle via visited-set detection.
        // Group "a" (1) has parent 2, group "b" (2) has parent 1 — a cycle.
        // With no root, we pick group 1 as synthetic root. But group 1 also
        // appears as a child of group 2 (from its original parentId=2), so
        // group 1 has two locations: root and under b. The cycle-detected
        // copy under b has no children.
        val groups = listOf(
            group("a", 1, 2),
            group("b", 2, 1),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        // Group 1 is at root (empty path, filtered out) and at /b/a (cycle broken).
        // Only the non-empty path remains.
        assertEquals("/b/a", provider.getPathForGroup(1))
        assertEquals("/b", provider.getPathForGroup(2))
    }

    @Test
    fun `group in two parents with common prefix shows collapsed path`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
            group("c", 3, 1),
            group("child", 4, parentIds = setOf(2L, 3L)),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        assertEquals("/a/.../child", provider.getPathForGroup(4))
    }

    @Test
    fun `group in two parents with no common prefix shows ellipsis after root`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
            group("c", 3, 0),
            group("d", 4, 3),
            group("child", 5, parentIds = setOf(2L, 4L)),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        assertEquals(".../child", provider.getPathForGroup(5))
    }

    @Test
    fun `group in multiple parents with longer common prefix`() {
        val groups = listOf(
            group(parentId = null),
            group("path", 1, 0),
            group("to", 2, 1),
            group("ga", 3, 2),
            group("gb", 4, 2),
            group("child", 5, parentIds = setOf(3L, 4L)),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        assertEquals("/path/to/.../child", provider.getPathForGroup(5))
    }

    @Test
    fun `group in three parents shows collapsed path`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("x", 2, 1),
            group("y", 3, 1),
            group("z", 4, 1),
            group("child", 5, parentIds = setOf(2L, 3L, 4L)),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        assertEquals("/a/.../child", provider.getPathForGroup(5))
    }

    @Test
    fun `group with single parent in set shows full path`() {
        val groups = listOf(
            group(parentId = null),
            group("a", 1, 0),
            group("b", 2, 1),
            group("child", 3, parentIds = setOf(2L)),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        assertEquals("/a/b/child", provider.getPathForGroup(3))
    }

    @Test
    fun `group whose ancestor has multiple parents shows all paths`() {
        // Root
        // ├── c (id=1)
        // │   └── b (id=3)
        // │       └── child (id=4)
        // └── d (id=2)
        //     └── b (id=3, same b in both c and d)
        //         └── child (id=4)
        val groups = listOf(
            group(parentId = null),
            group("c", 1, 0),
            group("d", 2, 0),
            group("b", 3, parentIds = setOf(1L, 2L)),
            group("child", 4, 3),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        assertEquals(".../b/child", provider.getPathForGroup(4))
    }

    // ── Same-group duplicate placement tests ──

    @Test
    fun `feature placed twice in same group produces single path without ellipsis`() {
        val groups = listOf(
            group(parentId = null),
            group("Health", 1, 0),
        )

        // Same feature placed twice in group 1 via two groupIds entries
        // buildGroupGraph uses a Set so we construct manually
        val graph = GroupGraph(
            group = Group(0, "", 0, unique = true),
            children = listOf(
                GroupGraphItem.GroupNode(
                    GroupGraph(
                        group = Group(1, "Health", 0, unique = true),
                        children = listOf(
                            GroupGraphItem.TrackerNode(
                                Tracker(
                                    id = 10, name = "Steps", featureId = 10,
                                    description = "", dataType = DataType.CONTINUOUS,
                                    hasDefaultValue = false, defaultValue = 0.0, defaultLabel = "",
                                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                                    suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
                                )
                            ),
                            GroupGraphItem.TrackerNode(
                                Tracker(
                                    id = 10, name = "Steps", featureId = 10,
                                    description = "", dataType = DataType.CONTINUOUS,
                                    hasDefaultValue = false, defaultValue = 0.0, defaultLabel = "",
                                    suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
                                    suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
                                )
                            ),
                        ),
                    )
                )
            ),
        )

        val provider = FeaturePathProvider(graph)

        assertEquals("/Health/Steps", provider.getPathForFeature(10))
    }

    @Test
    fun `feature in two groups plus duplicated in one still shows collapsed path with only distinct paths`() {
        // Root
        // ├── a (1)
        // │   ├── Steps (featureId=10)
        // │   └── Steps (featureId=10)  ← same-group duplicate
        // └── b (2)
        //     └── Steps (featureId=10)
        val tracker = Tracker(
            id = 10, name = "Steps", featureId = 10,
            description = "", dataType = DataType.CONTINUOUS,
            hasDefaultValue = false, defaultValue = 0.0, defaultLabel = "",
            suggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
        )
        val graph = GroupGraph(
            group = Group(0, "", 0, unique = true),
            children = listOf(
                GroupGraphItem.GroupNode(
                    GroupGraph(
                        group = Group(1, "a", 0, unique = true),
                        children = listOf(
                            GroupGraphItem.TrackerNode(tracker),
                            GroupGraphItem.TrackerNode(tracker),
                        ),
                    )
                ),
                GroupGraphItem.GroupNode(
                    GroupGraph(
                        group = Group(2, "b", 0, unique = true),
                        children = listOf(
                            GroupGraphItem.TrackerNode(tracker),
                        ),
                    )
                ),
            ),
        )

        val provider = FeaturePathProvider(graph)

        // Should be 2 distinct paths (/a/Steps, /b/Steps), collapsed as ".../Steps"
        assertEquals(".../Steps", provider.getPathForFeature(10))
    }

    @Test
    fun `group placed twice in same parent produces single path`() {
        val graph = GroupGraph(
            group = Group(0, "", 0, unique = true),
            children = listOf(
                GroupGraphItem.GroupNode(
                    GroupGraph(
                        group = Group(1, "Sub", 0, unique = true),
                        children = emptyList(),
                    )
                ),
                GroupGraphItem.GroupNode(
                    GroupGraph(
                        group = Group(1, "Sub", 0, unique = true),
                        children = emptyList(),
                    )
                ),
            ),
        )

        val provider = FeaturePathProvider(graph)

        assertEquals("/Sub", provider.getPathForGroup(1))
    }

    @Test
    fun `group with parents at different depths shows ellipsis`() {
        // Root (0)
        // ├── a (1, parent of target) -- via parentIds
        // ├── d (2)
        // │   └── c (3, parent of target)
        // └── x (4)
        //     └── y (5, parent of target)
        val groups = listOf(
            group(parentId = null),
            group("d", 2, 0),
            group("c", 3, 2),
            group("x", 4, 0),
            group("y", 5, 4),
            group("a", 1, parentIds = setOf(0L, 3L, 5L)),
        )

        val provider = FeaturePathProvider(buildGroupGraph(groups))

        assertEquals(".../a", provider.getPathForGroup(1))
    }
}
