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

package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.dto.CheckedDays
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.FunctionCreateRequest
import com.samco.trackandgraph.data.database.dto.GraphEndDate
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.GroupChildOrderData
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.database.dto.LastValueStat
import com.samco.trackandgraph.data.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.data.database.dto.LineGraphFeature
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.PieChart
import com.samco.trackandgraph.data.database.dto.IntervalPeriodPair
import com.samco.trackandgraph.data.database.dto.MonthDayOccurrence
import com.samco.trackandgraph.data.database.dto.MonthDayType
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.ReminderParams
import com.samco.trackandgraph.data.database.dto.TimeHistogram
import com.samco.trackandgraph.data.database.dto.Period as ReminderPeriod
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.interactor.DataInteractor
import kotlinx.serialization.json.Json
import org.threeten.bp.Duration
import org.threeten.bp.LocalDateTime
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period

private val json = Json { ignoreUnknownKeys = true }

suspend fun createScreenshotsGroup(dataInteractor: DataInteractor) {
    val outerGroupId = dataInteractor.insertGroup(createGroup("Screenshots"))
    createGroupListForScreenshots(dataInteractor, outerGroupId)
    val exerciseFeatureId = createDailyGroup(dataInteractor, outerGroupId)
    createExerciseGroup(dataInteractor, outerGroupId)
    createRestDaysGroup(dataInteractor, outerGroupId)
    createFunctionsGroup(dataInteractor, outerGroupId)
    createReminders(dataInteractor, exerciseFeatureId)
}

private suspend fun createReminders(dataInteractor: DataInteractor, exerciseFeatureId: Long) {
    // Week Day Reminder
    dataInteractor.insertReminder(
        Reminder(
            id = 1L,
            displayIndex = 0,
            reminderName = "Tracking dailies",
            groupId = null,
            featureId = null,
            params = ReminderParams.WeekDayParams(
                time = LocalTime.of(22, 0),
                checkedDays = CheckedDays(
                    monday = true,
                    tuesday = true,
                    wednesday = true,
                    thursday = true,
                    friday = true,
                    saturday = true,
                    sunday = true
                )
            )
        )
    )

    // Periodic Reminder
    dataInteractor.insertReminder(
        Reminder(
            id = 2L,
            displayIndex = 1,
            reminderName = "Weekly review",
            groupId = null,
            featureId = null,
            params = ReminderParams.PeriodicParams(
                starts = LocalDateTime.now().withHour(10).withMinute(0),
                ends = null,
                interval = 1,
                period = ReminderPeriod.WEEKS
            )
        )
    )

    // Month Day Reminder
    dataInteractor.insertReminder(
        Reminder(
            id = 3L,
            displayIndex = 2,
            reminderName = "Monthly goals",
            groupId = null,
            featureId = null,
            params = ReminderParams.MonthDayParams(
                time = LocalTime.of(9, 0),
                occurrence = MonthDayOccurrence.FIRST,
                dayType = MonthDayType.MONDAY,
                ends = null
            )
        )
    )

    // Time Since Last Reminder
    dataInteractor.insertReminder(
        Reminder(
            id = 4L,
            displayIndex = 3,
            reminderName = "2 days without Exercise",
            groupId = null,
            featureId = exerciseFeatureId,
            params = ReminderParams.TimeSinceLastParams(
                firstInterval = IntervalPeriodPair(
                    interval = 2,
                    period = ReminderPeriod.DAYS
                ),
                secondInterval = IntervalPeriodPair(
                    interval = 1,
                    period = ReminderPeriod.DAYS
                )
            )
        )
    )
}

