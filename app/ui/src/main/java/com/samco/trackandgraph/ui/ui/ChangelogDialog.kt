package com.samco.trackandgraph.ui.ui

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.ui.theming.TnGComposeTheme

data class ChangelogReleaseNote(
    val version: String,
    val markdown: String,
)

@Composable
fun ChangelogDialogContent(
    releaseNotes: List<ChangelogReleaseNote>,
    onDismissRequest: () -> Unit = {},
    dismissOnClickOutside: Boolean = false,
    dismissOnBackPress: Boolean = false,
    supportContent: (@Composable () -> Unit)? = null,
) = CustomDialog(
    onDismissRequest = onDismissRequest,
    dismissOnClickOutside = dismissOnClickOutside,
    dismissOnBackPress = dismissOnBackPress,
    scrollContent = false,
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

        supportContent?.let {
            HorizontalDivider()
            it()
        }
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
        )
    }
}
