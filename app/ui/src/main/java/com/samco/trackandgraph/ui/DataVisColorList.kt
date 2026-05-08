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

package com.samco.trackandgraph.ui

import androidx.compose.ui.graphics.Color

//this is a number coprime to the number of colours used to select them in a pseudo random order for greater contrast
const val dataVisColorGenerator = 7

// Data visualisation colors have been moved to Compose in DataVisColorList.kt
// Some useful tools for finding color blind safe color palettes:
// For checking how safe the palette is: https://projects.susielu.com/viz-palette
// For generating palette's: http://paletton.com/#uid=12K0Q0kw0w0jyC+oRxVy4oIDfjr
// The source for the original scheme: https://colorbrewer2.org/#type=diverging&scheme=RdYlBu&n=10
val dataVisColorList = listOf(
    Color(0xFFA50026),
    Color(0xFFD73027),
    Color(0xFFF46D43),
    Color(0xFFFDAE61),
    Color(0xFFFEE090),
    Color(0xFFE0F3F8),
    Color(0xFFABD9E9),
    Color(0xFF74ADD1),
    Color(0xFF4575B4),
    Color(0xFF313695),
    Color(0xFF54D931),
    Color(0xFF1B8200),
)
