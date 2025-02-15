package com.samco.trackandgraph.graphstatview.factories.helpers

import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILuaGraphViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITextViewData
import com.samco.trackandgraph.lua.dto.LuaGraphResultData
import com.samco.trackandgraph.lua.dto.TextAlignment
import com.samco.trackandgraph.lua.dto.TextSize
import javax.inject.Inject

class TextLuaHelper @Inject constructor() {
    operator fun invoke(
        textData: LuaGraphResultData.TextData,
        graphOrStat: GraphOrStat
    ): ILuaGraphViewData = object : ILuaGraphViewData {
        override val wrapped: IGraphStatViewData = object : ITextViewData {
            override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
            override val graphOrStat: GraphOrStat = graphOrStat
            override val text: String? = textData.text
            override val textSize: ITextViewData.TextSize = textData.size.toTextSize()
            override val textAlignment: ITextViewData.TextAlignment = textData.alignment.toTextAlignment()
        }
        override val state: IGraphStatViewData.State = IGraphStatViewData.State.READY
        override val graphOrStat: GraphOrStat = graphOrStat
    }

    private fun TextSize.toTextSize(): ITextViewData.TextSize = when (this) {
        TextSize.SMALL -> ITextViewData.TextSize.SMALL
        TextSize.MEDIUM -> ITextViewData.TextSize.MEDIUM
        TextSize.LARGE -> ITextViewData.TextSize.LARGE
    }

    private fun TextAlignment.toTextAlignment(): ITextViewData.TextAlignment = when (this) {
        TextAlignment.START -> ITextViewData.TextAlignment.START
        TextAlignment.CENTER -> ITextViewData.TextAlignment.CENTER
        TextAlignment.END -> ITextViewData.TextAlignment.END
    }
}

