package com.samco.trackandgraph.group

import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupChildType

sealed class GroupChild {
    abstract val id: Long
    abstract val type: GroupChildType

    class ChildGroup(
        override val id: Long,
        val group: Group,
        override val type: GroupChildType = GroupChildType.GROUP
    ) : GroupChild()

    class ChildTracker(
        override val id: Long,
        val displayTracker: DisplayTracker,
        override val type: GroupChildType = GroupChildType.TRACKER
    ) : GroupChild()

    class ChildGraph(
        override val id: Long,
        val graph: CalculatedGraphViewData,
        override val type: GroupChildType = GroupChildType.GRAPH
    ) : GroupChild()

    class ChildFunction(
        override val id: Long,
        val displayFunction: DisplayFunction,
        override val type: GroupChildType = GroupChildType.FUNCTION
    ) : GroupChild()
}
