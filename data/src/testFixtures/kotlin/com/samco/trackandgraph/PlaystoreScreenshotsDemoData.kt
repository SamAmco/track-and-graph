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
import com.samco.trackandgraph.data.database.dto.Function
import com.samco.trackandgraph.data.database.dto.GraphEndDate
import com.samco.trackandgraph.data.database.dto.GraphOrStat
import com.samco.trackandgraph.data.database.dto.GraphStatType
import com.samco.trackandgraph.data.database.dto.LastValueStat
import com.samco.trackandgraph.data.database.dto.LineGraphAveraginModes
import com.samco.trackandgraph.data.database.dto.LineGraphFeature
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.LineGraphWithFeatures
import com.samco.trackandgraph.data.database.dto.PieChart
import com.samco.trackandgraph.data.database.dto.Reminder
import com.samco.trackandgraph.data.database.dto.TimeHistogram
import com.samco.trackandgraph.data.database.dto.TimeHistogramWindow
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionType
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.interactor.DataInteractor
import kotlinx.serialization.json.Json
import org.threeten.bp.Duration
import org.threeten.bp.Instant
import org.threeten.bp.LocalTime
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period
import org.threeten.bp.ZoneOffset

suspend fun createScreenshotsGroup(dataInteractor: DataInteractor) {
    val outerGroupId = dataInteractor.insertGroup(createGroup("Screenshots"))
    createGroupListForScreenshots(dataInteractor, outerGroupId)
    createDailyGroup(dataInteractor, outerGroupId)
    createExerciseGroup(dataInteractor, outerGroupId)
    createRestDaysGroup(dataInteractor, outerGroupId)
    createFunctionsGroup(dataInteractor, outerGroupId)
    createReminders(dataInteractor)
}

