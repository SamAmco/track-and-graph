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

import com.samco.trackandgraph.base.database.dto.Feature
import com.samco.trackandgraph.base.database.dto.Group
import com.samco.trackandgraph.util.FeaturePathProvider
import com.samco.trackandgraph.util.GroupPathProvider
import junit.framework.Assert.assertEquals
import org.junit.Test

class PathProviderTest {
    private val groups = listOf(
        group(parentId = null),
        group("group1", 1),
        group("group1child1", 2, 1),
        group("group1child2", 3, 1),
        group("group2", 4),
        group("group2child", 5, 4),
        group("group2childChild", 6, 5),
    )

    private data class FeatureDto(
        override val featureId: Long,
        override val name: String,
        override val groupId: Long,
    ) : Feature {
        override val displayIndex: Int = 0
        override val description: String = ""
    }

    private val features = listOf(
        FeatureDto(0L, "Test", 0),
        FeatureDto(1L, "Test2", 1),
        FeatureDto(2L, "Test3", 2)
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
    fun `test group path provider filters out groups with the group filter id`() {
        //PREPARE
        val provider = GroupPathProvider(groups, groupFilterId = 4)

        //VERIFY
        assertEquals(
            listOf(0L, 1L, 2L, 3L),
            provider.filteredSortedGroups.map { it.group.id }
        )
    }

    @Test
    fun test_feature_path_provider() {
        //PREPARE
        val provider = FeaturePathProvider(features, groups)

        //EXECUTE
        val ans0 = provider.getPathForFeature(0)
        val ans1 = provider.getPathForFeature(1)
        val ans2 = provider.getPathForFeature(2)

        //VERIFY
        assertEquals("/Test", ans0)
        assertEquals("/group1/Test2", ans1)
        assertEquals("/group1/group1child1/Test3", ans2)
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
        assertEquals(
            listOf("/", "/Apple", "/Apple/Zebra", "/Banana", "/Carrot", "/Zebra", "/Zebra/Apple"),
            sortedGroups
        )
    }

    private fun group(name: String = "", id: Long = 0, parentId: Long? = 0): Group {
        return Group(id, name, 0, parentId, 0)
    }
}
