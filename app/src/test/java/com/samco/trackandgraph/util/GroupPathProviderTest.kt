package com.samco.trackandgraph.util

import junit.framework.Assert
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
        Assert.assertEquals("/", ans0)
        Assert.assertEquals("/group1", ans1)
        Assert.assertEquals("/group1/group1child1", ans2)
        Assert.assertEquals("/group1/group1child2", ans3)
        Assert.assertEquals("/group2", ans4)
        Assert.assertEquals("/group2/group2child", ans5)
        Assert.assertEquals("/group2/group2child/group2childChild", ans6)
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
        Assert.assertEquals("/b/a", ans1)
        Assert.assertEquals("/a/b", ans2)
    }

    @Test
    fun `test group path provider filters out groups with the group filter id`() {
        //PREPARE
        val provider = GroupPathProvider(groups, groupFilterId = 4)

        //VERIFY
        Assert.assertEquals(
            listOf(0L, 1L, 2L, 3L),
            provider.filteredSortedGroups.map { it.group.id }
        )
    }

    @Test
    fun `filtered sorted groups come in alphabetical order`() {
        //PREPARE
        val unsortedGroups = listOf(
            group(parentId = null),
            group("Apple", 2, 0),
            group("Zebra", 1, 0),
            group("Apple", 5, 1),
            group("Zebra", 6, 2),
            group("Banana", 3, 0),
            group("Carrot", 4, 0)
        )
        val provider = GroupPathProvider(unsortedGroups)

        //EXECUTE
        val sortedGroups = provider.filteredSortedGroups.map { it.path }

        //VERIFY
        Assert.assertEquals(
            listOf("/", "/Apple", "/Apple/Zebra", "/Banana", "/Carrot", "/Zebra", "/Zebra/Apple"),
            sortedGroups
        )
    }
}