private suspend fun createReminders(dataInteractor: DataInteractor) {
    dataInteractor.updateReminders(
        listOf(
            Reminder(
                1L,
                0,
                "Weight",
                time = LocalTime.of(17, 0),
                checkedDays = CheckedDays(
                    monday = false,
                    tuesday = false,
                    wednesday = true,
                    thursday = false,
                    friday = false,
                    saturday = false,
                    sunday = true
                )
            ),
            Reminder(
                2L,
                1,
                "Tracking dailies",
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
}

private suspend fun createRestDaysGroup(dataInteractor: DataInteractor, parent: Long) {
    val stressTracker = dataInteractor.insertTracker(createTracker("Stress", parent))
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

    val dayOffTracker = dataInteractor.insertTracker(createTracker("Day off", parent))
    val dayOffFeatureId = dataInteractor.getTrackerById(dayOffTracker)!!.featureId

    dataInteractor.insertDataPoint(
        createDataPoint(
            timestamp = OffsetDateTime.now().minusDays(2).minusHours(2).minusMinutes(43),
            featureId = dayOffFeatureId,
            value = 1.0,
        )
    )

    dataInteractor.insertGroup(
        createGroup(name = "Rest day statistics", parentGroupId = parent)
    ).let {
        createStressPieChart(dataInteractor, stressFeatureId, it)
        createTimeSinceDayOff(dataInteractor, dayOffFeatureId, it)
        createStressfulDaysHistogram(dataInteractor, stressFeatureId, it)
    }
}

private suspend fun createStressfulDaysHistogram(
    dataInteractor: DataInteractor,
    stressFeatureId: Long,
    parent: Long
) {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Most stressful days",
        type = GraphStatType.TIME_HISTOGRAM,
        displayIndex = 2
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

    dataInteractor.insertTimeHistogram(graphStat, timeHistogram)
}

private suspend fun createTimeSinceDayOff(
    dataInteractor: DataInteractor,
    dayOffFeatureId: Long,
    parent: Long
) {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Time since taking a day off",
        type = GraphStatType.LAST_VALUE,
        displayIndex = 1
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

    dataInteractor.insertLastValueStat(graphStat, lastValueStat)
}

private suspend fun createStressPieChart(
    dataInteractor: DataInteractor,
    stressFeatureId: Long,
    parent: Long
) {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Stress pie chart",
        type = GraphStatType.PIE_CHART,
        displayIndex = 0
    )

    val pieChart = PieChart(
        id = 0L,
        graphStatId = 0L,
        featureId = stressFeatureId,
        sampleSize = null,
        endDate = GraphEndDate.Latest,
        sumByCount = true
    )

    dataInteractor.insertPieChart(graphStat, pieChart)
}

private suspend fun createGroupListForScreenshots(dataInteractor: DataInteractor, parent: Long) {
    val groupListGroup = dataInteractor.insertGroup(
        createGroup(
            name = "Track & Graph                              Groups list",
            parentGroupId = parent
        )
    )

    dataInteractor.insertGroup(
        createGroup(
            name = "Meal time tracking",
            displayIndex = 1,
            parentGroupId = groupListGroup,
            colorIndex = 11
        )
    )
    dataInteractor.insertGroup(
        createGroup(
            name = "Morning tracking",
            displayIndex = 2,
            parentGroupId = groupListGroup,
            colorIndex = 6
        )
    )
    dataInteractor.insertGroup(
        createGroup(
            name = "Daily tracking",
            displayIndex = 3,
            parentGroupId = groupListGroup,
            colorIndex = 0
        )
    )
    dataInteractor.insertGroup(
        createGroup(
            name = "Weekly tracking",
            displayIndex = 4,
            parentGroupId = groupListGroup,
            colorIndex = 2
        )
    )
    dataInteractor.insertGroup(
        createGroup(
            name = "Exercise routine tracking",
            displayIndex = 5,
            parentGroupId = groupListGroup,
            colorIndex = 8
        )
    )
    dataInteractor.insertGroup(
        createGroup(
            name = "Weight loss graphs",
            displayIndex = 6,
            parentGroupId = groupListGroup,
            colorIndex = 7
        )
    )
    dataInteractor.insertGroup(
        createGroup(
            name = "Mood quality",
            displayIndex = 7,
            parentGroupId = groupListGroup,
            colorIndex = 3
        )
    )
    dataInteractor.insertGroup(
        createGroup(
            name = "Stress and rest statistics",
            displayIndex = 8,
            parentGroupId = groupListGroup,
            colorIndex = 4
        )
    )
}

private suspend fun createExerciseGroup(dataInteractor: DataInteractor, parent: Long) {
    val exerciseTracker = dataInteractor.insertTracker(
        createTracker(name = "Exercise", groupId = parent)
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

    val illnessTracker = dataInteractor.insertTracker(
        createTracker(name = "Sick day (weekly)", groupId = parent)
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

    dataInteractor.insertGroup(createGroup("Exercise", parentGroupId = parent)).let {
        createExerciseGraph1(
            dataInteractor = dataInteractor,
            exerciseFeatureId = exerciseFeatureId,
            illnessFeatureId = illnessFeatureId,
            parent = it
        )
        createExerciseGraph2(
            dataInteractor = dataInteractor,
            exerciseFeatureId = exerciseFeatureId,
            parent = it
        )
    }
}

private suspend fun createExerciseGraph2(
    dataInteractor: DataInteractor,
    exerciseFeatureId: Long,
    parent: Long
) {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Exercise weekly totals in the last 6 months",
        type = GraphStatType.LINE_GRAPH,
        displayIndex = 0
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

    dataInteractor.insertLineGraph(graphStat, lineGraph)
}

private suspend fun createExerciseGraph1(
    dataInteractor: DataInteractor,
    exerciseFeatureId: Long,
    illnessFeatureId: Long,
    parent: Long
) {
    val graphStat = GraphOrStat(
        id = 0L,
        groupId = parent,
        name = "Exercise Vs illness moving averages in the last 6 months",
        type = GraphStatType.LINE_GRAPH,
        displayIndex = 1
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

    dataInteractor.insertLineGraph(graphStat, lineGraph)
}

private suspend fun createDailyGroup(dataInteractor: DataInteractor, parent: Long) {
    dataInteractor.insertGroup(createGroup("Daily", parentGroupId = parent)).let {
        createSleepTracker(dataInteractor, it)
        createProductivityTracker(dataInteractor, it)
        createAlcoholTracker(dataInteractor, it)
        createMeditationTracker(dataInteractor, it)
        createWorkTracker(dataInteractor, it)
        createWeightTracker(dataInteractor, it)
        createExerciseTracker(dataInteractor, it)
        createStudyingTracker(dataInteractor, it)
        createStressTracker(dataInteractor, it)
    }
}

private suspend fun createStressTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val stressTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Stress",
            description = "Just random data",
            groupId = dailyGroupId,
            displayIndex = 8
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
}

private suspend fun createStudyingTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val studyingTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Studying",
            description = "Just random data",
            groupId = dailyGroupId,
            dataType = DataType.DURATION,
            suggestionType = TrackerSuggestionType.NONE,
            displayIndex = 7
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
}

private suspend fun createExerciseTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val exerciseTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Exercise",
            description = "Just random data",
            groupId = dailyGroupId,
            hasDefaultValue = true,
            suggestionType = TrackerSuggestionType.NONE,
            defaultValue = 1.0,
            displayIndex = 6
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
}

private suspend fun createWeightTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val weightTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Weight",
            description = "Just random data",
            suggestionType = TrackerSuggestionType.NONE,
            groupId = dailyGroupId,
            displayIndex = 5
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
}

private suspend fun createWorkTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val workTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Work",
            description = "Just random data",
            groupId = dailyGroupId,
            dataType = DataType.DURATION,
            suggestionType = TrackerSuggestionType.NONE,
            displayIndex = 4
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
}

