package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITextViewData
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge

@Composable
fun LuaTextView(
    modifier: Modifier = Modifier,
    textData: ITextViewData
) = Column(modifier = modifier.fillMaxWidth()) {
    Text(
        modifier = modifier
            .fillMaxWidth()
            .padding(inputSpacingLarge),
        text = textData.text ?: "",
        style = when (textData.textSize) {
            ITextViewData.TextSize.SMALL -> MaterialTheme.typography.titleSmall
            ITextViewData.TextSize.MEDIUM -> MaterialTheme.typography.displaySmall
            ITextViewData.TextSize.LARGE -> MaterialTheme.typography.displayLarge
        },
        textAlign = when (textData.textAlignment) {
            ITextViewData.TextAlignment.START -> TextAlign.Start
            ITextViewData.TextAlignment.CENTER -> TextAlign.Center
            ITextViewData.TextAlignment.END -> TextAlign.End
        }
    )
}

@Preview(showBackground = true)
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