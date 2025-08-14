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

import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.size
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp

val cardMarginSmall: Dp = 4.dp
val halfDialogInputSpacing: Dp = 5.dp
val cardPadding: Dp = 10.dp
val dialogInputSpacing: Dp = 10.dp
val inputSpacingLarge: Dp = 20.dp
val cardElevation = 4.dp
val inputSpacingXLarge = 50.dp


@Composable
fun CardMarginSmall() = Spacer(modifier = Modifier.size(cardMarginSmall))

@Composable
fun HalfDialogInputSpacing() = Spacer(modifier = Modifier.size(halfDialogInputSpacing))

@Composable
fun CardPadding() = Spacer(modifier = Modifier.size(cardPadding))

@Composable
fun DialogInputSpacing() = Spacer(modifier = Modifier.size(dialogInputSpacing))

@Composable
fun InputSpacingLarge() = Spacer(modifier = Modifier.size(inputSpacingLarge))

@Composable
fun InputSpacingXLarge() = Spacer(modifier = Modifier.size(inputSpacingXLarge))
