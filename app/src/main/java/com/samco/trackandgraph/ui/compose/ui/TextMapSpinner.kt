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

package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier

@Composable
fun <T> TextMapSpinner(
    modifier: Modifier = Modifier,
    strings: Map<T, String>,
    selectedItem: T,
    onItemSelected: (T) -> Unit
) {
    Spinner(
        modifier = modifier,
        items = strings.keys.toList(),
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        selectedItemFactory = { modifier, item, expanded ->
            Text(
                modifier = modifier.weight(1f),
                text = strings[item] ?: "",
                fontSize = MaterialTheme.typography.body1.fontSize,
                fontWeight = MaterialTheme.typography.body1.fontWeight,
            )
        },
        dropdownItemFactory = { item, _ ->
            Text(
                text = strings[item] ?: "",
                fontSize = MaterialTheme.typography.body1.fontSize,
                fontWeight = MaterialTheme.typography.body1.fontWeight
            )
        }
    )
}

