package com.samco.trackandgraph.graphstatview.factories.viewdto

import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData.State

interface ITextViewData : IGraphStatViewData {

    enum class TextSize {
        SMALL, MEDIUM, LARGE
    }

    val text: String?
        get() = null

    val textSize: TextSize
        get() = TextSize.MEDIUM

    companion object {
        fun loading(graphOrStat: GraphOrStat) = object : ITextViewData {
            override val state: State
                get() = State.LOADING
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }

        fun default(graphOrStat: GraphOrStat) = object : ITextViewData {
            override val state: State
                get() = State.READY
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }

}