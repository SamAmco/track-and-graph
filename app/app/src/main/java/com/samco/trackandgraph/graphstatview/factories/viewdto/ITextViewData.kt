package com.samco.trackandgraph.graphstatview.factories.viewdto

import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData.State

interface ITextViewData : IGraphStatViewData {

    enum class TextSize {
        SMALL, MEDIUM, LARGE
    }

    enum class TextAlignment {
        START, CENTER, END
    }

    val text: String?
        get() = null

    val textSize: TextSize
        get() = TextSize.MEDIUM

    val textAlignment: TextAlignment
        get() = TextAlignment.CENTER

    companion object {
        fun loading(graphOrStat: GraphOrStat) = object : ITextViewData {
            override val state: State
                get() = State.LOADING
            override val graphOrStat: GraphOrStat
                get() = graphOrStat
        }
    }
}