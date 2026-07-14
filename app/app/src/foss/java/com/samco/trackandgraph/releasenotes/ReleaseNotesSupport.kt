package com.samco.trackandgraph.releasenotes

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.material3.ButtonDefaults
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
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import androidx.lifecycle.compose.LocalLifecycleOwner
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.theming.tngColors
import com.samco.trackandgraph.ui.ui.ButtonLocation
import com.samco.trackandgraph.ui.ui.CustomDialog
import com.samco.trackandgraph.ui.ui.FullWidthIconTextButton
import com.samco.trackandgraph.ui.ui.SelectorButton
import com.samco.trackandgraph.ui.ui.SmallTextButton
import com.samco.trackandgraph.ui.ui.dialogInputSpacing
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
            onClick = {
                wasDonationLaunched = true
                onDonateClicked()
            },
            icon = R.drawable.bmc_logo,
            textAlign = TextAlign.Center,
            buttonLocation = ButtonLocation.End,
            text = stringResource(R.string.release_notes_support_development),
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
