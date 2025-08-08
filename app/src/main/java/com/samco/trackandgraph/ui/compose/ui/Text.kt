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
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.IntrinsicSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.layout.wrapContentWidth
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.text.TextStyle
import androidx.compose.ui.text.font.FontStyle
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.helpers.formatDayMonthYearHourMinuteWeekDayOneLine
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import org.threeten.bp.OffsetDateTime

@Preview(showBackground = true)
@Composable
private fun TextPreview() = TnGComposeTheme {
    Column(
        verticalArrangement = Arrangement.spacedBy(cardPadding),
    ) {
        Text(
            style = MaterialTheme.typography.h1,
            text = "Text h1"
        )
        Text(
            style = MaterialTheme.typography.h2,
            text = "Text h2"
        )
        Text(
            style = MaterialTheme.typography.h3,
            text = "Text h3"
        )
        Text(
            style = MaterialTheme.typography.h4,
            text = "Text h4"
        )
        Text(
            style = MaterialTheme.typography.h5,
            text = "Text h5"
        )
        Text(
            style = MaterialTheme.typography.h6,
            text = "Text h6"
        )
        Text(
            style = MaterialTheme.typography.body1,
            text = "Text body 1"
        )
        Text(
            style = MaterialTheme.typography.body2,
            text = "Text body 2"
        )
        Text(
            style = MaterialTheme.typography.subtitle1,
            text = "Text subtitle 1"
        )
        Text(
            style = MaterialTheme.typography.subtitle2,
            text = "Text subtitle 2"
        )

        TrackerNameHeadline(name = "Tracker name")
        TextBody1(text = "Text body 1")
        TextSubtitle2(text = "Text body 2")
        TextLink(text = "Text link", onClick = {})
        DayMonthYearHourMinuteWeekDayOneLineText(
            dateTime = OffsetDateTime.parse("2025-07-25T10:30:00Z"),
        )
    }
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

@Composable
fun DayMonthYearHourMinuteWeekDayOneLineText(
    dateTime: OffsetDateTime,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.body2,
    fontWeight: FontWeight = FontWeight.Bold,
    fontStyle: FontStyle = FontStyle.Italic,
    overflow: TextOverflow = TextOverflow.Ellipsis,
    maxLines: Int = 1,
) {
    val context = LocalContext.current
    val weekDayNames = remember {
        listOf(
            context.getString(R.string.mon),
            context.getString(R.string.tue),
            context.getString(R.string.wed),
            context.getString(R.string.thu),
            context.getString(R.string.fri),
            context.getString(R.string.sat),
            context.getString(R.string.sun)
        )
    }

    val isPreview = LocalInspectionMode.current

    val formattedText = remember(dateTime, weekDayNames, isPreview) {
        val offsetDiffHours = if (isPreview) 1 else null
        formatDayMonthYearHourMinuteWeekDayOneLine(
            context = context, 
            weekDayNames = weekDayNames, 
            dateTime = dateTime,
            offsetDiffHours = offsetDiffHours
        )
    }
    
    Text(
        modifier = modifier,
        text = formattedText,
        style = style,
        fontWeight = fontWeight,
        fontStyle = fontStyle,
        overflow = overflow,
        maxLines = maxLines
    )
}