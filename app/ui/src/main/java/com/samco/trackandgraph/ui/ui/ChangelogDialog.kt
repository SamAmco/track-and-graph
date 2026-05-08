package com.samco.trackandgraph.ui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.R
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.theming.tngColors

data class ChangelogReleaseNote(
    val version: String,
    val markdown: String,
)

@Composable
fun ChangelogDialogContent(
    releaseNotes: List<ChangelogReleaseNote>,
    supportText: String,
    maybeLaterText: String,
    donateText: String,
    onDonateClicked: () -> Unit = {},
    onSkipDonationClicked: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) = CustomDialog(
    onDismissRequest = onDismissRequest,
    dismissOnClickOutside = false,
    scrollContent = false,
    dismissOnBackPress = false,
) {
    FadingScrollColumn(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing),
    ) {
        releaseNotes.forEach { releaseNote ->
            ReleaseNoteItem(
                version = releaseNote.version,
                markdown = releaseNote.markdown,
            )
        }

        DialogInputSpacing()

        HorizontalDivider()

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = supportText,
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        SelectorButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSkipDonationClicked,
            text = maybeLaterText,
        )

        FullWidthIconTextButton(
            modifier = Modifier.fillMaxWidth(),
            buttonColors = ButtonDefaults.buttonColors(
                containerColor = MaterialTheme.tngColors.primary,
            ),
            iconSize = 28.dp,
            onClick = onDonateClicked,
            icon = R.drawable.bmc_logo,
            textAlign = TextAlign.Center,
            buttonLocation = ButtonLocation.End,
            text = donateText,
        )
    }
}

@Composable
private fun ReleaseNoteItem(
    version: String,
    markdown: String,
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(
            text = version,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = dialogInputSpacing),
        )

        TnGMarkdown(
            content = markdown,
            modifier = Modifier.fillMaxWidth(),
        )
    }
}

@Preview
@Composable
private fun ChangelogDialogContentPreview() {
    TnGComposeTheme {
        ChangelogDialogContent(
            releaseNotes = listOf(
                ChangelogReleaseNote(
                    version = "v10.1.0",
                    markdown = """
                        # Better release notes

                        - Added a changelog preview app for checking markdown before release.
                        - Improved shared UI extraction so dialogs use the production theme.

                        ## Fixes

                        Markdown links, lists, and spacing should match the app dialog.
                    """.trimIndent(),
                ),
                ChangelogReleaseNote(
                    version = "v10.0.0",
                    markdown = """
                        # New foundations

                        This release moves common Compose UI into a shared module.
                    """.trimIndent(),
                ),
            ),
            supportText = "If you find Track & Graph useful, you can support future development.",
            maybeLaterText = "Maybe later",
            donateText = "Buy me a coffee",
        )
    }
}