private suspend fun createRestDaysGroup(dataInteractor: DataInteractor, parent: Long) {
    val stressTracker = dataInteractor.createTracker(createTrackerRequest("Stress", parent))
    val stressFeatureId = dataInteractor.getTrackerById(stressTracker)!!.featureId

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = stressTracker,
        sinTransform = SinTransform(3.0, 21.0, -1.0, 8.0),
        randomOffsetScalar = 1.0,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1),
        roundToInt = true,
        clampMin = 0.0,
        clampMax = 3.0,
        labels = listOf("None", "Low", "Medium", "High")
    )

    val dayOffTracker = dataInteractor.createTracker(createTrackerRequest("Day off", parent))
    val dayOffFeatureId = dataInteractor.getTrackerById(dayOffTracker)!!.featureId

    dataInteractor.insertDataPoint(
        createDataPoint(
            timestamp = OffsetDateTime.now().minusDays(2).minusHours(2).minusMinutes(43),
            featureId = dayOffFeatureId,
            value = 1.0,
        )
    )

    val restDayStatsGroupId = dataInteractor.insertGroup(
        createGroup(name = "Rest day statistics", parentGroupId = parent)
    )

    val pieChartId = createStressPieChart(dataInteractor, stressFeatureId, restDayStatsGroupId)
    val timeSinceId = createTimeSinceDayOff(dataInteractor, dayOffFeatureId, restDayStatsGroupId)
    val histogramId = createStressfulDaysHistogram(dataInteractor, stressFeatureId, restDayStatsGroupId)

    // Reorder graphs to correct order
    dataInteractor.updateGroupChildOrder(
        restDayStatsGroupId,
        listOf(
            GroupChildOrderData(GroupChildType.GRAPH, pieChartId, 0),
            GroupChildOrderData(GroupChildType.GRAPH, timeSinceId, 1),
            GroupChildOrderData(GroupChildType.GRAPH, histogramId, 2),
        )
    )
}

private suspend fun createStressfulDaysHistogram(
    dataInteractor: DataInteractor,
    stressFeatureId: Long,
    parent: Long
): Long {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Most stressful days",
        type = GraphStatType.TIME_HISTOGRAM,
        displayIndex = 0,
    )

    val timeHistogram = TimeHistogram(
        id = 0L,
        graphStatId = 0L,
        featureId = stressFeatureId,
        sampleSize = null,
        window = TimeHistogramWindow.WEEK,
        sumByCount = false,
        endDate = GraphEndDate.Latest
    )

    return dataInteractor.insertTimeHistogram(graphStat, timeHistogram)
}

private suspend fun createTimeSinceDayOff(
    dataInteractor: DataInteractor,
    dayOffFeatureId: Long,
    parent: Long
): Long {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Time since taking a day off",
        type = GraphStatType.LAST_VALUE,
        displayIndex = 1,
    )

    val lastValueStat = LastValueStat(
        id = 0L,
        graphStatId = 0L,
        featureId = dayOffFeatureId,
        endDate = GraphEndDate.Latest,
        fromValue = 0.0,
        toValue = 1.0,
        labels = emptyList(),
        filterByRange = false,
        filterByLabels = false
    )

    return dataInteractor.insertLastValueStat(graphStat, lastValueStat)
}

private suspend fun createStressPieChart(
    dataInteractor: DataInteractor,
    stressFeatureId: Long,
    parent: Long
): Long {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Stress pie chart",
        type = GraphStatType.PIE_CHART,
        displayIndex = 0,
    )

    val pieChart = PieChart(
        id = 0L,
        graphStatId = 0L,
        featureId = stressFeatureId,
        sampleSize = null,
        endDate = GraphEndDate.Latest,
        sumByCount = true
    )

    return dataInteractor.insertPieChart(graphStat, pieChart)
}

