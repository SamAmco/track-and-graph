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
}