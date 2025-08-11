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

package com.samco.trackandgraph.group

import android.content.Context
import android.text.format.DateUtils
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.clickable
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxWithConstraints
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material.Card
import androidx.compose.material.DropdownMenu
import androidx.compose.material.DropdownMenuItem
import androidx.compose.material.Icon
import androidx.compose.material.IconButton
import androidx.compose.material.MaterialTheme
import androidx.compose.material.Text
import androidx.compose.material.ripple
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalDensity
import androidx.compose.ui.platform.LocalInspectionMode
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import com.samco.trackandgraph.R
import com.samco.trackandgraph.base.database.dto.DataType
import com.samco.trackandgraph.base.database.dto.DisplayTracker
import com.samco.trackandgraph.helpers.formatTimeDuration
import com.samco.trackandgraph.ui.compose.theming.TnGComposeTheme
import com.samco.trackandgraph.ui.compose.theming.fadedGreen
import com.samco.trackandgraph.ui.compose.ui.DialogInputSpacing
import com.samco.trackandgraph.ui.compose.ui.buttonSize
import com.samco.trackandgraph.ui.compose.ui.cardElevation
import com.samco.trackandgraph.ui.compose.ui.cardMarginSmall
import com.samco.trackandgraph.ui.compose.ui.inputSpacingLarge
import kotlinx.coroutines.delay
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import kotlin.math.hypot

/**
 * Composable that displays a tracker item card with timer functionality,
 * context menu, and click handling for add/history actions.
 */
@Composable
fun Tracker(
    modifier: Modifier = Modifier,
    isElevated: Boolean = false,
    tracker: DisplayTracker,
    onEdit: (DisplayTracker) -> Unit,
    onDelete: (DisplayTracker) -> Unit,
    onMoveTo: (DisplayTracker) -> Unit,
    onDescription: (DisplayTracker) -> Unit,
    onAdd: (DisplayTracker, useDefault: Boolean) -> Unit,
    onHistory: (DisplayTracker) -> Unit,
    onPlayTimer: (DisplayTracker) -> Unit,
    onStopTimer: (DisplayTracker) -> Unit,
) = Box(modifier = modifier.fillMaxWidth()) {
    val context = LocalContext.current
    val density = LocalDensity.current

    var showContextMenu by remember { mutableStateOf(false) }
    var timerText by remember { mutableStateOf("") }

    val noDataText = stringResource(R.string.no_data)
    var timeSinceLastText by remember(tracker.timestamp) {
        mutableStateOf(
            tracker.timestamp
                ?.let { formatRelativeTimeSpan(context, it, Duration.ZERO) }
                ?: noDataText
        )
    }

    // Timer update effect for DURATION trackers
    LaunchedEffect(tracker.timerStartInstant, tracker.dataType) {
        while (tracker.dataType == DataType.DURATION && tracker.timerStartInstant != null) {
            val duration = Duration.between(tracker.timerStartInstant, Instant.now())
            timerText = formatTimeDuration(duration.seconds)
            delay(1000) // Update every second
        }
    }

    val previewMode = LocalInspectionMode.current
    // Last tracked time stamp update effect
    LaunchedEffect(tracker.timestamp) {
        val timestamp = tracker.timestamp
        while (true) {
            if (timestamp == null) {
                timeSinceLastText = noDataText
            } else {
                val duration = if (previewMode) Duration.ZERO else Duration.between(timestamp.toInstant(), Instant.now())
                timeSinceLastText = formatRelativeTimeSpan(context, timestamp, duration)
            }
            delay(1000) // Update every second
        }
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(cardMarginSmall)
            .clickable { onHistory(tracker) },
        elevation = if (isElevated) cardElevation * 3 else cardElevation,
        shape = MaterialTheme.shapes.small,
    ) {
        BoxWithConstraints {
            // Calculate ripple radius to fill entire card
            val rippleRadius = remember(maxWidth, maxHeight) {
                with(density) {
                    hypot(maxWidth.toPx(), maxHeight.toPx()).toDp() / 2
                }
            }

            Column(
                modifier = Modifier.fillMaxWidth(),
            ) {
                TrackerMenuButton(
                    modifier = Modifier.align(Alignment.End),
                    showContextMenu = showContextMenu,
                    onShowContextMenu = { showContextMenu = it },
                    tracker = tracker,
                    onEdit = onEdit,
                    onDelete = onDelete,
                    onMoveTo = onMoveTo,
                    onDescription = onDescription
                )

                TrackerNameText(trackerName = tracker.name)

                DialogInputSpacing()

                TrackerDateTimeArea(
                    tracker = tracker,
                    timerText = timerText,
                    timeSinceLastText = timeSinceLastText
                )

                TrackerButtonsArea(
                    tracker = tracker,
                    rippleRadius = rippleRadius,
                    onPlayTimer = onPlayTimer,
                    onStopTimer = onStopTimer,
                    onAdd = onAdd
                )
            }
        }
    }
}