private suspend fun createGroupListForScreenshots(dataInteractor: DataInteractor, parent: Long) {
    val groupListGroup = dataInteractor.insertGroup(
        createGroup(
            name = "Track & Graph                              Groups list",
            parentGroupId = parent
        )
    )

    val mealTimeId = dataInteractor.insertGroup(
        createGroup(
            name = "Meal time tracking",
            parentGroupId = groupListGroup,
            colorIndex = 11
        )
    )
    val morningId = dataInteractor.insertGroup(
        createGroup(
            name = "Morning tracking",
            parentGroupId = groupListGroup,
            colorIndex = 6
        )
    )
    val dailyId = dataInteractor.insertGroup(
        createGroup(
            name = "Daily tracking",
            parentGroupId = groupListGroup,
            colorIndex = 0
        )
    )
    val weeklyId = dataInteractor.insertGroup(
        createGroup(
            name = "Weekly tracking",
            parentGroupId = groupListGroup,
            colorIndex = 2
        )
    )
    val exerciseRoutineId = dataInteractor.insertGroup(
        createGroup(
            name = "Exercise routine tracking",
            parentGroupId = groupListGroup,
            colorIndex = 8
        )
    )
    val weightLossId = dataInteractor.insertGroup(
        createGroup(
            name = "Weight loss graphs",
            parentGroupId = groupListGroup,
            colorIndex = 7
        )
    )
    val moodQualityId = dataInteractor.insertGroup(
        createGroup(
            name = "Mood quality",
            parentGroupId = groupListGroup,
            colorIndex = 3
        )
    )
    val stressRestId = dataInteractor.insertGroup(
        createGroup(
            name = "Stress and rest statistics",
            parentGroupId = groupListGroup,
            colorIndex = 4
        )
    )

    // Reorder groups to correct order (original displayIndex 1-8)
    dataInteractor.updateGroupChildOrder(
        groupListGroup,
        listOf(
            GroupChildOrderData(GroupChildType.GROUP, mealTimeId, 1),
            GroupChildOrderData(GroupChildType.GROUP, morningId, 2),
            GroupChildOrderData(GroupChildType.GROUP, dailyId, 3),
            GroupChildOrderData(GroupChildType.GROUP, weeklyId, 4),
            GroupChildOrderData(GroupChildType.GROUP, exerciseRoutineId, 5),
            GroupChildOrderData(GroupChildType.GROUP, weightLossId, 6),
            GroupChildOrderData(GroupChildType.GROUP, moodQualityId, 7),
            GroupChildOrderData(GroupChildType.GROUP, stressRestId, 8),
        )
    )
}

private suspend fun createExerciseGroup(dataInteractor: DataInteractor, parent: Long) {
    val exerciseTracker = dataInteractor.createTracker(
        createTrackerRequest(name = "Exercise", groupId = parent)
    )
    val exerciseFeatureId = dataInteractor.getTrackerById(exerciseTracker)!!.featureId

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = exerciseTracker,
        sinTransform = SinTransform(amplitude = 1.0, wavelength = 5.0, yOffset = -1.0),
        randomOffsetScalar = 1.0,
        roundToInt = true,
        clampMin = 0.0,
        clampMax = 1.0,
    )

    val illnessTracker = dataInteractor.createTracker(
        createTrackerRequest(name = "Sick day (weekly)", groupId = parent)
    )
    val illnessFeatureId = dataInteractor.getTrackerById(illnessTracker)!!.featureId

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = illnessTracker,
        sinTransform = SinTransform(
            amplitude = 3.0,
            wavelength = 160.0,
            yOffset = -2.0,
            xOffset = 80.0
        ),
        randomOffsetScalar = 1.0,
        roundToInt = true,
        clampMin = 0.0,
        clampMax = 7.0,
    )

    val exerciseGroupId = dataInteractor.insertGroup(createGroup("Exercise", parentGroupId = parent))

    val graph1Id = createExerciseGraph1(
        dataInteractor = dataInteractor,
        exerciseFeatureId = exerciseFeatureId,
        illnessFeatureId = illnessFeatureId,
        parent = exerciseGroupId
    )
    val graph2Id = createExerciseGraph2(
        dataInteractor = dataInteractor,
        exerciseFeatureId = exerciseFeatureId,
        parent = exerciseGroupId
    )

    // Reorder graphs to correct order
    dataInteractor.updateGroupChildOrder(
        exerciseGroupId,
        listOf(
            GroupChildOrderData(GroupChildType.GRAPH, graph2Id, 0),
            GroupChildOrderData(GroupChildType.GRAPH, graph1Id, 1),
        )
    )
}

