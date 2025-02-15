package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.padding
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.GraphOrStat
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITextViewData

@Composable
fun LuaTextView(
    modifier: Modifier = Modifier,
    textData: ITextViewData
) = Column(modifier = modifier) {
    Text(
        modifier = modifier
            .padding(vertical = dimensionResource(id = R.dimen.input_spacing_large)),
        text = textData.text ?: "",
        style = when (textData.textSize) {
            ITextViewData.TextSize.SMALL -> MaterialTheme.typography.body1
            ITextViewData.TextSize.MEDIUM -> MaterialTheme.typography.h4
            ITextViewData.TextSize.LARGE -> MaterialTheme.typography.h2
        },
        textAlign = TextAlign.Center
    )
}

@Preview
@Composable
fun LuaTextViewPreview() {
    LuaTextView(
        textData = object : ITextViewData {
            override val text = "Hello, World!"
            override val textSize = ITextViewData.TextSize.SMALL
            override val state: IGraphStatViewData.State get() = throw NotImplementedError()
            override val graphOrStat: GraphOrStat get() = throw NotImplementedError()
        }
    )
}