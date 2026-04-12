package com.samco.trackandgraph.data.database.dto

data class GroupGraph(
    val group: Group,
    val children: List<GroupGraphItem>,
)


sealed class GroupGraphItem {
    abstract val groupItemId: Long

    interface FeatureNode

    data class GroupNode(override val groupItemId: Long, val groupGraph: GroupGraph) : GroupGraphItem()
    data class GraphNode(override val groupItemId: Long, val graph: GraphOrStat) : GroupGraphItem()
    data class TrackerNode(override val groupItemId: Long, val tracker: Tracker) : GroupGraphItem(), FeatureNode
    data class FunctionNode(override val groupItemId: Long, val function: Function) : GroupGraphItem(), FeatureNode
}
