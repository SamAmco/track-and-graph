/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import android.app.Activity
import android.content.Context
import android.content.ContextWrapper
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.hilt.lifecycle.viewmodel.compose.hiltViewModel
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.R as UiR
import com.samco.trackandgraph.ui.ui.ContinueCancelButtons
import com.samco.trackandgraph.ui.ui.CustomDialog
import com.samco.trackandgraph.ui.ui.SupportOptionViewData
import com.samco.trackandgraph.ui.ui.SupportOptionsContent
import com.samco.trackandgraph.ui.ui.SupportThankYouContent
import com.samco.trackandgraph.ui.ui.dialogInputSpacing
import com.samco.trackandgraph.ui.ui.halfDialogInputSpacing
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.ui.inputSpacingXLarge

@Composable
internal fun SupportDeveloperDialog(
    onDismissRequest: () -> Unit,
    viewModel: SupportBillingViewModel = hiltViewModel(),
) {
    CustomDialog(
        onDismissRequest = {
            viewModel.dismiss()
            onDismissRequest()
        },
        scrollContent = false,
        supportSmoothHeightAnimation = true,
        paddingValues = PaddingValues(
            start = inputSpacingLarge,
            end = inputSpacingLarge,
            top = inputSpacingLarge,
            bottom = halfDialogInputSpacing,
        ),
    ) {
        SupportDeveloperScreen(
            onBack = onDismissRequest,
            onThankYouClose = onDismissRequest,
            viewModel = viewModel,
        )
    }
}

/** Content-only form used inside the animated release-notes dialog flow. */
@Composable
internal fun SupportDeveloperScreen(
    onBack: () -> Unit,
    onThankYouClose: () -> Unit,
    viewModel: SupportBillingViewModel = hiltViewModel(),
) {
    val state = viewModel.state.collectAsStateWithLifecycle().value
    val activity = LocalContext.current.findActivity()

    LaunchedEffect(viewModel) {
        viewModel.load()
    }

    SupportDeveloperDialogContent(
        state = state,
        onOptionClicked = { optionId ->
            if (activity != null) {
                viewModel.purchase(activity.asSupportBillingFlowHost(), optionId)
            }
        },
        onBack = {
            viewModel.dismiss()
            onBack()
        },
        onThankYouClose = {
            viewModel.dismiss()
            onThankYouClose()
        },
    )
}

@Composable
internal fun SupportDeveloperDialogContent(
    state: SupportBillingState,
    onOptionClicked: (String) -> Unit = {},
    onBack: () -> Unit = {},
    onThankYouClose: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        when (state) {
            SupportBillingState.Loading -> {
                LoadingContent()
                CancelButton(onBack)
            }

            is SupportBillingState.Unavailable -> {
                Column(
                    modifier = Modifier.fillMaxWidth(),
                    verticalArrangement = Arrangement.spacedBy(dialogInputSpacing),
                    horizontalAlignment = Alignment.CenterHorizontally,
                ) {
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.support_developer),
                        style = MaterialTheme.typography.headlineSmall,
                        textAlign = TextAlign.Center,
                    )
                    Text(
                        modifier = Modifier.fillMaxWidth(),
                        text = stringResource(R.string.support_unavailable),
                        style = MaterialTheme.typography.bodyMedium,
                        textAlign = TextAlign.Center,
                    )
                    if (state.hasPendingPurchase) {
                        BillingMessage(SupportCheckoutState.PaymentPending)
                    }
                }
                CancelButton(onBack)
            }

            is SupportBillingState.Available -> {
                AvailableProductsContent(
                    state = state,
                    onOptionClicked = onOptionClicked,
                )
                BillingMessage(state.checkoutState)
                CancelButton(onBack)
            }

            SupportBillingState.ThankYou -> SupportThankYouContent(
                message = stringResource(UiR.string.release_notes_thank_you),
                closeText = stringResource(UiR.string.support_close),
                onClose = onThankYouClose,
            )
        }
    }
}

@Composable
private fun BillingMessage(checkoutState: SupportCheckoutState) {
    val message = when (checkoutState) {
        SupportCheckoutState.PaymentFailed -> R.string.support_payment_failed
        SupportCheckoutState.PaymentPending -> R.string.support_payment_pending
        SupportCheckoutState.Idle,
        SupportCheckoutState.InProgress -> return
    }
    Text(
        modifier = Modifier.fillMaxWidth(),
        text = stringResource(message),
        style = MaterialTheme.typography.bodyMedium,
        color = if (checkoutState == SupportCheckoutState.PaymentFailed) {
            MaterialTheme.colorScheme.error
        } else {
            MaterialTheme.colorScheme.onSurfaceVariant
        },
        textAlign = TextAlign.Center,
    )
}

@Composable
private fun CancelButton(onBack: () -> Unit) {
    ContinueCancelButtons(
        cancelVisible = true,
        continueVisible = false,
        cancelText = R.string.cancel,
        onCancel = onBack,
    )
}

@Composable
private fun LoadingContent() {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = inputSpacingXLarge),
        contentAlignment = Alignment.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun ColumnScope.AvailableProductsContent(
    state: SupportBillingState.Available,
    onOptionClicked: (String) -> Unit,
) {
    SupportOptionsContent(
        description = state.description,
        options = state.options.map {
            SupportOptionViewData(
                id = it.id,
                formattedPrice = it.formattedPrice,
                highlighted = it.highlighted,
            )
        },
        purchaseInProgress = state.checkoutState == SupportCheckoutState.InProgress,
        optionsEnabled = state.checkoutState != SupportCheckoutState.InProgress &&
            state.checkoutState != SupportCheckoutState.PaymentPending,
        onOptionClicked = onOptionClicked,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