private suspend fun createMeditationTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val meditationTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Meditation",
            description = "Just random data",
            groupId = dailyGroupId,
            dataType = DataType.DURATION,
            suggestionType = TrackerSuggestionType.NONE,
            displayIndex = 3
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
}

private suspend fun createAlcoholTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val alcoholTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Alcohol",
            groupId = dailyGroupId,
            description = "Just random data",
            suggestionType = TrackerSuggestionType.NONE,
            displayIndex = 2
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
}

private suspend fun createProductivityTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val productivityTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Productivity",
            description = "Just random data",
            groupId = dailyGroupId,
            suggestionType = TrackerSuggestionType.NONE,
            displayIndex = 1
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
}

private suspend fun createSleepTracker(dataInteractor: DataInteractor, dailyGroupId: Long) {
    val sleepTracker = dataInteractor.insertTracker(
        createTracker(
            name = "Sleep",
            groupId = dailyGroupId,
            dataType = DataType.DURATION,
            suggestionType = TrackerSuggestionType.NONE,
            displayIndex = 0
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
}

// Function graph JSON strings for Playstore Screenshot group

private fun personal_bests_function_graph(featureId: Long) =
    """{"nodes":[{"type":"FeatureNode","x":-1762.3301,"y":-26.252203,"id":2,"featureId":""" +
            featureId +
            """}],"outputNode":{"x":0.0,"y":0.0,"id":1,"dependencies":[{"connectorIndex":0,"nodeId":2}]},"isDuration":false}"""

private fun personal_best_rpm_function_graph(featureId: Long) =
    """{"nodes":[{"type":"FeatureNode","x":-1364.5275,"y":-67.224884,"id":2,"featureId":""" +
            featureId +
            """}],"outputNode":{"x":0.0,"y":0.0,"id":1,"dependencies":[{"connectorIndex":0,"nodeId":2}]},"isDuration":false}"""

private fun personal_best_weight_jumps_function_graph(featureId: Long) =
    """{"nodes":[{"type":"FeatureNode","x":-1367.9341,"y":56.33628,"id":2,"featureId":""" +
            featureId +
            """}],"outputNode":{"x":-41.805542,"y":2.338745,"id":1,"dependencies":[{"connectorIndex":0,"nodeId":2}]},"isDuration":false}"""

private suspend fun createFunctionsGroup(dataInteractor: DataInteractor, parent: Long) {
    val groupId = dataInteractor.insertGroup(
        createGroup(
            name = "Squats",
            parentGroupId = parent,
            displayIndex = 2
        )
    )

    // Create tracker: Squats 🏋🏼
    val squatsTrackerId = dataInteractor.insertTracker(
        createTracker(
            name = "Squats 🏋🏼",
            groupId = groupId,
            displayIndex = 0,
            dataType = DataType.CONTINUOUS,
            hasDefaultValue = false,
            suggestionType = TrackerSuggestionType.LABEL_ONLY,
            suggestionOrder = TrackerSuggestionOrder.LABEL_ASCENDING
        )
    )
    val squatsFeatureId = dataInteractor.getTrackerById(squatsTrackerId)!!.featureId

    // Insert data points for Squats
    val dataPoints = listOf(
        Triple(1743343444822L, 1.5, ""),
        Triple(1746021888293L, 1.0, ""),
        Triple(1748613899924L, 0.7, ""),
        Triple(1751292323594L, 0.55, ""),
        Triple(1753884333876L, 0.48, ""),
        Triple(1756562746678L, 0.46, ""),
        Triple(1759241155311L, 0.43, ""),
        Triple(1761833185676L, 0.42, "")
    )

    dataPoints.forEach { (epochMilli, value, label) ->
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = OffsetDateTime.ofInstant(
                    Instant.ofEpochMilli(epochMilli),
                    ZoneOffset.UTC
                ),
                featureId = squatsFeatureId,
                value = value,
                label = label
            )
        )
    }

    // Create functions
    val json = Json { ignoreUnknownKeys = true }

    // Function: Personal Bests
    dataInteractor.insertFunction(
        Function(
            name = "Personal Bests",
            groupId = groupId,
            displayIndex = 1,
            description = "",
            functionGraph = json.decodeFromString(personal_bests_function_graph(squatsFeatureId)),
            inputFeatureIds = listOf(squatsFeatureId)
        )
    )

    // Function: Personal Best Rate per Month
    dataInteractor.insertFunction(
        Function(
            name = "Personal Best Rate per Month",
            groupId = groupId,
            displayIndex = 2,
            description = "",
            functionGraph = json.decodeFromString(personal_best_rpm_function_graph(squatsFeatureId)),
            inputFeatureIds = listOf(squatsFeatureId)
        )
    )

    // Function: Personal Best Weight Jumps
    dataInteractor.insertFunction(
        Function(
            name = "Personal Best Weight Jumps",
            groupId = groupId,
            displayIndex = 3,
            description = "",
            functionGraph = json.decodeFromString(
                personal_best_weight_jumps_function_graph(
                    squatsFeatureId
                )
            ),
            inputFeatureIds = listOf(squatsFeatureId)
        )
    )

    // Create line graphs
    createFunctionsScreenshotLineGraphs(dataInteractor, groupId, squatsFeatureId)
}

