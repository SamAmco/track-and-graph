/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */
@file:OptIn(ExperimentalMaterialApi::class)

package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.material.ExperimentalMaterialApi
import androidx.compose.material.ExposedDropdownMenuDefaults
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
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
                    fontSize = MaterialTheme.typography.subtitle2.fontSize,
                    fontWeight = MaterialTheme.typography.subtitle2.fontWeight,
                )
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        },
        dropdownItemFactory = { item, _ ->
            Text(
                text = strings[item] ?: "",
                fontSize = MaterialTheme.typography.subtitle2.fontSize,
                fontWeight = MaterialTheme.typography.subtitle2.fontWeight
            )
        }
    )
}

