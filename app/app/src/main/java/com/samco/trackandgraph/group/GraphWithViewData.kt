package com.samco.trackandgraph.group

import com.samco.trackandgraph.data.database.dto.GraphOrStat

class GraphWithViewData(
    val graph: GraphOrStat,
    val viewData: CalculatedGraphViewData
)