private suspend fun createFunctionsScreenshotLineGraphs(
    dataInteractor: DataInteractor,
    groupId: Long,
    squatsFeatureId: Long
) {
    // Line Graph 1
    val graphStat1 = GraphOrStat(
        id = 0L,
        groupId = groupId,
        name = "Personal Best Rate Per Month",
        type = GraphStatType.LINE_GRAPH,
        displayIndex = 4
    )

    val lineGraph1 = LineGraphWithFeatures(
        id = 0L,
        graphStatId = 0L,
        features = listOf(
            LineGraphFeature(
                id = 0L,
                lineGraphId = 0L,
                featureId = squatsFeatureId,
                name = "Squats 🏋🏼",
                colorIndex = 0,
                averagingMode = LineGraphAveraginModes.NO_AVERAGING,
                plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                pointStyle = LineGraphPointStyle.CIRCLES_AND_NUMBERS,
                offset = 0.0,
                scale = 1.0,
                durationPlottingMode = DurationPlottingMode.NONE
            )
        ),
        sampleSize = null,
        yRangeType = YRangeType.FIXED,
        yFrom = 0.0,
        yTo = 1.5,
        endDate = GraphEndDate.Latest
    )

    dataInteractor.insertLineGraph(graphStat1, lineGraph1)

    // Line Graph 2
    val graphStat2 = GraphOrStat(
        id = 0L,
        groupId = groupId,
        name = "Personal Best Rate Per Month",
        type = GraphStatType.LINE_GRAPH,
        displayIndex = 5
    )

    val lineGraph2 = LineGraphWithFeatures(
        id = 0L,
        graphStatId = 0L,
        features = listOf(
            LineGraphFeature(
                id = 0L,
                lineGraphId = 0L,
                featureId = squatsFeatureId,
                name = "PB",
                colorIndex = 2,
                averagingMode = LineGraphAveraginModes.NO_AVERAGING,
                plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                pointStyle = LineGraphPointStyle.CIRCLES_AND_NUMBERS,
                offset = 0.0,
                scale = 1.0,
                durationPlottingMode = DurationPlottingMode.NONE
            )
        ),
        sampleSize = null,
        yRangeType = YRangeType.FIXED,
        yFrom = 0.0,
        yTo = 1.5,
        endDate = GraphEndDate.Latest
    )

    dataInteractor.insertLineGraph(graphStat2, lineGraph2)
}
