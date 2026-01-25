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

package com.samco.trackandgraph.ui.compose.utils

import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.ui.unit.LayoutDirection

operator fun PaddingValues.plus(other: PaddingValues): PaddingValues = object : PaddingValues {
    override fun calculateLeftPadding(layoutDirection: LayoutDirection) =
        this@plus.calculateLeftPadding(layoutDirection) +
            other.calculateLeftPadding(layoutDirection)

    override fun calculateTopPadding() =
        this@plus.calculateTopPadding() + other.calculateTopPadding()

    override fun calculateRightPadding(layoutDirection: LayoutDirection) =
        this@plus.calculateRightPadding(layoutDirection) +
            other.calculateRightPadding(layoutDirection)

    override fun calculateBottomPadding() =
        this@plus.calculateBottomPadding() + other.calculateBottomPadding()
}
