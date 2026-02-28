package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.interactor.DataInteractor
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

suspend fun createFaq1Group(dataInteractor: DataInteractor) {
    val outerGroupId = dataInteractor.insertGroup(createGroup("FAQ 1"))

    val exerciseGroupId = createExerciseGroup(dataInteractor, outerGroupId)
    val caloriesGroupId = createCaloriesAndExerciseGroup(dataInteractor, outerGroupId)
    val dailyGroupId = createDailyGroup(dataInteractor, outerGroupId)

    // Reorder groups to correct order
    dataInteractor.updateGroupChildOrder(
        outerGroupId,
        listOf(
            GroupChildDisplayIndex(GroupChildType.GROUP, exerciseGroupId, 0),
            GroupChildDisplayIndex(GroupChildType.GROUP, caloriesGroupId, 1),
            GroupChildDisplayIndex(GroupChildType.GROUP, dailyGroupId, 2),
        )
    )
}

private suspend fun createDailyGroup(dataInteractor: DataInteractor, parent: Long): Long {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Daily", parentGroupId = parent)
    )
    val sleepTrackerId = createSleepTracker(dataInteractor, group)
    val productivityTrackerId = createProductivityTracker(dataInteractor, group)
    val weatherTrackerId = createWeatherTracker(dataInteractor, group)
    val sickDayTrackerId = createSickDayTracker(dataInteractor, group)
    val workTrackerId = createWorkTracker(dataInteractor, group)
    val stressTrackerId = createStressTracker(dataInteractor, group)

    // Reorder trackers to correct order
    dataInteractor.updateGroupChildOrder(
        group,
        listOf(
            GroupChildDisplayIndex(GroupChildType.TRACKER, sleepTrackerId, 0),
            GroupChildDisplayIndex(GroupChildType.TRACKER, productivityTrackerId, 1),
            GroupChildDisplayIndex(GroupChildType.TRACKER, weatherTrackerId, 2),
            GroupChildDisplayIndex(GroupChildType.TRACKER, sickDayTrackerId, 3),
            GroupChildDisplayIndex(GroupChildType.TRACKER, workTrackerId, 4),
            GroupChildDisplayIndex(GroupChildType.TRACKER, stressTrackerId, 5),
        )
    )

    return group
}

private suspend fun createStressTracker(dataInteractor: DataInteractor, parent: Long): Long {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Stress",
            groupId = parent,
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

    return tracker
}

private suspend fun createWorkTracker(dataInteractor: DataInteractor, parent: Long): Long {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Work",
            groupId = parent,
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

    return tracker
}

private suspend fun createSickDayTracker(dataInteractor: DataInteractor, parent: Long): Long {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Sick day",
            groupId = parent,
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

    return tracker
}

private suspend fun createWeatherTracker(dataInteractor: DataInteractor, parent: Long): Long {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Weather",
            groupId = parent,
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

    return tracker
}

private suspend fun createProductivityTracker(dataInteractor: DataInteractor, parent: Long): Long {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Productivity",
            groupId = parent,
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

    return tracker
}

private suspend fun createSleepTracker(dataInteractor: DataInteractor, parent: Long): Long {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Sleep",
            groupId = parent,
            dataType = DataType.DURATION,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
        )
    )
    return tracker
}

private suspend fun createCaloriesAndExerciseGroup(dataInteractor: DataInteractor, parent: Long): Long {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Track & Graph                  Calories & Exercise", parentGroupId = parent)
    )

    val caloriesTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Calories",
            groupId = group,
            description = "I track the number of calories I eat each day."
        )
    )
    val caloriesFeatureId = dataInteractor.getTrackerById(caloriesTrackerId)!!.featureId

    dataInteractor.insertDataPoint(
        createDataPoint(
            timestamp = OffsetDateTime.now().minusDays(1),
            featureId = caloriesFeatureId,
            value = 2000.0
        )
    )

    val exerciseTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Exercise",
            groupId = group,
            description = "I track a value of 1 every time I exercise."
        )
    )
    val exerciseFeatureId = dataInteractor.getTrackerById(exerciseTrackerId)!!.featureId

    // Reorder trackers to correct order
    dataInteractor.updateGroupChildOrder(
        group,
        listOf(
            GroupChildDisplayIndex(GroupChildType.TRACKER, caloriesTrackerId, 0),
            GroupChildDisplayIndex(GroupChildType.TRACKER, exerciseTrackerId, 1),
        )
    )

    return group
}

private suspend fun createExerciseGroup(dataInteractor: DataInteractor, parent: Long): Long {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Track & Graph                  Exercise", parentGroupId = parent)
    )

    dataInteractor.createTracker(
        createTrackerRequest(
            name = "Exercise",
            groupId = group,
            description = "I track a value of 1 every time I exercise."
        )
    )

    return group
}