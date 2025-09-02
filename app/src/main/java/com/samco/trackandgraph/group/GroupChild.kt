package com.samco.trackandgraph.group

import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupChildType

sealed class GroupChild {
    fun toDto() = com.samco.trackandgraph.data.database.dto.GroupChild(
        type = when (this) {
            is ChildGroup -> GroupChildType.GROUP
            is ChildTracker -> GroupChildType.TRACKER
            is ChildGraph -> GroupChildType.GRAPH
            is ChildFunction -> GroupChildType.FUNCTION
        },
        id = id,
        displayIndex = displayIndex
    )

    abstract val id: Long
    abstract val displayIndex: Int
    abstract val type: GroupChildType

    class ChildGroup(
        override val id: Long,
        override val displayIndex: Int,
        val group: Group,
        override val type: GroupChildType = GroupChildType.GROUP
    ) : GroupChild()

    class ChildTracker(
        override val id: Long,
        override val displayIndex: Int,
        val displayTracker: DisplayTracker,
        override val type: GroupChildType = GroupChildType.TRACKER
    ) : GroupChild()

    class ChildGraph(
        override val id: Long,
        override val displayIndex: Int,
        val graph: CalculatedGraphViewData,
        override val type: GroupChildType = GroupChildType.GRAPH
    ) : GroupChild()

    class ChildFunction(
        override val id: Long,
        override val displayIndex: Int,
        val displayFunction: DisplayFunction,
        override val type: GroupChildType = GroupChildType.FUNCTION
    ) : GroupChild()
}