package com.samco.trackandgraph.releasenotes

import androidx.compose.runtime.Composable

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
): (@Composable () -> Unit)? = null
