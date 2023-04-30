package com.samco.trackandgraph.graphstatview.factories.viewdto

import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.base.database.dto.IDataPoint

interface ILastValueData : IGraphStatViewData {
    val lastDataPoint: IDataPoint?
        get() = null

    companion object {
        fun loading(graphOrStat: GraphOrStat) = object : ILastValueData {
            override val state: IGraphStatViewData.State
                get() = IGraphStatViewData.State.LOADING
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }
}