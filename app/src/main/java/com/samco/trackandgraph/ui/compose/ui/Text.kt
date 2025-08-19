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
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.Typography
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
import androidx.compose.ui.unit.sp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.helpers.formatDayMonthYearHourMinuteWeekDayOneLine
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.tngColors
import org.threeten.bp.OffsetDateTime

// Custom Typography matching original Material 2 text sizes from dimens.xml
val CustomTypography = Typography(
    // Display styles (largest text)
    displayLarge = Typography().displayLarge.copy(fontSize = 70.sp),
    displayMedium = Typography().displayMedium.copy(fontSize = 45.sp),
    displaySmall = Typography().displaySmall.copy(fontSize = 36.sp),

    // Headline styles
    headlineLarge = Typography().headlineLarge.copy(fontSize = 30.sp, fontWeight = FontWeight.Bold),
    headlineMedium = Typography().headlineMedium.copy(fontSize = 22.sp, fontWeight = FontWeight.Bold, lineHeight = 28.sp),
    headlineSmall = Typography().headlineSmall.copy(fontSize = 20.sp, fontWeight = FontWeight.W500),

    // Title styles
    titleLarge = Typography().titleLarge.copy(fontSize = 28.sp),
    titleMedium = Typography().titleMedium.copy(fontSize = 18.sp),
    titleSmall = Typography().titleSmall.copy(fontSize = 16.sp),

    // Body styles
    bodyLarge = Typography().bodyLarge.copy(fontSize = 15.sp),
    bodyMedium = Typography().bodyMedium.copy(fontSize = 15.sp),
    bodySmall = Typography().bodySmall.copy(fontSize = 13.sp),

    // Label styles (smallest text)
    labelLarge = Typography().labelLarge.copy(fontSize = 14.sp),
    labelMedium = Typography().labelMedium.copy(fontSize = 11.sp),
    labelSmall = Typography().labelSmall.copy(fontSize = 10.sp),
)

@Preview(showBackground = true)
@Composable
private fun TextPreview() = TnGComposeTheme {
    Column(
        verticalArrangement = Arrangement.spacedBy(cardPadding),
    ) {
        // Display typography
        Text(
            style = MaterialTheme.typography.displayLarge,
            text = "Display Large"
        )
        Text(
            style = MaterialTheme.typography.displayMedium,
            text = "Display Medium"
        )
        Text(
            style = MaterialTheme.typography.displaySmall,
            text = "Display Small"
        )
        
        // Headline typography
        Text(
            style = MaterialTheme.typography.headlineLarge,
            text = "Headline Large"
        )
        Text(
            style = MaterialTheme.typography.headlineMedium,
            text = "Headline Medium"
        )
        Text(
            style = MaterialTheme.typography.headlineSmall,
            text = "Headline Small"
        )
        
        // Title typography
        Text(
            style = MaterialTheme.typography.titleLarge,
            text = "Title Large"
        )
        Text(
            style = MaterialTheme.typography.titleMedium,
            text = "Title Medium"
        )
        Text(
            style = MaterialTheme.typography.titleSmall,
            text = "Title Small"
        )
        
        // Body typography
        Text(
            style = MaterialTheme.typography.bodyLarge,
            text = "Body Large"
        )
        Text(
            style = MaterialTheme.typography.bodyMedium,
            text = "Body Medium"
        )
        Text(
            style = MaterialTheme.typography.bodySmall,
            text = "Body Small"
        )
        
        // Label typography
        Text(
            style = MaterialTheme.typography.labelLarge,
            text = "Label Large"
        )
        Text(
            style = MaterialTheme.typography.labelMedium,
            text = "Label Medium"
        )
        Text(
            style = MaterialTheme.typography.labelSmall,
            text = "Label Small"
        )

        TrackerNameHeadline(name = "Tracker name")
        TextBody1(text = "Text body 1")
        TextSubtitle2(text = "Text body 2")
        TextLink(text = "Text link", onClick = {})
        EmptyPageHintText(text = "Empty page hint text")
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
        style = MaterialTheme.typography.titleLarge,
        textAlign = TextAlign.Center,
    )
    Box(
        Modifier
            .background(MaterialTheme.colorScheme.secondary)
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
    style = MaterialTheme.typography.bodyLarge,
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
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurface,
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
    style = MaterialTheme.typography.bodyLarge,
    color = MaterialTheme.tngColors.hyperlinkColor,
    maxLines = maxLines
)

@Composable
fun EmptyPageHintText(
    modifier: Modifier = Modifier,
    text: String,
) = Text(
    modifier = modifier,
    text = text,
    style = MaterialTheme.typography.titleSmall,
    color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.6f),
    textAlign = TextAlign.Center,
)

@Composable
fun DayMonthYearHourMinuteWeekDayOneLineText(
    dateTime: OffsetDateTime,
    modifier: Modifier = Modifier,
    style: TextStyle = MaterialTheme.typography.bodyMedium,
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