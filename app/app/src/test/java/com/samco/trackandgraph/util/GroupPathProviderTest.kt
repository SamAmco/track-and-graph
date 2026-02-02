package com.samco.trackandgraph.util

import junit.framework.TestCase.assertEquals
import org.junit.Test

class GroupPathProviderTest {
    private val groups = listOf(
        group(parentId = null),
        group("group1", 1),
        group("group1child1", 2, 1),
        group("group1child2", 3, 1),
        group("group2", 4),
        group("group2child", 5, 4),
        group("group2childChild", 6, 5),
    )

    @Test
    fun `test group path provider`() {
        //PREPARE
        val provider = GroupPathProvider(groups)

        //EXECUTE
        val ans0 = provider.getPathForGroup(0)
        val ans1 = provider.getPathForGroup(1)
        val ans2 = provider.getPathForGroup(2)
        val ans3 = provider.getPathForGroup(3)
        val ans4 = provider.getPathForGroup(4)
        val ans5 = provider.getPathForGroup(5)
        val ans6 = provider.getPathForGroup(6)

        //VERIFY
        assertEquals("/", ans0)
        assertEquals("/group1", ans1)
        assertEquals("/group1/group1child1", ans2)
        assertEquals("/group1/group1child2", ans3)
        assertEquals("/group2", ans4)
        assertEquals("/group2/group2child", ans5)
        assertEquals("/group2/group2child/group2childChild", ans6)
    }

    @Test
    //At one point it was possible to move groups into their own children creating a cycle
    // which lead to an infinite loop when calculating the path. This shouldn't be possible moving forward,
    // but to break the infinite loop we need to handle cycles elegantly.
    fun `test group paths with accidental cycle`() {
        //PREPARE
        val provider = GroupPathProvider(
            listOf(
                group("a", 1, 2),
                group("b", 2, 1),
            )
        )

        //EXECUTE
        val ans1 = provider.getPathForGroup(1)
        val ans2 = provider.getPathForGroup(2)

        //VERIFY
        assertEquals("/b/a", ans1)
        assertEquals("/a/b", ans2)
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

        val provider = GroupPathProvider(groups)

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

        val provider = GroupPathProvider(groups)

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

        val provider = GroupPathProvider(groups)

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

        val provider = GroupPathProvider(groups)

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

        val provider = GroupPathProvider(groups)

        assertEquals("/a/b/child", provider.getPathForGroup(3))
    }

    @Test
    fun `group whose ancestor has multiple parents shows all paths`() {
        // Structure:
        // Root
        // ├── c (id=1)
        // │   └── b (id=3)
        // │       └── child (id=4)
        // └── d (id=2)
        //     └── b (id=3, same b in both c and d)
        //         └── child (id=4)
        //
        // child's path should show both /c/b/child and /d/b/child collapsed
        val groups = listOf(
            group(parentId = null),
            group("c", 1, 0),
            group("d", 2, 0),
            group("b", 3, parentIds = setOf(1L, 2L)),
            group("child", 4, 3),
        )

        val provider = GroupPathProvider(groups)

        // b is in both c and d, so child has two paths: /c/b/child and /d/b/child
        // Common prefix: none (c vs d differ)
        // Common suffix: /b/child
        assertEquals(".../b/child", provider.getPathForGroup(4))
    }

    @Test
    fun `group with parents at different depths shows ellipsis`() {
        // Structure:
        // Root (0)
        // ├── a (1, parent of target)
        // ├── d (2)
        // │   └── c (3, parent of target)
        // └── x (4)
        //     └── y (5, parent of target)
        //
        // target has paths: /a, /d/c/a, /x/y/a
        // Should show /.../a since paths have different lengths
        val groups = listOf(
            group(parentId = null),
            group("d", 2, 0),
            group("c", 3, 2),
            group("x", 4, 0),
            group("y", 5, 4),
            group("a", 1, parentIds = setOf(0L, 3L, 5L)),
        )

        val provider = GroupPathProvider(groups)

        assertEquals(".../a", provider.getPathForGroup(1))
    }
}