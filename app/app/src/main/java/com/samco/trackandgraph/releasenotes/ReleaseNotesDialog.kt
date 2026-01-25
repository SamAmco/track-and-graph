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

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.samco.trackandgraph.ui.compose.ui.TnGMarkdown
import com.samco.trackandgraph.R
import com.samco.trackandgraph.data.localisation.TranslatedString
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.ButtonLocation
import com.samco.trackandgraph.ui.compose.ui.CustomDialog
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.FadingScrollColumn
import com.samco.trackandgraph.ui.compose.ui.FullWidthIconTextButton
import com.samco.trackandgraph.ui.compose.ui.SelectorButton
import com.samco.trackandgraph.ui.compose.ui.SmallTextButton
import com.samco.trackandgraph.ui.compose.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.compose.ui.resolve

@Composable
fun ReleaseNotesDialog(
    releaseNotes: List<ReleaseNoteViewData>,
    onDismissRequest: () -> Unit,
    onDonateClicked: () -> Unit = {},
    onSkipDonationClicked: () -> Unit = {}
) {
    var wasDonationLaunched by remember { mutableStateOf(false) }
    var showThankYou by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(Unit) {
        lifecycleOwner.lifecycle.addObserver(object : DefaultLifecycleObserver {
            override fun onResume(owner: LifecycleOwner) {
                if (wasDonationLaunched) {
                    showThankYou = true
                    wasDonationLaunched = false
                }
            }
        })
    }

    if (showThankYou) {
        ThankYouDialogContent(onDismissRequest = onDismissRequest)
    } else {
        ReleaseNotesDialogContent(
            releaseNotes = releaseNotes,
            onDonateClicked = {
                wasDonationLaunched = true
                onDonateClicked()
            },
            onDismissRequest = onDismissRequest,
            onSkipDonationClicked = onSkipDonationClicked
        )
    }
}

@Composable
private fun ThankYouDialogContent(
    onDismissRequest: () -> Unit = {}
) = CustomDialog(
    onDismissRequest = onDismissRequest,
    paddingValues = PaddingValues(
        top = inputSpacingLarge,
        start = inputSpacingLarge,
        end = inputSpacingLarge,
        bottom = 0.dp,
    )
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing),
        horizontalAlignment = Alignment.End
    ) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.release_notes_thank_you),
            style = MaterialTheme.typography.titleSmall,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        SmallTextButton(
            onClick = onDismissRequest,
            stringRes = R.string.close,
        )
    }
}

@Composable
private fun ReleaseNotesDialogContent(
    releaseNotes: List<ReleaseNoteViewData>,
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
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing)
    ) {
        // Release notes content
        releaseNotes.forEach { releaseNote ->
            ReleaseNoteItem(
                version = releaseNote.version,
                text = releaseNote.text
            )
        }

        DialogInputSpacing()

        HorizontalDivider()

        Text(
            modifier = Modifier.fillMaxWidth(),
            text = stringResource(R.string.release_notes_support_text),
            style = MaterialTheme.typography.labelLarge,
            color = MaterialTheme.colorScheme.onSurface,
            textAlign = TextAlign.Center,
        )

        // Donation buttons
        SelectorButton(
            modifier = Modifier.fillMaxWidth(),
            onClick = onSkipDonationClicked,
            text = stringResource(R.string.release_notes_maybe_later),
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
            text = stringResource(R.string.release_notes_support_development)
        )
    }
}

@Composable
private fun ReleaseNoteItem(
    version: String,
    text: TranslatedString
) {
    Column(
        modifier = Modifier.fillMaxWidth()
    ) {
        // Version title
        Text(
            text = version,
            style = MaterialTheme.typography.headlineSmall,
            color = MaterialTheme.colorScheme.primary,
            modifier = Modifier.padding(bottom = dialogInputSpacing)
        )

        // Release note content using markdown viewer
        TnGMarkdown(
            content = text.resolve() ?: "Failed to resolve release note text.. Sorry :/",
            modifier = Modifier.fillMaxWidth()
        )
    }
}

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
        ThankYouDialogContent(
            onDismissRequest = {}
        )
    }
}
