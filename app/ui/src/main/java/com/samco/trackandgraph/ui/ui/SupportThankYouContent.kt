package com.samco.trackandgraph.ui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.ui.R
import com.samco.trackandgraph.ui.theming.TnGComposeTheme

/** The common post-support content used by FOSS and Play Store payment flows. */
@Composable
fun SupportThankYouContent(
    message: String,
    closeText: String,
    onClose: () -> Unit = {},
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing),
        horizontalAlignment = Alignment.End,
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = message,
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        SmallTextButton(
            text = closeText,
            onClick = onClose,
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SupportThankYouContentPreview() {
    TnGComposeTheme {
        SupportThankYouContent(
            message = stringResource(R.string.release_notes_thank_you),
            closeText = stringResource(R.string.support_close),
        )
    }
}