@Composable
private fun TrackerMenuButton(
    modifier: Modifier = Modifier,
    showContextMenu: Boolean,
    onShowContextMenu: (Boolean) -> Unit,
    tracker: DisplayTracker,
    onEdit: (DisplayTracker) -> Unit,
    onDelete: (DisplayTracker) -> Unit,
    onMoveTo: (DisplayTracker) -> Unit,
    onDescription: (DisplayTracker) -> Unit
) {
    Box(
        modifier = modifier.size(buttonSize)
    ) {
        IconButton(
            modifier = Modifier.size(buttonSize),
            onClick = { onShowContextMenu(true) },
        ) {
            Icon(
                painterResource(R.drawable.list_menu_icon),
                contentDescription = stringResource(R.string.tracked_data_menu_button_content_description),
                tint = MaterialTheme.colors.onSurface
            )
        }

        // Context menu
        DropdownMenu(
            expanded = showContextMenu,
            onDismissRequest = { onShowContextMenu(false) }
        ) {
            DropdownMenuItem(
                onClick = {
                    onShowContextMenu(false)
                    onEdit(tracker)
                }
            ) {
                Text(stringResource(R.string.edit))
            }
            DropdownMenuItem(
                onClick = {
                    onShowContextMenu(false)
                    onDelete(tracker)
                }
            ) {
                Text(stringResource(R.string.delete))
            }
            DropdownMenuItem(
                onClick = {
                    onShowContextMenu(false)
                    onMoveTo(tracker)
                }
            ) {
                Text(stringResource(R.string.move_to))
            }
            DropdownMenuItem(
                onClick = {
                    onShowContextMenu(false)
                    onDescription(tracker)
                }
            ) {
                Text(stringResource(R.string.description))
            }
        }
    }
}

@Composable
private fun TrackerNameText(
    trackerName: String
) {
    Text(
        text = trackerName,
        modifier = Modifier
            .fillMaxWidth()
            .padding(cardMarginSmall),
        style = MaterialTheme.typography.h5,
        fontWeight = FontWeight.Bold,
        textAlign = TextAlign.Center,
        maxLines = 10,
        overflow = TextOverflow.Ellipsis
    )
}

