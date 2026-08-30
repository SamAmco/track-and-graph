/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.ui.ui

import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.ColumnScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp

data class SupportOptionViewData(
    val id: String,
    val formattedPrice: String,
    val highlighted: Boolean,
)

/** Shared visual content for the real Play support screen and the changelog viewer mock. */
@Composable
fun ColumnScope.SupportOptionsContent(
    description: String,
    options: List<SupportOptionViewData>,
    purchaseInProgress: Boolean,
    optionsEnabled: Boolean = !purchaseInProgress,
    onOptionClicked: (String) -> Unit,
) {
    Column(
        modifier = Modifier
            .weight(1f, fill = false)
            .fillMaxWidth()
            .verticalScroll(rememberScrollState())
            .padding(dialogInputSpacing),
        verticalArrangement = Arrangement.spacedBy(dialogInputSpacing),
    ) {
        options.forEach { option ->
            SelectorButton(
                modifier = Modifier.fillMaxWidth(),
                text = option.formattedPrice,
                border = if (option.highlighted) {
                    BorderStroke(2.dp, MaterialTheme.colorScheme.primary)
                } else null,
                enabled = optionsEnabled,
                onClick = { onOptionClicked(option.id) },
            )
        }
    }

    if (description.isNotBlank()) {
        Text(
            modifier = Modifier.fillMaxWidth(),
            text = description,
            style = MaterialTheme.typography.bodyMedium,
            textAlign = TextAlign.Center,
        )
    }

    if (purchaseInProgress) {
        CircularProgressIndicator()
    }
}
