/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import androidx.compose.runtime.Composable
import androidx.compose.ui.tooling.preview.Preview
import com.samco.trackandgraph.ui.theming.TnGComposeTheme

@Preview(showBackground = true)
@Composable
private fun SupportDeveloperDialogAvailablePreview() {
    TnGComposeTheme {
        SupportDeveloperDialogContent(
            state = SupportBillingState.Available(
                description = "Support is voluntary and does not unlock any features.",
                options = listOf(
                    SupportPurchaseOption("small", "£1.99", 1_990_000, false),
                    SupportPurchaseOption("medium", "£4.99", 4_990_000, true),
                    SupportPurchaseOption("large", "£9.99", 9_990_000, false),
                ),
            )
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SupportDeveloperDialogLoadingPreview() {
    TnGComposeTheme {
        SupportDeveloperDialogContent(
            state = SupportBillingState.Loading
        )
    }
}

@Preview(showBackground = true)
@Composable
private fun SupportDeveloperDialogErrorPreview() {
    TnGComposeTheme {
        SupportDeveloperDialogContent(
            state = SupportBillingState.Available(
                description = "Support is voluntary and does not unlock any features.",
                options = listOf(
                    SupportPurchaseOption("medium", "£4.99", 4_990_000, true),
                ),
                checkoutState = SupportCheckoutState.PaymentFailed,
            )
        )
    }
}
