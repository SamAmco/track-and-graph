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
package com.samco.trackandgraph.playstore

import androidx.compose.runtime.mutableStateOf
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.text.input.TextFieldValue
import com.samco.trackandgraph.TimeHistogramWindowData
import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.DataPoint
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.DisplayTracker
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.Group as DisplayGroup
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.Period
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.functions.node_editor.viewmodel.Connector
import com.samco.trackandgraph.functions.node_editor.viewmodel.ConnectorType
import com.samco.trackandgraph.functions.node_editor.viewmodel.Edge
import com.samco.trackandgraph.functions.node_editor.viewmodel.Hint
import com.samco.trackandgraph.functions.node_editor.viewmodel.Node
import com.samco.trackandgraph.graphstatview.factories.viewdto.ColorSpec
import com.samco.trackandgraph.graphstatview.factories.viewdto.IGraphStatViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ILastValueViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.IPieChartViewData
import com.samco.trackandgraph.graphstatview.factories.viewdto.ITimeHistogramViewData
import com.samco.trackandgraph.group.CalculatedGraphViewData
import com.samco.trackandgraph.group.GroupChild
import com.samco.trackandgraph.reminders.ui.ReminderViewData
import kotlinx.coroutines.flow.MutableStateFlow
import org.threeten.bp.LocalDateTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneOffset

internal fun playStoreDailyChildren(): List<GroupChild> {
    val trackedAt = OffsetDateTime.of(2026, 5, 7, 22, 0, 0, 0, ZoneOffset.UTC)
    val trackerSpecs = listOf(
        TrackerSpec("Sleep", DataType.DURATION, false, ""),
        TrackerSpec("Productivity", DataType.CONTINUOUS, false, ""),
        TrackerSpec("Alcohol", DataType.CONTINUOUS, false, ""),
        TrackerSpec("Meditation", DataType.DURATION, false, ""),
        TrackerSpec("Work", DataType.DURATION, false, ""),
        TrackerSpec("Weight", DataType.CONTINUOUS, false, ""),
        TrackerSpec("Exercise", DataType.CONTINUOUS, true, "1"),
        TrackerSpec("Studying", DataType.DURATION, false, ""),
        TrackerSpec("Stress", DataType.CONTINUOUS, false, ""),
    )

    return trackerSpecs.mapIndexed { index, spec ->
        val id = index + 1L
        GroupChild.ChildTracker(
            groupItemId = id,
            id = id,
            displayTracker = DisplayTracker(
                id = id,
                featureId = 100L + id,
                name = spec.name,
                dataType = spec.dataType,
                hasDefaultValue = spec.hasDefaultValue,
                defaultValue = if (spec.hasDefaultValue) 1.0 else 0.0,
                defaultLabel = spec.defaultLabel,
                timestamp = trackedAt.minusHours(index.toLong()),
                description = "Just random data",
                timerStartInstant = null,
                unique = true,
            )
        )
    }
}

internal fun playStoreExerciseChildren(): List<GroupChild> =
    exerciseWavePoints().let { exercise ->
        val illness = illnessWavePoints()
        listOf(
            GroupChild.ChildGraph(
                groupItemId = 1L,
                id = 1L,
                graph = CalculatedGraphViewData(
                    time = 0L,
                    viewData = lineGraphViewData(
                        id = 1L,
                        name = "Exercise weekly totals in the last 6 months",
                        lines = listOf(
                            PreviewLine(
                                name = "Exercise",
                                colorIndex = 0,
                                pointStyle = LineGraphPointStyle.CIRCLES_AND_NUMBERS,
                                values = weeklyTotals(exercise, numberOfWeeks = 26)
                            )
                        )
                    )
                )
            ),
            GroupChild.ChildGraph(
                groupItemId = 2L,
                id = 2L,
                graph = CalculatedGraphViewData(
                    time = 0L,
                    viewData = lineGraphViewData(
                        id = 2L,
                        name = "Exercise Vs illness moving averages in the last 6 months",
                        lines = listOf(
                            PreviewLine(
                                name = "Weekly",
                                colorIndex = 7,
                                pointStyle = LineGraphPointStyle.NONE,
                                values = sampledMovingAverage(exercise, windowDays = 7, numberOfWeeks = 26)
                            ),
                            PreviewLine(
                                name = "Monthly",
                                colorIndex = 0,
                                pointStyle = LineGraphPointStyle.NONE,
                                values = sampledMovingAverage(exercise, windowDays = 30, numberOfWeeks = 26)
                            ),
                            PreviewLine(
                                name = "Yearly",
                                colorIndex = 3,
                                pointStyle = LineGraphPointStyle.NONE,
                                values = sampledMovingAverage(exercise, windowDays = 365, numberOfWeeks = 26)
                            ),
                            PreviewLine(
                                name = "Sick day (weekly)",
                                colorIndex = 11,
                                pointStyle = LineGraphPointStyle.NONE,
                                values = sampledMovingAverage(illness, windowDays = 7, numberOfWeeks = 26)
                                    .map { it * 0.1 }
                            ),
                        )
                    )
                )
            ),
        )
    }

