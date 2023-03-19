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

import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.ui.dataVisColorList

@Composable
fun <T> Spinner(
    modifier: Modifier = Modifier,
    dropDownModifier: Modifier = Modifier,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    selectedItemFactory: @Composable (Modifier, T, Boolean) -> Unit,
    dropdownItemFactory: @Composable (T, Int) -> Unit,
    enableTrailingIcon: Boolean = true,
    dropdownContentAlignment: Alignment.Horizontal = Alignment.Start
) {
    var expanded: Boolean by remember { mutableStateOf(false) }

    Box(modifier = modifier.wrapContentSize(Alignment.TopStart)) {
        Row(
            modifier = modifier
                .padding(horizontal = dimensionResource(id = R.dimen.card_padding))
                .clickable { expanded = !expanded },
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectedItemFactory(Modifier, selectedItem, expanded)
            if (enableTrailingIcon)
                ExposedDropdownMenuDefaults.TrailingIcon(expanded) { expanded = !expanded }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false },
            modifier = dropDownModifier
                .background(color = MaterialTheme.colors.surface)
        ) {
            items.forEachIndexed { index, element ->
                DropdownMenuItem(
                    onClick = {
                        onItemSelected(items[index])
                        expanded = false
                    }
                ) {
                    Column(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalAlignment = dropdownContentAlignment
                    ) {
                        dropdownItemFactory(element, index)
                    }
                }
            }
        }
    }
}

@Composable
fun ColorSpinner(
    modifier: Modifier = Modifier,
    colors: List<Int> = dataVisColorList,
    selectedColor: Int,
    onColorSelected: (Int) -> Unit
) = Spinner(
    modifier = modifier,
    items = dataVisColorList.indices.toList(),
    selectedItem = selectedColor,
    onItemSelected = { onColorSelected(it) },
    selectedItemFactory = { mod, index, _ -> ColorCircle(modifier = mod, color = colors[index]) },
    dropdownItemFactory = { index, _ ->
        SpacingExtraSmall()
        ColorCircle(color = colors[index])
        SpacingExtraSmall()
    },
    enableTrailingIcon = false,
    dropdownContentAlignment = Alignment.CenterHorizontally
)

@Composable
fun ColorCircle(
    modifier: Modifier = Modifier,
    @ColorRes color: Int
) = Card(
    modifier = modifier.size(60.dp),
    backgroundColor = colorResource(id = color),
    shape = RoundedCornerShape(100),
    elevation = 0.dp,
    content = {}
)
