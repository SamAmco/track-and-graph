package com.samco.trackandgraph.releasenotes

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.ui.ChangelogDialogBody
import com.samco.trackandgraph.ui.ui.ChangelogReleaseNote
import com.samco.trackandgraph.ui.ui.inputSpacingLarge

@Preview(showBackground = true, widthDp = 360, heightDp = 640)
@Composable
private fun ReleaseNotesSupportPreview() {
    TnGComposeTheme {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(MaterialTheme.colorScheme.background),
            contentAlignment = Alignment.Center,
        ) {
            Surface(
                modifier = Modifier.padding(16.dp),
                shape = MaterialTheme.shapes.large,
                color = MaterialTheme.colorScheme.surface,
            ) {
                Column(modifier = Modifier.padding(inputSpacingLarge)) {
                    ChangelogDialogBody(
                        releaseNotes = listOf(
                            ChangelogReleaseNote(
                                version = "v10.5.0",
                                markdown = """
                                    ## What's new

                                    - A useful new feature
                                    - A couple of important fixes
                                """.trimIndent(),
                            )
                        ),
                        supportContent = {
                            ReleaseNotesSupportPromptContent()
                        },
                    )
                }
            }
        }
    }
}
