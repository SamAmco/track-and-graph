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
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R

val cardMarginSmall: Dp
    @Composable
    get() = dimensionResource(id = R.dimen.card_margin_small)

val halfDialogInputSpacing: Dp
    @Composable
    get() = dimensionResource(id = R.dimen.half_dialog_input_spacing)

val cardPadding: Dp
    @Composable
    get() = dimensionResource(id = R.dimen.card_padding)

val dialogInputSpacing: Dp
    @Composable
    get() = dimensionResource(id = R.dimen.dialog_input_spacing)

val inputSpacingLarge: Dp
    @Composable
    get() = dimensionResource(id = R.dimen.input_spacing_large)

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