private suspend fun createExerciseGraph2(
    dataInteractor: DataInteractor,
    exerciseFeatureId: Long,
    parent: Long
): Long {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Exercise weekly totals in the last 6 months",
        type = GraphStatType.LINE_GRAPH,
        displayIndex = 0,
    )

    val lineGraph = LineGraphWithFeatures(
        id = 0L,
        graphStatId = 0L,
        features = listOf(
            LineGraphFeature(
                id = 0L,
                lineGraphId = 0L,
                featureId = exerciseFeatureId,
                name = "Exercise",
                colorIndex = 0,
                averagingMode = LineGraphAveraginModes.NO_AVERAGING,
                plottingMode = LineGraphPlottingModes.GENERATE_WEEKLY_TOTALS,
                pointStyle = LineGraphPointStyle.CIRCLES_AND_NUMBERS,
                offset = 0.0,
                scale = 1.0,
                durationPlottingMode = DurationPlottingMode.NONE
            )
        ),
        sampleSize = Period.ofMonths(6),
        yRangeType = YRangeType.DYNAMIC,
        yFrom = 0.0,
        yTo = 0.0,
        endDate = GraphEndDate.Latest
    )

    return dataInteractor.insertLineGraph(graphStat, lineGraph)
}

private suspend fun createExerciseGraph1(
    dataInteractor: DataInteractor,
    exerciseFeatureId: Long,
    illnessFeatureId: Long,
    parent: Long
): Long {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Exercise Vs illness moving averages in the last 6 months",
        type = GraphStatType.LINE_GRAPH,
        displayIndex = 1,
    )

    val lineGraph = LineGraphWithFeatures(
        id = 0L,
        graphStatId = 0L,
        features = listOf(
            LineGraphFeature(
                id = 0L,
                lineGraphId = 0L,
                featureId = exerciseFeatureId,
                name = "Weekly",
                colorIndex = 7,
                averagingMode = LineGraphAveraginModes.WEEKLY_MOVING_AVERAGE,
                plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                pointStyle = LineGraphPointStyle.NONE,
                offset = 0.0,
                scale = 1.0,
                durationPlottingMode = DurationPlottingMode.NONE
            ),
            LineGraphFeature(
                id = 0L,
                lineGraphId = 0L,
                featureId = exerciseFeatureId,
                name = "Monthly",
                colorIndex = 0,
                averagingMode = LineGraphAveraginModes.MONTHLY_MOVING_AVERAGE,
                plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                pointStyle = LineGraphPointStyle.NONE,
                offset = 0.0,
                scale = 1.0,
                durationPlottingMode = DurationPlottingMode.NONE
            ),
            LineGraphFeature(
                id = 0L,
                lineGraphId = 0L,
                featureId = exerciseFeatureId,
                name = "Yearly",
                colorIndex = 5,
                averagingMode = LineGraphAveraginModes.YEARLY_MOVING_AVERAGE,
                plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                pointStyle = LineGraphPointStyle.NONE,
                offset = 0.0,
                scale = 1.0,
                durationPlottingMode = DurationPlottingMode.NONE
            ),
            LineGraphFeature(
                id = 0L,
                lineGraphId = 0L,
                featureId = illnessFeatureId,
                name = "Sick day (weekly)",
                colorIndex = 11,
                averagingMode = LineGraphAveraginModes.WEEKLY_MOVING_AVERAGE,
                plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                pointStyle = LineGraphPointStyle.NONE,
                offset = 0.0,
                scale = 0.1,
                durationPlottingMode = DurationPlottingMode.NONE
            ),
        ),
        sampleSize = Period.ofMonths(6),
        yRangeType = YRangeType.DYNAMIC,
        yFrom = 0.0,
        yTo = 0.0,
        endDate = GraphEndDate.Latest
    )

    return dataInteractor.insertLineGraph(graphStat, lineGraph)
}

