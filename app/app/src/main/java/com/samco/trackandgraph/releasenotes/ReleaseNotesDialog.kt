/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
package com.samco.trackandgraph.releasenotes

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.ui.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.ui.ChangelogDialogContent
import com.samco.trackandgraph.ui.ui.ChangelogReleaseNote
import com.samco.trackandgraph.ui.ui.resolve

@Composable
fun ReleaseNotesDialog(
    releaseNotes: List<ReleaseNoteViewData>,
    onDismissRequest: () -> Unit,
    onDonateClicked: () -> Unit = {},
    onSkipDonationClicked: () -> Unit = {}
) {
    ReleaseNotesDialogContent(
        releaseNotes = releaseNotes,
        onDonateClicked = onDonateClicked,
        onDismissRequest = onDismissRequest,
        onSkipDonationClicked = onSkipDonationClicked
    )
}

@Composable
private fun ReleaseNotesDialogContent(
    releaseNotes: List<ReleaseNoteViewData>,
    onDonateClicked: () -> Unit = {},
    onSkipDonationClicked: () -> Unit = {},
    onDismissRequest: () -> Unit = {},
) = ChangelogDialogContent(
    releaseNotes = releaseNotes.map {
        ChangelogReleaseNote(
            version = it.version,
            markdown = it.text.resolve() ?: "Failed to resolve release note text.. Sorry :/",
        )
    },
    onDismissRequest = onDismissRequest,
    dismissOnClickOutside = releaseNotesDismissOnClickOutside(),
    dismissOnBackPress = releaseNotesDismissOnBackPress(),
    supportContent = releaseNotesSupportContent(
        onDonateClicked = onDonateClicked,
        onSkipDonationClicked = onSkipDonationClicked,
        onDismissRequest = onDismissRequest,
    ),
)

@Preview(locale = "en")
@Composable
private fun ReleaseNotesDialogPreview() {
    TnGComposeTheme {
        ReleaseNotesDialog(
            releaseNotes = listOf(
                ReleaseNoteViewData(
                    version = "v1.2.0",
                    text = TranslatedString.Simple("## New Features\n- Added release notes dialog\n- Improved UI animations\n\n## Bug Fixes\n- Fixed crash on startup")
                ),
                ReleaseNoteViewData(
                    version = "v1.1.5",
                    text = TranslatedString.Simple("## Bug Fixes\n- Fixed data export issue\n- Improved performance")
                )
            ),
            onDismissRequest = {}
        )
    }
}

@Preview(locale = "en", showBackground = true)
@Composable
private fun ThankYouDialogContentPreview() {
    TnGComposeTheme {
        ReleaseNotesDialogContent(
            releaseNotes = emptyList(),
            onDismissRequest = {},
        )
    }
}
