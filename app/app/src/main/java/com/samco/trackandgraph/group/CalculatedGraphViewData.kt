package com.samco.trackandgraph.group

import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData

data class CalculatedGraphViewData(
    val time: Long,
    val viewData: IGraphStatViewData,
    val unique: Boolean = true,
) {
    fun isLoading() = viewData.state == IGraphStatViewData.State.LOADING
    fun isReady() = viewData.state == IGraphStatViewData.State.READY
}