private suspend fun createDailyGroup(dataInteractor: DataInteractor, parent: Long): Long {
    val dailyGroupId = dataInteractor.insertGroup(createGroup("Daily", parentGroupId = parent))
    val sleepFeatureId = createSleepTracker(dataInteractor, dailyGroupId)
    val productivityFeatureId = createProductivityTracker(dataInteractor, dailyGroupId)
    val alcoholFeatureId = createAlcoholTracker(dataInteractor, dailyGroupId)
    val meditationFeatureId = createMeditationTracker(dataInteractor, dailyGroupId)
    val workFeatureId = createWorkTracker(dataInteractor, dailyGroupId)
    val weightFeatureId = createWeightTracker(dataInteractor, dailyGroupId)
    val exerciseFeatureId = createExerciseTracker(dataInteractor, dailyGroupId)
    val studyingFeatureId = createStudyingTracker(dataInteractor, dailyGroupId)
    val stressFeatureId = createStressTracker(dataInteractor, dailyGroupId)

    // Reorder trackers to correct order
    dataInteractor.updateGroupChildOrder(
        dailyGroupId,
        listOf(
            GroupChildOrderData(GroupChildType.FEATURE, sleepFeatureId, 0),
            GroupChildOrderData(GroupChildType.FEATURE, productivityFeatureId, 1),
            GroupChildOrderData(GroupChildType.FEATURE, alcoholFeatureId, 2),
            GroupChildOrderData(GroupChildType.FEATURE, meditationFeatureId, 3),
            GroupChildOrderData(GroupChildType.FEATURE, workFeatureId, 4),
            GroupChildOrderData(GroupChildType.FEATURE, weightFeatureId, 5),
            GroupChildOrderData(GroupChildType.FEATURE, exerciseFeatureId, 6),
            GroupChildOrderData(GroupChildType.FEATURE, studyingFeatureId, 7),
            GroupChildOrderData(GroupChildType.FEATURE, stressFeatureId, 8),
        )
    )

    return exerciseFeatureId
}

private suspend fun createStressTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val stressTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Stress",
            description = "Just random data",
            groupId = dailyGroupId,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = stressTracker,
        sinTransform = SinTransform(3.0, 50.0, -1.0),
        randomOffsetScalar = 0.0,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        numDataPoints = 50,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1),
        roundToInt = true,
        clampMin = 0.0,
        clampMax = 3.0,
        labels = listOf("None", "Low", "Medium", "High")
    )

    return dataInteractor.getTrackerById(stressTracker)!!.featureId
}

private suspend fun createStudyingTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val studyingTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Studying",
            description = "Just random data",
            groupId = dailyGroupId,
            dataType = DataType.DURATION,
            suggestionType = TrackerSuggestionType.NONE,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = studyingTracker,
        sinTransform = SinTransform(1.0, 50.0, 6.0),
        randomOffsetScalar = 0.5,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        numDataPoints = 1,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1)
    )

    return dataInteractor.getTrackerById(studyingTracker)!!.featureId
}

private suspend fun createExerciseTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val exerciseTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Exercise",
            description = "Just random data",
            groupId = dailyGroupId,
            hasDefaultValue = true,
            suggestionType = TrackerSuggestionType.NONE,
            defaultValue = 1.0,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = exerciseTracker,
        sinTransform = SinTransform(1.0, 50.0, 6.0),
        randomOffsetScalar = 0.5,
        numDataPoints = 1,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1)
    )

    return dataInteractor.getTrackerById(exerciseTracker)!!.featureId
}

private suspend fun createWeightTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val weightTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Weight",
            description = "Just random data",
            suggestionType = TrackerSuggestionType.NONE,
            groupId = dailyGroupId,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = weightTracker,
        sinTransform = SinTransform(1.0, 50.0, 6.0),
        randomOffsetScalar = 0.5,
        numDataPoints = 1,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1)
    )

    return dataInteractor.getTrackerById(weightTracker)!!.featureId
}