internal fun playStoreGroupsListChildren(): List<GroupChild> {
    val groupSpecs = listOf(
        GroupSpec("Meal time tracking", 11),
        GroupSpec("Morning tracking", 6),
        GroupSpec("Daily tracking", 0),
        GroupSpec("Weekly tracking", 2),
        GroupSpec("Exercise routine tracking", 8),
        GroupSpec("Weight loss graphs", 7),
        GroupSpec("Mood quality", 3),
        GroupSpec("Stress and rest statistics", 4),
    )

    return groupSpecs.mapIndexed { index, spec ->
        val id = index + 1L
        GroupChild.ChildGroup(
            groupItemId = id,
            id = id,
            group = DisplayGroup(
                id = id,
                name = spec.name,
                colorIndex = spec.colorIndex,
                unique = true,
            )
        )
    }
}

internal fun playStoreRestDayStatisticsChildren(): List<GroupChild> = listOf(
    graphChild(
        id = 1L,
        viewData = pieChartViewData(
            id = 1L,
            name = "Stress pie chart",
            segments = listOf(
                IPieChartViewData.Segment(21.0, "None", ColorSpec.ColorIndex(0)),
                IPieChartViewData.Segment(34.0, "Low", ColorSpec.ColorIndex(7)),
                IPieChartViewData.Segment(30.0, "Medium", ColorSpec.ColorIndex(3)),
                IPieChartViewData.Segment(15.0, "High", ColorSpec.ColorIndex(11)),
            )
        )
    ),
    graphChild(
        id = 2L,
        viewData = lastValueViewData(
            id = 2L,
            name = "Time since taking a day off",
            dataPoint = DataPoint(
                timestamp = PREVIEW_END_TIME.minusDays(2).minusHours(2).minusMinutes(43),
                featureId = 200L,
                value = 1.0,
                label = "",
                note = "",
            )
        )
    ),
    graphChild(
        id = 3L,
        viewData = timeHistogramViewData(
            id = 3L,
            name = "Most stressful days",
            barValues = listOf(
                ITimeHistogramViewData.BarValue("None", listOf(14.0, 8.0, 5.0, 11.0, 6.0, 18.0, 16.0)),
                ITimeHistogramViewData.BarValue("Low", listOf(28.0, 26.0, 22.0, 30.0, 24.0, 20.0, 18.0)),
                ITimeHistogramViewData.BarValue("Medium", listOf(18.0, 25.0, 28.0, 20.0, 26.0, 14.0, 12.0)),
                ITimeHistogramViewData.BarValue("High", listOf(10.0, 16.0, 22.0, 11.0, 19.0, 8.0, 6.0)),
            )
        )
    ),
)

internal fun playStoreReminders(): List<ReminderViewData> {
    val now = LocalDateTime.of(2026, 5, 8, 12, 0)

    return listOf(
        ReminderViewData.WeekDayReminderViewData(
            id = 1L,
            groupItemId = 1L,
            name = "Tracking dailies",
            enabled = true,
            nextScheduled = now.withHour(22).withMinute(0),
            checkedDays = CheckedDays.all(),
            reminderDto = null,
        ),
        ReminderViewData.PeriodicReminderViewData(
            id = 2L,
            groupItemId = 2L,
            name = "Weekly review",
            enabled = true,
            nextScheduled = now.plusDays(3).withHour(10).withMinute(0),
            starts = now.minusWeeks(2).withHour(10).withMinute(0),
            ends = null,
            interval = 1,
            period = Period.WEEKS,
            reminderDto = null,
            progressToNextReminder = 0.55f,
            isBeforeStartTime = false,
        ),
        ReminderViewData.MonthDayReminderViewData(
            id = 3L,
            groupItemId = 3L,
            name = "Monthly goals",
            enabled = true,
            nextScheduled = LocalDateTime.of(2026, 6, 1, 9, 0),
            occurrence = MonthDayOccurrence.FIRST,
            dayType = MonthDayType.MONDAY,
            ends = null,
            reminderDto = null,
        ),
        ReminderViewData.TimeSinceLastReminderViewData(
            id = 4L,
            groupItemId = 4L,
            name = "2 days without Exercise",
            enabled = true,
            nextScheduled = now.plusHours(6),
            reminderDto = null,
            progressToNextReminder = 0.75f,
            currentInterval = 2,
            currentPeriod = Period.DAYS,
        ),
    )
}

