package com.samco.trackandgraph.releasenotes

import androidx.compose.runtime.Composable
import androidx.compose.ui.res.stringResource
import com.samco.trackandgraph.support.SupportDeveloperScreen
import com.samco.trackandgraph.ui.R as UiR
import com.samco.trackandgraph.ui.ui.ReleaseNotesSupportAction
import com.samco.trackandgraph.ui.ui.ReleaseNotesSupportPrompt

internal fun shouldShowReleaseNotesButton(
    hasNotes: Boolean,
    hasSupportLink: Boolean,
) = hasNotes

internal fun releaseNotesDismissOnClickOutside() = false

internal fun releaseNotesDismissOnBackPress() = true

@Composable
internal fun releaseNotesSupportContent(
    onDonateClicked: () -> Unit,
    onSkipDonationClicked: () -> Unit,
    onDismissRequest: () -> Unit,
    onSupportClicked: () -> Unit,
): (@Composable () -> Unit)? = {
    ReleaseNotesSupportPrompt(
        supportText = stringResource(UiR.string.release_notes_support_text),
        maybeLaterText = stringResource(UiR.string.release_notes_maybe_later),
        supportActions = listOf(
            ReleaseNotesSupportAction(
                text = stringResource(UiR.string.release_notes_support_development),
                icon = UiR.drawable.support_developer_icon,
                onClick = onSupportClicked,
            )
        ),
        onMaybeLaterClicked = onSkipDonationClicked,
    )
}

@Composable
internal fun releaseNotesSupportScreen(
    onBack: () -> Unit,
    onThankYouClose: () -> Unit,
): (@Composable () -> Unit)? = {
    SupportDeveloperScreen(
        onBack = onBack,
        onThankYouClose = onThankYouClose,
    )
}
