package com.samco.trackandgraph.releasenotes

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samco.trackandgraph.ui.R as UiR
import com.samco.trackandgraph.ui.ui.CustomDialog
import com.samco.trackandgraph.ui.ui.ReleaseNotesSupportAction
import com.samco.trackandgraph.ui.ui.ReleaseNotesSupportPrompt
import com.samco.trackandgraph.ui.ui.SupportThankYouContent
import com.samco.trackandgraph.ui.ui.inputSpacingLarge

internal fun shouldShowReleaseNotesButton(
    hasNotes: Boolean,
    hasSupportLink: Boolean,
) = hasNotes && hasSupportLink

internal fun releaseNotesDismissOnClickOutside() = false

internal fun releaseNotesDismissOnBackPress() = false

@Composable
internal fun releaseNotesSupportContent(
    onDonateClicked: () -> Unit,
    onSkipDonationClicked: () -> Unit,
    onDismissRequest: () -> Unit,
    onSupportClicked: () -> Unit,
): (@Composable () -> Unit)? {
    var wasDonationLaunched by remember { mutableStateOf(false) }
    var showThankYou by remember { mutableStateOf(false) }

    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(lifecycleOwner) {
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
    }

    return {
        ReleaseNotesSupportPromptContent(
            onSkipDonationClicked = onSkipDonationClicked,
            onDonateClicked = {
                wasDonationLaunched = true
                onDonateClicked()
            },
        )
    }
}

@Composable
internal fun ReleaseNotesSupportPromptContent(
    onDonateClicked: () -> Unit = {},
    onSkipDonationClicked: () -> Unit = {},
) {
    ReleaseNotesSupportPrompt(
        supportText = stringResource(UiR.string.release_notes_support_text),
        maybeLaterText = stringResource(UiR.string.release_notes_maybe_later),
        supportActions = listOf(
            ReleaseNotesSupportAction(
                text = stringResource(UiR.string.release_notes_support_development),
                icon = UiR.drawable.bmc_logo,
                onClick = onDonateClicked,
            )
        ),
        onMaybeLaterClicked = onSkipDonationClicked,
    )
}

@Composable
internal fun releaseNotesSupportScreen(
    onBack: () -> Unit,
    onThankYouClose: () -> Unit,
): (@Composable () -> Unit)? = null

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
    SupportThankYouContent(
        message = stringResource(UiR.string.release_notes_thank_you),
        closeText = stringResource(UiR.string.support_close),
        onClose = onDismissRequest,
    )
}