internal class PlayStoreFunctionEditorState {
    internal val featurePathMap = mapOf(
        1L to "Exercise / Running",
        2L to "Exercise / Cycling",
    )

    internal val outputNode = Node.Output(
        id = 1,
        name = mutableStateOf(TextFieldValue("Exercise")),
        description = mutableStateOf(TextFieldValue("")),
        isDuration = mutableStateOf(false),
        isUpdateMode = false,
    )

    internal val nodePositions = mapOf(
        1 to Offset(0.0f, 0.0f),
        2 to Offset(-1502.8608f, -311.32462f),
        3 to Offset(-1491.5502f, 310.2478f),
    )

    internal val connectorPositions = mutableMapOf<Connector, Offset>()

    val nodes = MutableStateFlow(
        listOf(
            Node.DataSource(
                id = 2,
                selectedFeatureId = mutableStateOf(1L),
                featurePathMap = featurePathMap,
            ),
            Node.DataSource(
                id = 3,
                selectedFeatureId = mutableStateOf(2L),
                featurePathMap = featurePathMap,
            ),
            outputNode,
        )
    )

    val edges = MutableStateFlow(
        listOf(
            Edge(
                from = Connector(3, ConnectorType.OUTPUT, 0),
                to = Connector(1, ConnectorType.INPUT, 0),
            ),
            Edge(
                from = Connector(2, ConnectorType.OUTPUT, 0),
                to = Connector(1, ConnectorType.INPUT, 0),
            ),
        )
    )

    val selectedEdge = MutableStateFlow<Edge?>(null)
    val hints = MutableStateFlow(emptyList<Hint>())
    val connectors = MutableStateFlow(emptySet<Connector>())
    val draggingConnector = MutableStateFlow<Connector?>(null)

    fun getWorldPosition(node: Node): Offset? = nodePositions[node.id]

    fun onUpsertConnector(connector: Connector, worldPosition: Offset) {
        connectorPositions[connector] = worldPosition
        connectors.value += connector
    }

    fun getConnectorWorldPosition(connector: Connector): Offset? = connectorPositions[connector]
}

internal fun graphChild(
    id: Long,
    viewData: IGraphStatViewData,
) = GroupChild.ChildGraph(
    groupItemId = id,
    id = id,
    graph = CalculatedGraphViewData(
        time = 0L,
        viewData = viewData,
    )
)

internal fun pieChartViewData(
    id: Long,
    name: String,
    segments: List<IPieChartViewData.Segment>,
) = object : IPieChartViewData {
    override val state = IGraphStatViewData.State.READY
    override val graphOrStat = graphOrStat(id, name, GraphStatType.PIE_CHART)
    override val segments = segments
}

internal fun lastValueViewData(
    id: Long,
    name: String,
    dataPoint: DataPoint,
) = object : ILastValueViewData {
    override val state = IGraphStatViewData.State.READY
    override val graphOrStat = graphOrStat(id, name, GraphStatType.LAST_VALUE)
    override val isDuration = false
    override val lastDataPoint = dataPoint
}

internal fun timeHistogramViewData(
    id: Long,
    name: String,
    barValues: List<ITimeHistogramViewData.BarValue>,
) = object : ITimeHistogramViewData {
    override val state = IGraphStatViewData.State.READY
    override val graphOrStat = graphOrStat(id, name, GraphStatType.TIME_HISTOGRAM)
    override val window = TimeHistogramWindowData.getWindowData(TimeHistogramWindow.WEEK)
    override val barValues = barValues
    override val maxDisplayHeight = 100.0
}
