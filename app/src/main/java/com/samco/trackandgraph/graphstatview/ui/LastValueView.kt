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

package com.samco.trackandgraph.graphstatview.ui

import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material.*
import androidx.compose.runtime.Composable
import androidx.compose.runtime.CompositionLocalProvider
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.dimensionResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.helpers.formatDayMonthYearHourMinuteWeekDayTwoLines
import com.samco.trackandgraph.base.helpers.formatTimeToDaysHoursMinutesSeconds
import com.samco.trackandgraph.base.helpers.getWeekDayNames
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueData
import com.samco.trackandgraph.ui.compose.theming.tngColors
import com.samco.trackandgraph.ui.compose.ui.DataPointValueAndDescription
import com.samco.trackandgraph.ui.compose.ui.SpacingLarge
import com.samco.trackandgraph.ui.compose.ui.SpacingSmall
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

@Composable
fun LastValueStatView(
    modifier: Modifier = Modifier,
    viewData: ILastValueData,
    listMode: Boolean,
    graphHeight: Int? = null
) = CompositionLocalProvider(
    LocalContentColor provides MaterialTheme.tngColors.onSurface,
) {
    viewData.lastDataPoint?.let {
        LastValueStatViewBody(
            modifier = modifier,
            dataPoint = it,
            isDuration = viewData.isDuration,
            listMode = listMode,
            graphHeight = graphHeight
        )

    } ?: GraphErrorView(
        modifier = modifier,
        error = R.string.graph_stat_view_not_enough_data_stat
    )
}

@Composable
private fun LastValueStatViewBody(
    modifier: Modifier = Modifier,
    dataPoint: DataPoint,
    isDuration: Boolean,
    listMode: Boolean,
    graphHeight: Int?
) = Column(
    modifier = modifier
        .padding(dimensionResource(id = R.dimen.card_padding))
        .let {
            if (graphHeight != null) it.height(graphHeight.dp)
            else it
        },
    horizontalAlignment = Alignment.CenterHorizontally
) {
    val context = LocalContext.current

    val weekdayNames = getWeekDayNames(context)

    val duration = Duration.between(dataPoint.timestamp, OffsetDateTime.now())

    val durationText =
        formatTimeToDaysHoursMinutesSeconds(context, duration.toMillis(), false)

    Text(
        text = stringResource(id = R.string.time_ago, durationText),
        style = MaterialTheme.typography.h5
    )

    SpacingSmall()

    Box(
        modifier = Modifier
            .border(
                1.dp,
                MaterialTheme.colors.onSurface.copy(alpha = 0.12f),
                MaterialTheme.shapes.small
            )
            .alpha(0.8f)
    ) {
        Row(
            modifier = Modifier.padding(dimensionResource(id = R.dimen.card_padding)),
            verticalAlignment = Alignment.CenterVertically
        ) {

            Text(
                text = formatDayMonthYearHourMinuteWeekDayTwoLines(
                    context,
                    weekdayNames,
                    dataPoint.timestamp
                ),
                style = MaterialTheme.typography.body2,
                textAlign = TextAlign.Center,
            )

            SpacingLarge()

            DataPointValueAndDescription(
                modifier = Modifier.weight(1f),
                dataPoint = dataPoint,
                isDuration = isDuration,
                restrictNoteText = listMode
            )
        }
    }
}