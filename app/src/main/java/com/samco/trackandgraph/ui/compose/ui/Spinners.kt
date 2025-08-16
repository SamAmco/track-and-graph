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

import androidx.annotation.ColorRes
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.RowScope
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.wrapContentSize
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.platform.LocalFocusManager
import androidx.compose.ui.res.colorResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.material3.Text
import com.samco.trackandgraph.ui.dataVisColorList
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun <T> Spinner(
    modifier: Modifier = Modifier,
    dropDownModifier: Modifier = Modifier,
    items: List<T>,
    selectedItem: T,
    onItemSelected: (T) -> Unit,
    selectedItemFactory: @Composable RowScope.(Modifier, T, Boolean) -> Unit,
    dropdownItemFactory: @Composable (T, Int) -> Unit,
    enabled: Boolean = true,
    enableTrailingIcon: Boolean = true,
    paddingValues: PaddingValues = PaddingValues(cardPadding),
    dropdownContentAlignment: Alignment.Horizontal = Alignment.Start
) {
    var expanded: Boolean by remember { mutableStateOf(false) }
    val focusManager = LocalFocusManager.current

    Box(
        modifier = modifier
            .wrapContentSize(Alignment.TopStart)
    ) {
        Row(
            modifier = modifier
                .let {
                    if (enabled) it.clickable {
                        if (!expanded) focusManager.clearFocus()
                        expanded = !expanded
                    }
                    else it
                }
                .padding(paddingValues),
            verticalAlignment = Alignment.CenterVertically
        ) {
            selectedItemFactory(Modifier, selectedItem, expanded)
            if (enableTrailingIcon) {
                ExposedDropdownMenuDefaults.TrailingIcon(expanded)
            }
        }

        val dropdownPositionAlignment = remember(dropdownContentAlignment) {
            return@remember when (dropdownContentAlignment) {
                Alignment.Start -> Alignment.TopStart
                Alignment.CenterHorizontally -> Alignment.Center
                Alignment.End -> Alignment.TopEnd
                else -> Alignment.TopStart
            }
        }

        Box(modifier = Modifier.align(dropdownPositionAlignment)) {
            DropdownMenu(
                expanded = expanded,
                onDismissRequest = { expanded = false },
                modifier = dropDownModifier
                    .background(color = MaterialTheme.colorScheme.surface)
            ) {
                items.forEachIndexed { index, element ->
                    DropdownMenuItem(
                        enabled = enabled,
                        onClick = {
                            onItemSelected(items[index])
                            expanded = false
                        },
                        text = {
                            Column(
                                modifier = Modifier.fillMaxWidth(),
                                horizontalAlignment = dropdownContentAlignment
                            ) {
                                dropdownItemFactory(element, index)
                            }
                        }
                    )
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
        HalfDialogInputSpacing()
        Circle(content = { circleContent(index) })
        HalfDialogInputSpacing()
    },
    enableTrailingIcon = false,
    dropdownContentAlignment = Alignment.CenterHorizontally
)

@Composable
fun ColorCircle(
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    color: Color,
) = Circle(
    modifier = modifier,
    size = size,
    backgroundColor = color
) {}

@Composable
fun ColorCircle(
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    @ColorRes color: Int,
) = ColorCircle(
    modifier = modifier,
    size = size,
    color = colorResource(color)
)

@Composable
fun Circle(
    modifier: Modifier = Modifier,
    size: Dp = 54.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.surface,
    content: @Composable () -> Unit
) = Card(
    modifier = modifier.size(size),
    shape = RoundedCornerShape(100),
    colors = CardDefaults.cardColors(containerColor = backgroundColor),
    elevation = CardDefaults.cardElevation(defaultElevation = 0.dp)
) {
    content()
}

// Previews
@Preview(showBackground = true)
@Composable
fun SpinnerPreview() {
    TnGComposeTheme {
        val items = listOf("Option 1", "Option 2", "Option 3", "Option 4")
        val selectedItem = remember { mutableStateOf(items[0]) }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Generic Spinner Example:")
            Spinner(
                items = items,
                selectedItem = selectedItem.value,
                onItemSelected = { selectedItem.value = it },
                selectedItemFactory = { modifier, item, expanded ->
                    Text(
                        text = item,
                        modifier = modifier.weight(1f),
                        style = MaterialTheme.typography.bodyLarge
                    )
                },
                dropdownItemFactory = { item, _ ->
                    Text(
                        text = item,
                        style = MaterialTheme.typography.bodyMedium,
                        modifier = Modifier.padding(8.dp)
                    )
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorSpinnerPreview() {
    TnGComposeTheme {
        val selectedColor = remember { mutableStateOf(0) }

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Color Spinner Example:")
            ColorSpinner(
                selectedColor = selectedColor.value,
                onColorSelected = { selectedColor.value = it }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CircleSpinnerPreview() {
    TnGComposeTheme {
        val selectedIndex = remember { mutableStateOf(0) }
        val colors = listOf(Color.Red, Color.Green, Color.Blue, Color.Yellow, Color.Magenta)

        Column(modifier = Modifier.padding(16.dp)) {
            Text("Circle Spinner Example:")
            CircleSpinner(
                numItems = colors.size,
                selectedIndex = selectedIndex.value,
                onIndexSelected = { selectedIndex.value = it },
                circleContent = { index ->
                    ColorCircle(color = colors[index])
                }
            )
        }
    }
}

@Preview(showBackground = true)
@Composable
fun ColorCirclePreview() {
    TnGComposeTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Color Circle Examples:")
            Row {
                ColorCircle(color = Color.Red)
                ColorCircle(color = Color.Green, size = 40.dp)
                ColorCircle(color = Color.Blue, size = 30.dp)
            }
        }
    }
}

@Preview(showBackground = true)
@Composable
fun CirclePreview() {
    TnGComposeTheme {
        Column(modifier = Modifier.padding(16.dp)) {
            Text("Circle Examples:")
            Row {
                Circle {
                    Text("A", modifier = Modifier.padding(16.dp))
                }
                Circle(backgroundColor = Color.LightGray) {
                    Text("B", modifier = Modifier.padding(16.dp))
                }
                Circle(size = 40.dp, backgroundColor = Color.Cyan) {
                    Text("C", modifier = Modifier.padding(8.dp))
                }
            }
        }
    }
}