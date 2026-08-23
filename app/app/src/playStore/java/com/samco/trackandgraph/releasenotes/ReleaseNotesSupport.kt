package com.samco.trackandgraph.releasenotes

import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.support.SupportDeveloperScreen
import com.samco.trackandgraph.ui.theming.tngColors
import com.samco.trackandgraph.ui.ui.ButtonLocation
import com.samco.trackandgraph.ui.ui.FullWidthIconTextButton
import com.samco.trackandgraph.ui.ui.SelectorButton

internal fun shouldShowReleaseNotesButton(
    hasNotes: Boolean,
    hasSupportLink: Boolean,
) = hasNotes

internal fun releaseNotesDismissOnClickOutside() = true

internal fun releaseNotesDismissOnBackPress() = true

@Composable
internal fun releaseNotesSupportContent(
    onDonateClicked: () -> Unit,
    onSkipDonationClicked: () -> Unit,
    onDismissRequest: () -> Unit,
    onSupportClicked: () -> Unit,
): (@Composable () -> Unit)? = {
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(R.string.release_notes_support_text),
        style = MaterialTheme.typography.labelLarge,
        color = MaterialTheme.colorScheme.onSurface,
        textAlign = TextAlign.Center,
    )

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
        onClick = onSupportClicked,
        icon = R.drawable.support_developer_icon,
        textAlign = TextAlign.Center,
        buttonLocation = ButtonLocation.End,
        text = stringResource(R.string.release_notes_support_development),
    )
}

@Composable
internal fun releaseNotesSupportScreen(
    onBack: () -> Unit,
): (@Composable () -> Unit)? = {
    SupportDeveloperScreen(onBack = onBack)
}
