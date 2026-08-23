package com.samco.trackandgraph.ui.ui

import androidx.annotation.DrawableRes
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.R
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.theming.tngColors

/** Shared release-notes footer used by both app variants and the changelog viewer. */
@Composable
fun ReleaseNotesSupportPrompt(
    supportText: String,
    maybeLaterText: String,
    supportActions: List<ReleaseNotesSupportAction>,
    onMaybeLaterClicked: () -> Unit = {},
) {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = supportText,
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

    SelectorButton(
        modifier = Modifier.fillMaxWidth(),
        onClick = onMaybeLaterClicked,
        text = maybeLaterText,
    )

    supportActions.forEach { action ->
        FullWidthIconTextButton(
            modifier = Modifier.fillMaxWidth(),
            buttonColors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.tngColors.primary,
            ),
            iconSize = 28.dp,
            onClick = action.onClick,
            icon = action.icon,
            textAlign = TextAlign.Center,
            buttonLocation = ButtonLocation.End,
            text = action.text,
        )
    }
}

data class ReleaseNotesSupportAction(
    val text: String,
    @DrawableRes val icon: Int,
    val onClick: () -> Unit = {},
)

@Preview(showBackground = true)
@Composable
private fun ReleaseNotesSupportPromptPreview() {
    TnGComposeTheme {
        Column(verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)) {
            ReleaseNotesSupportPrompt(
                supportText = stringResource(R.string.release_notes_support_text),
                maybeLaterText = stringResource(R.string.release_notes_maybe_later),
                supportActions = listOf(
                    ReleaseNotesSupportAction(
                        text = stringResource(R.string.release_notes_support_development),
                        icon = R.drawable.support_developer_icon,
                    )
                ),
            )
        }
    }
}
