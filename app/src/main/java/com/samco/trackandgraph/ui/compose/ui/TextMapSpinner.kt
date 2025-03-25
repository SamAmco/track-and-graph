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

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.text.style.TextAlign
import com.samco.trackandgraph.R

@Composable
fun <T> TextMapSpinner(
    modifier: Modifier = Modifier,
    strings: Map<T, String>,
    selectedItem: T,
    enabled: Boolean = true,
    textAlign: TextAlign = TextAlign.Start,
    paddingValues: PaddingValues = PaddingValues(
        horizontal = dimensionResource(id = R.dimen.card_padding),
    ),
    onItemSelected: (T) -> Unit
) {
    val dropdownContentAlignment = remember(textAlign) {
        return@remember when (textAlign) {
            TextAlign.Start -> Alignment.Start
            TextAlign.Center -> Alignment.CenterHorizontally
            TextAlign.End -> Alignment.End
            else -> Alignment.Start
        }
    }

    Spinner(
        modifier = modifier,
        items = strings.keys.toList(),
        selectedItem = selectedItem,
        onItemSelected = onItemSelected,
        enabled = enabled,
        selectedItemFactory = { modifier, item, expanded ->
            Text(
                modifier = modifier.weight(1f),
                text = strings[item] ?: "",
                textAlign = textAlign,
                fontSize = MaterialTheme.typography.body1.fontSize,
                fontWeight = MaterialTheme.typography.body1.fontWeight,
            )
        },
        dropdownItemFactory = { item, _ ->
            Text(
                text = strings[item] ?: "",
                textAlign = textAlign,
                fontSize = MaterialTheme.typography.body1.fontSize,
                fontWeight = MaterialTheme.typography.body1.fontWeight
            )
        },
        paddingValues = paddingValues,
        dropdownContentAlignment = dropdownContentAlignment,
    )
}

