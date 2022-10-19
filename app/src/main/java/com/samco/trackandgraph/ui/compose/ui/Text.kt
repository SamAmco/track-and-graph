package com.samco.trackandgraph.ui.compose.ui/*
* This file is part of Track & Graph
*
* Track & Graph is free software: you can redistribute it and/or modify
* it under the terms of the GNU General Public License as published by
* the Free Software Foundation, either version 3 of the License, or
* (at your option) any later version.
*
* Track & Graph is distributed in the hope that it will be useful,
* but WITHOUT ANY WARRANTY; without even the implied warranty of
* MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
* GNU General Public License for more details.
*
* You should have received a copy of the GNU General Public License
* along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
*/
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme

@Preview
@Composable
private fun TrackerNameHeadlinePreview() = TnGComposeTheme {
    TrackerNameHeadline(name = "Tracker name")
}

@Composable
fun TrackerNameHeadline(
    name: String
) = Column(Modifier.width(IntrinsicSize.Max)) {
    Text(
        modifier = Modifier.wrapContentWidth(),
        text = name,
        fontSize = MaterialTheme.typography.headlineMedium.fontSize,
        fontWeight = MaterialTheme.typography.headlineMedium.fontWeight
    )
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.secondary)
            .fillMaxWidth()
            .height(1.dp)
    )
}

