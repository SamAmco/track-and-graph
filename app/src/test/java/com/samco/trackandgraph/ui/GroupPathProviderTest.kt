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
import junit.framework.Assert.assertEquals
import org.junit.Test

class GroupPathProviderTest {

    @Test
    fun test_group_path_provider() {
        //PREPARE
        val groups = listOf(
            group(parentId = null),
            group("group1", 1),
            group("group1child1", 2, 1),
            group("group1child2", 3, 1),
            group("group2", 4),
            group("group2child", 5, 4),
            group("group2childChild", 6, 5),
        )
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

    private fun group(name: String = "", id: Long = 0, parentId: Long? = 0): Group {
        return Group(id, name, 0, parentId, 0)
    }
}