@Composable
private fun TrackerDateTimeArea(
    tracker: DisplayTracker,
    timerText: String,
    timeSinceLastText: String
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .padding(horizontal = inputSpacingLarge),
        contentAlignment = Alignment.Center
    ) {
        if (tracker.dataType == DataType.DURATION && tracker.timerStartInstant != null) {
            Text(
                text = timerText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.h6,
                color = MaterialTheme.colors.error,
                maxLines = 1
            )
        } else {
            Text(
                text = timeSinceLastText,
                modifier = Modifier.fillMaxWidth(),
                textAlign = TextAlign.Center,
                style = MaterialTheme.typography.body1
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class)
@Composable
private fun TrackerButtonsArea(
    tracker: DisplayTracker,
    rippleRadius: Dp,
    onPlayTimer: (DisplayTracker) -> Unit,
    onStopTimer: (DisplayTracker) -> Unit,
    onAdd: (DisplayTracker, Boolean) -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxWidth()
            .height(buttonSize)
    ) {
        // Timer control buttons (for DURATION trackers)
        if (tracker.dataType == DataType.DURATION) {
            IconButton(
                modifier = Modifier
                    .size(buttonSize)
                    .align(Alignment.BottomCenter),
                onClick = if (tracker.timerStartInstant == null) {
                    { onPlayTimer(tracker) }
                } else {
                    { onStopTimer(tracker) }
                },
            ) {
                val isRunning = tracker.timerStartInstant != null
                Icon(
                    painter =
                        if (isRunning) painterResource(R.drawable.ic_stop_timer)
                        else painterResource(R.drawable.ic_play_timer),
                    contentDescription = null,
                    tint = if (isRunning) MaterialTheme.colors.error else fadedGreen
                )
            }
        }

        // Add button with unbounded ripple that fills the card
        val rippleIndication = remember(rippleRadius) {
            ripple(
                bounded = false,
                radius = rippleRadius
            )
        }

        Icon(
            painterResource(R.drawable.ic_add_record),
            contentDescription = stringResource(R.string.add_data_point_button_content_description),
            modifier = Modifier
                .size(buttonSize)
                .align(Alignment.BottomEnd)
                .combinedClickable(
                    interactionSource = remember { MutableInteractionSource() },
                    indication = rippleIndication,
                    onClick = { onAdd(tracker, tracker.hasDefaultValue) },
                    onLongClick = { onAdd(tracker, false) }
                ),
            tint = if (tracker.hasDefaultValue) MaterialTheme.colors.secondary else MaterialTheme.colors.onSurface
        )
    }
}

private fun formatRelativeTimeSpan(
    context: Context,
    dateTime: OffsetDateTime,
    duration: Duration,
): String {
    //There's a special case here where the time since is less than a day and more than an hour ago
    //because DateUtils.getRelativeTimeSpanString() will not mention how many minutes ago it was. So
    //it would just say 1 hour ago even if it was 1 hour and 59 minutes ago.
    //DateUtils.getRelativeTimeSpanString() still does most of the logic though for the various
    //edge cases and across locales
    return if (!duration.isNegative && duration.toDays() < 1 && duration.toMinutes() > 60) {
        val hours = duration.toHours()
        val minutes = duration.toMinutes() % 60
        context.getString(R.string.hours_and_minutes_ago, hours, minutes)
    } else DateUtils.getRelativeTimeSpanString(dateTime.toInstant().toEpochMilli()).toString()
}

@Preview(showBackground = true)
@Composable
fun TrackerPreview() {
    TnGComposeTheme {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {

            // Tracker with default value (quick add)
            Tracker(
                tracker = DisplayTracker(
                    id = 4,
                    groupId = 1,
                    name = "Water Intake",
                    description = "",
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = true,
                    defaultValue = 1.0,
                    defaultLabel = "Glass",
                    featureId = 4,
                    timestamp = null,
                    displayIndex = 3,
                    timerStartInstant = null
                ),
                onEdit = {},
                onDelete = {},
                onMoveTo = {},
                onDescription = {},
                onAdd = { _, _ -> },
                onHistory = {},
                onPlayTimer = {},
                onStopTimer = {}
            )

            // Regular tracker with data
            Tracker(
                tracker = DisplayTracker(
                    id = 1,
                    groupId = 1,
                    name = "Daily Exercise",
                    description = "",
                    dataType = DataType.CONTINUOUS,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    featureId = 1,
                    timestamp = OffsetDateTime.parse("2024-01-15T10:30:00+01:00"),
                    displayIndex = 0,
                    timerStartInstant = null
                ),
                onEdit = {},
                onDelete = {},
                onMoveTo = {},
                onDescription = {},
                onAdd = { _, _ -> },
                onHistory = {},
                onPlayTimer = {},
                onStopTimer = {}
            )

            // Duration tracker with running timer
            Tracker(
                tracker = DisplayTracker(
                    id = 2,
                    groupId = 1,
                    name = "Work Session",
                    description = "",
                    dataType = DataType.DURATION,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    featureId = 2,
                    timestamp = null,
                    displayIndex = 1,
                    timerStartInstant = Instant.parse("2025-07-15T10:30:00.00Z").minusSeconds(125)
                ),
                onEdit = {},
                onDelete = {},
                onMoveTo = {},
                onDescription = {},
                onAdd = { _, _ -> },
                onHistory = {},
                onPlayTimer = {},
                onStopTimer = {}
            )

            // Duration tracker with stopped timer
            Tracker(
                tracker = DisplayTracker(
                    id = 3,
                    groupId = 1,
                    name = "Sleep Tracking",
                    description = "",
                    dataType = DataType.DURATION,
                    hasDefaultValue = false,
                    defaultValue = 0.0,
                    defaultLabel = "",
                    featureId = 3,
                    timestamp = OffsetDateTime.parse("2024-01-15T02:30:00+01:00"),
                    displayIndex = 2,
                    timerStartInstant = null
                ),
                onEdit = {},
                onDelete = {},
                onMoveTo = {},
                onDescription = {},
                onAdd = { _, _ -> },
                onHistory = {},
                onPlayTimer = {},
                onStopTimer = {}
            )

        }
    }
}
