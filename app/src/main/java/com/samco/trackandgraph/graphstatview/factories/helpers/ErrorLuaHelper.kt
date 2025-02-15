package com.samco.trackandgraph.graphstatview.factories.helpers

import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import javax.inject.Inject

class ErrorLuaHelper @Inject constructor() {
    operator fun invoke(
        graphOrStat: GraphOrStat,
        throwable: Throwable,
    ): ILuaGraphViewData =
        object : ILuaGraphViewData {
            override val wrapped: IGraphStatViewData? = null
            override val state = IGraphStatViewData.State.ERROR
            override val graphOrStat = graphOrStat
            override val error = throwable
        }
}