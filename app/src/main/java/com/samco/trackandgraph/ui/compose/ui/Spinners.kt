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
import androidx.compose.ui.graphics.Color
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
) = CircleSpinner(
    modifier = modifier,
    numItems = colors.size,
    selectedIndex = selectedColor,
    onIndexSelected = { onColorSelected(it) },
    circleContent = { index -> ColorCircle(color = colors[index]) }
)

@Composable
fun CircleSpinner(
    modifier: Modifier = Modifier,
    numItems: Int,
    selectedIndex: Int,
    onIndexSelected: (Int) -> Unit,
    circleContent: @Composable (Int) -> Unit,
) = Spinner(
    modifier = modifier,
    items = (0 until numItems).toList(),
    selectedItem = selectedIndex,
    onItemSelected = { onIndexSelected(it) },
    selectedItemFactory = { mod, index, _ ->
        Circle(
            modifier = mod,
            content = { circleContent(index) }
        )
    },
    dropdownItemFactory = { index, _ ->
        SpacingExtraSmall()
        Circle(content = { circleContent(index) })
        SpacingExtraSmall()
    },
    enableTrailingIcon = false,
    dropdownContentAlignment = Alignment.CenterHorizontally
)

@Composable
fun ColorCircle(
    modifier: Modifier = Modifier,
    @ColorRes color: Int
) = Circle(
    modifier = modifier,
    backgroundColor = colorResource(id = color)
) {}

@Composable
fun Circle(
    modifier: Modifier = Modifier,
    backgroundColor: Color = MaterialTheme.colors.surface,
    content: @Composable () -> Unit
) = Card(
    modifier = modifier.size(54.dp),
    shape = RoundedCornerShape(100),
    backgroundColor = backgroundColor,
    elevation = 0.dp,
    content = content
)