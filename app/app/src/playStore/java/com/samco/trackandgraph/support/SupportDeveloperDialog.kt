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
import com.samco.trackandgraph.ui.ui.inputSpacingLarge
import com.samco.trackandgraph.ui.ui.inputSpacingXLarge

@Composable
internal fun SupportDeveloperDialog(
    onDismissRequest: () -> Unit,
    viewModel: SupportBillingViewModel = hiltViewModel(),
) {
    CustomDialog(
        onDismissRequest = {
            viewModel.clearTransientState()
            onDismissRequest()
        },
        scrollContent = false,
        supportSmoothHeightAnimation = true,
        paddingValues = PaddingValues(inputSpacingLarge),
    ) {
        SupportDeveloperScreen(
            onBack = {
                viewModel.clearTransientState()
                onDismissRequest()
            },
            viewModel = viewModel,
        )
    }
}

/** Content-only form used inside the animated release-notes dialog flow. */
@Composable
internal fun SupportDeveloperScreen(
    onBack: () -> Unit,
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
            if (activity != null) viewModel.purchase(activity, optionId)
        },
        onBack = {
            viewModel.clearTransientState()
            onBack()
        },
    )
}

@Composable
internal fun SupportDeveloperDialogContent(
    state: SupportBillingState,
    onOptionClicked: (String) -> Unit = {},
    onBack: () -> Unit = {},
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing),
        horizontalAlignment = Alignment.CenterHorizontally,
    ) {
        if (state.showThankYou) {
            SupportThankYouContent(
                message = stringResource(UiR.string.release_notes_thank_you),
                closeText = stringResource(UiR.string.support_close),
                onClose = onBack,
            )
        } else {
            when (val products = state.products) {
                SupportProductsState.NotLoaded,
                SupportProductsState.Loading -> LoadingContent()

                SupportProductsState.Unavailable -> {
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
                }

                is SupportProductsState.Available -> AvailableProductsContent(
                    products = products,
                    purchaseInProgress = state.purchaseInProgress,
                    onOptionClicked = onOptionClicked,
                )
            }

            state.message?.let { message ->
                Text(
                    modifier = Modifier.fillMaxWidth(),
                    text = stringResource(
                        when (message) {
                            SupportBillingMessage.PaymentFailed -> R.string.support_payment_failed
                            SupportBillingMessage.PaymentPending -> R.string.support_payment_pending
                        }
                    ),
                    style = MaterialTheme.typography.bodyMedium,
                    color = if (message == SupportBillingMessage.PaymentFailed) {
                        MaterialTheme.colorScheme.error
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    },
                    textAlign = TextAlign.Center,
                )
            }
            ContinueCancelButtons(
                cancelVisible = true,
                continueVisible = false,
                cancelText = R.string.cancel,
                onCancel = onBack,
            )
        }
    }
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
    products: SupportProductsState.Available,
    purchaseInProgress: Boolean,
    onOptionClicked: (String) -> Unit,
) {
    SupportOptionsContent(
        description = products.description,
        options = products.options.map {
            SupportOptionViewData(
                id = it.id,
                formattedPrice = it.formattedPrice,
                highlighted = it.highlighted,
            )
        },
        purchaseInProgress = purchaseInProgress,
        onOptionClicked = onOptionClicked,
    )
}

private tailrec fun Context.findActivity(): Activity? = when (this) {
    is Activity -> this
    is ContextWrapper -> baseContext.findActivity()
    else -> null
}
