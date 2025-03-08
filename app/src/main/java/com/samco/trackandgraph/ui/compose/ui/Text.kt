/*
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
package com.samco.trackandgraph.ui.compose.ui

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors

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
        textAlign = TextAlign.Center,
        fontSize = MaterialTheme.typography.h4.fontSize,
        fontWeight = MaterialTheme.typography.h4.fontWeight
    )
    Box(
        Modifier
            .background(MaterialTheme.colors.secondary)
            .fillMaxWidth()
            .height(1.dp)
    )
}

@Composable
fun TextBody1(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) = Text(
    modifier = modifier,
    text = text,
    color = MaterialTheme.tngColors.textColorSecondary,
    textAlign = textAlign,
    style = MaterialTheme.typography.body1,
    maxLines = maxLines
)

@Composable
fun TextSubtitle2(
    modifier: Modifier = Modifier,
    text: String,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE
) = Text(
    modifier = modifier,
    text = text,
    textAlign = textAlign,
    style = MaterialTheme.typography.subtitle2,
    color = MaterialTheme.colors.onSurface,
    maxLines = maxLines
)

@Composable
fun TextLink(
    text: String,
    modifier: Modifier = Modifier,
    textAlign: TextAlign? = null,
    maxLines: Int = Int.MAX_VALUE,
    onClick: () -> Unit
) = Text(
    modifier = modifier.clickable(onClick = onClick),
    text = text,
    textAlign = textAlign,
    style = MaterialTheme.typography.body1,
    color = MaterialTheme.colors.secondaryVariant,
    maxLines = maxLines
)