private suspend fun createWorkTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val workTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Work",
            description = "Just random data",
            groupId = dailyGroupId,
            dataType = DataType.DURATION,
            suggestionType = TrackerSuggestionType.NONE,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = workTracker,
        sinTransform = SinTransform(1.0, 50.0, 6.0),
        randomOffsetScalar = 0.5,
        spacing = Duration.ofDays(1),
        numDataPoints = 1,
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1)
    )

    return dataInteractor.getTrackerById(workTracker)!!.featureId
}

private suspend fun createMeditationTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val meditationTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Meditation",
            description = "Just random data",
            groupId = dailyGroupId,
            dataType = DataType.DURATION,
            suggestionType = TrackerSuggestionType.NONE,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = meditationTracker,
        sinTransform = SinTransform(1.0, 50.0, 6.0),
        randomOffsetScalar = 0.5,
        numDataPoints = 1,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1)
    )

    return dataInteractor.getTrackerById(meditationTracker)!!.featureId
}

private suspend fun createAlcoholTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val alcoholTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Alcohol",
            groupId = dailyGroupId,
            description = "Just random data",
            suggestionType = TrackerSuggestionType.NONE,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = alcoholTracker,
        sinTransform = SinTransform(1.0, 50.0, 6.0),
        randomOffsetScalar = 0.5,
        numDataPoints = 1,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1)
    )

    return dataInteractor.getTrackerById(alcoholTracker)!!.featureId
}

private suspend fun createProductivityTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val productivityTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Productivity",
            description = "Just random data",
            groupId = dailyGroupId,
            suggestionType = TrackerSuggestionType.NONE,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = productivityTracker,
        sinTransform = SinTransform(1.0, 50.0, 6.0),
        randomOffsetScalar = 0.5,
        numDataPoints = 1,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1)
    )

    return dataInteractor.getTrackerById(productivityTracker)!!.featureId
}

private suspend fun createSleepTracker(dataInteractor: DataInteractor, dailyGroupId: Long): Long {
    val sleepTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Sleep",
            groupId = dailyGroupId,
            dataType = DataType.DURATION,
            suggestionType = TrackerSuggestionType.NONE,
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = sleepTracker,
        sinTransform = SinTransform(SEC_PER_HOUR * 1.5, 50.0, SEC_PER_HOUR * 6.0),
        randomOffsetScalar = 0.5,
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().withHour(22).minusDays(1)
    )

    return dataInteractor.getTrackerById(sleepTracker)!!.featureId
}

private suspend fun createFunctionsGroup(dataInteractor: DataInteractor, parent: Long) {
    val groupId = dataInteractor.insertGroup(createGroup("Functions", parentGroupId = parent))

    val runningTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Running ",
            groupId = groupId,
            dataType = DataType.DURATION,
            hasDefaultValue = false,
            suggestionType = TrackerSuggestionType.LABEL_ONLY,
            suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
        )
    )
    val runningFeatureId = dataInteractor.getTrackerById(runningTrackerId)!!.featureId

    val cyclingTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Cycling ",
            groupId = groupId,
            dataType = DataType.DURATION,
            hasDefaultValue = false,
            suggestionType = TrackerSuggestionType.LABEL_ONLY,
            suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
        )
    )
    val cyclingFeatureId = dataInteractor.getTrackerById(cyclingTrackerId)!!.featureId

    // Function: Exercise - combines Running and Cycling
    val exerciseFunctionId = dataInteractor.insertFunction(
        FunctionCreateRequest(
            name = "Exercise",
            groupId = groupId,
            description = "",
            functionGraph = json.decodeFromString(
                exercise_function_graph(runningFeatureId, cyclingFeatureId)
            ),
            inputFeatureIds = listOf(runningFeatureId, cyclingFeatureId)
        )
    )!!

    // Reorder to correct order (original: Exercise=0, Running=5, Cycling=7)
    dataInteractor.updateGroupChildOrder(
        groupId,
        listOf(
            GroupChildOrderData(GroupChildType.FEATURE, exerciseFunctionId, 0),
            GroupChildOrderData(GroupChildType.FEATURE, runningFeatureId, 5),
            GroupChildOrderData(GroupChildType.FEATURE, cyclingFeatureId, 7),
        )
    )
}