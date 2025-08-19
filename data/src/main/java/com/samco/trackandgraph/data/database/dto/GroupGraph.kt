package com.samco.trackandgraph.data.database.dto

data class GroupGraph(
    val group: Group,
    val children: List<GroupGraphItem>,
)

sealed class GroupGraphItem {
    interface FeatureNode

    data class GroupNode(val groupGraph: GroupGraph) : GroupGraphItem()
    data class GraphNode(val graph: GraphOrStat) : GroupGraphItem()
    data class TrackerNode(val tracker: Tracker) : GroupGraphItem(), FeatureNode
}
