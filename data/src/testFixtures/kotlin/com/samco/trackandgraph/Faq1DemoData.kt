package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.model.DataInteractor
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime


suspend fun createFaq1Group(dataInteractor: DataInteractor) {
    val outerGroupId = dataInteractor.insertGroup(createGroup("FAQ 1"))

    createExerciseGroup(dataInteractor, outerGroupId)
    createCaloriesAndExerciseGroup(dataInteractor, outerGroupId)
    createDailyGroup(dataInteractor, outerGroupId)
}

private suspend fun createDailyGroup(dataInteractor: DataInteractor, parent: Long) {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Daily", parentGroupId = parent)
    )
    createSleepTracker(dataInteractor, group)
    createProductivityTracker(dataInteractor, group)
    createWeatherTracker(dataInteractor, group)
    createSickDayTracker(dataInteractor, group)
    createWorkTracker(dataInteractor, group)
    createStressTracker(dataInteractor, group)
}

private suspend fun createStressTracker(dataInteractor: DataInteractor, parent: Long) {
    val tracker = dataInteractor.insertTracker(
        createTracker(
            name = "Stress",
            groupId = parent,
            displayIndex = 5,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )
    val featureId = dataInteractor.getTrackerById(tracker)!!.featureId

    listOf("None", "Low", "Medium", "High").forEachIndexed { index, value ->
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = OffsetDateTime.now().minusDays(1).minusSeconds(index.toLong()),
                featureId = featureId,
                value = index.toDouble(),
                label = value
            )
        )
    }
}

private suspend fun createWorkTracker(dataInteractor: DataInteractor, parent: Long) {
    val tracker = dataInteractor.insertTracker(
        createTracker(
            name = "Work",
            groupId = parent,
            displayIndex = 4,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )
    val featureId = dataInteractor.getTrackerById(tracker)!!.featureId

    listOf("None", "Low", "Medium", "High").forEachIndexed { index, value ->
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = OffsetDateTime.now().minusDays(1).minusSeconds(index.toLong()),
                featureId = featureId,
                value = index.toDouble(),
                label = value
            )
        )
    }
}

private suspend fun createSickDayTracker(dataInteractor: DataInteractor, parent: Long) {
    val tracker = dataInteractor.insertTracker(
        createTracker(
            name = "Sick day",
            groupId = parent,
            displayIndex = 3,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )
    val featureId = dataInteractor.getTrackerById(tracker)!!.featureId

    listOf("None", "Low", "Medium", "High").forEachIndexed { index, value ->
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = OffsetDateTime.now().minusDays(1).minusSeconds(index.toLong()),
                featureId = featureId,
                value = index.toDouble(),
                label = value
            )
        )
    }
}

private suspend fun createWeatherTracker(dataInteractor: DataInteractor, parent: Long) {
    val tracker = dataInteractor.insertTracker(
        createTracker(
            name = "Weather",
            groupId = parent,
            displayIndex = 2,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )
    val featureId = dataInteractor.getTrackerById(tracker)!!.featureId

    listOf("Very poor", "Poor", "Ok", "Good", "Very good").forEachIndexed { index, value ->
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = OffsetDateTime.now().minusDays(1).minusSeconds(index.toLong()),
                featureId = featureId,
                value = index.toDouble(),
                label = value
            )
        )
    }
}

private suspend fun createProductivityTracker(dataInteractor: DataInteractor, parent: Long) {
    val tracker = dataInteractor.insertTracker(
        createTracker(
            name = "Productivity",
            groupId = parent,
            displayIndex = 1,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = tracker,
        sinTransform = SinTransform(3.0, 50.0, -1.0),
        randomOffsetScalar = 1.0,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 4,
        endPoint = OffsetDateTime.now().minusDays(1),
        roundToInt = true,
        clampMin = 0.0,
        clampMax = 3.0,
        labels = listOf("None", "Low", "Medium", "High")
    )
}

private suspend fun createSleepTracker(dataInteractor: DataInteractor, parent: Long) {
    dataInteractor.insertTracker(
        createTracker(
            name = "Sleep",
            groupId = parent,
            dataType = DataType.DURATION,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
            displayIndex = 0
        )
    )
}

private suspend fun createCaloriesAndExerciseGroup(dataInteractor: DataInteractor, parent: Long) {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Track & Graph                  Calories & Exercise", parentGroupId = parent)
    )

    val calories = dataInteractor.insertTracker(
        createTracker(
            name = "Calories",
            groupId = group,
            description = "I track the number of calories I eat each day."
        )
    )
    val caloriesFeatureId = dataInteractor.getTrackerById(calories)!!.featureId

    dataInteractor.insertDataPoint(
        createDataPoint(
            timestamp = OffsetDateTime.now().minusDays(1),
            featureId = caloriesFeatureId,
            value = 2000.0
        )
    )

    dataInteractor.insertTracker(
        createTracker(
            name = "Exercise",
            groupId = group,
            description = "I track a value of 1 every time I exercise."
        )
    )
}

private suspend fun createExerciseGroup(dataInteractor: DataInteractor, parent: Long) {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Track & Graph                  Exercise", parentGroupId = parent)
    )

    dataInteractor.insertTracker(
        createTracker(
            name = "Exercise",
            groupId = group,
            description = "I track a value of 1 every time I exercise."
        )
    )
}