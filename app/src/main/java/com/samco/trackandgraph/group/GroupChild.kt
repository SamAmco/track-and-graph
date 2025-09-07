package com.samco.trackandgraph.group

import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.Group
import com.samco.trackandgraph.data.database.dto.GroupChildType

sealed class GroupChild {
    fun toDto() = com.samco.trackandgraph.data.database.dto.GroupChildOrderData(
        type = when (this) {
            is ChildGroup -> GroupChildType.GROUP
            is ChildTracker -> GroupChildType.FEATURE
            is ChildGraph -> GroupChildType.GRAPH
            is ChildFunction -> GroupChildType.FEATURE
        },
        id = idForGroupOrdering,
        displayIndex = displayIndex
    )

    abstract val displayIndex: Int
    abstract val type: GroupChildType
    abstract val idForGroupOrdering: Long

    class ChildGroup(
        val id: Long,
        override val displayIndex: Int,
        val group: Group,
        override val type: GroupChildType = GroupChildType.GROUP
    ) : GroupChild() {
        override val idForGroupOrdering: Long = id
    }

    class ChildTracker(
        val id: Long,
        override val displayIndex: Int,
        val displayTracker: DisplayTracker,
        override val type: GroupChildType = GroupChildType.FEATURE
    ) : GroupChild() {
        override val idForGroupOrdering: Long = displayTracker.featureId
    }

    class ChildGraph(
        val id: Long,
        override val displayIndex: Int,
        val graph: CalculatedGraphViewData,
        override val type: GroupChildType = GroupChildType.GRAPH
    ) : GroupChild() {
        override val idForGroupOrdering: Long = id
    }

    class ChildFunction(
        val id: Long,
        override val displayIndex: Int,
        val displayFunction: DisplayFunction,
        override val type: GroupChildType = GroupChildType.FEATURE
    ) : GroupChild() {
        override val idForGroupOrdering: Long = displayFunction.featureId
    }
}