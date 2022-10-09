package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.res.dimensionResource
import com.samco.trackandgraph.R

@Composable
fun <T> TextMapSpinner(
    strings: Map<T, String>,
    selectedItem: T,
    onItemSelected: (T) -> Unit
) {
    Spinner(
        items = strings.keys.toList(),
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        selectedItemFactory = { modifier, item, expanded ->
            Row(
                modifier = modifier
                    .padding(dimensionResource(id = R.dimen.card_padding)),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = strings[item] ?: "",
                    fontSize = MaterialTheme.typography.labelSmall.fontSize,
                    fontWeight = MaterialTheme.typography.labelSmall.fontWeight,
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        },
        dropdownItemFactory = { item, _ ->
            Text(
                text = strings[item] ?: "",
                fontSize = MaterialTheme.typography.labelSmall.fontSize,
                fontWeight = MaterialTheme.typography.labelSmall.fontWeight
            )
        }
    )
}

