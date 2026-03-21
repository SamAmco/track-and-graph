package com.samco.trackandgraph

import com.samco.trackandgraph.data.database.dto.CreatedComponent
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.dto.GroupChildDisplayIndex
import com.samco.trackandgraph.data.database.dto.GroupChildType
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.interactor.DataInteractor
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime

suspend fun createFaq1Group(dataInteractor: DataInteractor) {
    val outerGroup = dataInteractor.insertGroup(createGroup("FAQ 1"))

    val exerciseGroup = createExerciseGroup(dataInteractor, outerGroup.componentId)
    val caloriesGroup = createCaloriesAndExerciseGroup(dataInteractor, outerGroup.componentId)
    val dailyGroup = createDailyGroup(dataInteractor, outerGroup.componentId)

    // Reorder groups to correct order
    dataInteractor.updateGroupChildOrder(
        outerGroup.componentId,
        listOf(
            GroupChildDisplayIndex(exerciseGroup.groupItemId, GroupChildType.GROUP, exerciseGroup.componentId, 0),
            GroupChildDisplayIndex(caloriesGroup.groupItemId, GroupChildType.GROUP, caloriesGroup.componentId, 1),
            GroupChildDisplayIndex(dailyGroup.groupItemId, GroupChildType.GROUP, dailyGroup.componentId, 2),
        )
    )
}

private suspend fun createDailyGroup(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Daily", parentGroupId = parent)
    )
    val sleepTracker = createSleepTracker(dataInteractor, group.componentId)
    val productivityTracker = createProductivityTracker(dataInteractor, group.componentId)
    val weatherTracker = createWeatherTracker(dataInteractor, group.componentId)
    val sickDayTracker = createSickDayTracker(dataInteractor, group.componentId)
    val workTracker = createWorkTracker(dataInteractor, group.componentId)
    val stressTracker = createStressTracker(dataInteractor, group.componentId)

    // Reorder trackers to correct order
    dataInteractor.updateGroupChildOrder(
        group.componentId,
        listOf(
            GroupChildDisplayIndex(sleepTracker.groupItemId, GroupChildType.TRACKER, sleepTracker.componentId, 0),
            GroupChildDisplayIndex(productivityTracker.groupItemId, GroupChildType.TRACKER, productivityTracker.componentId, 1),
            GroupChildDisplayIndex(weatherTracker.groupItemId, GroupChildType.TRACKER, weatherTracker.componentId, 2),
            GroupChildDisplayIndex(sickDayTracker.groupItemId, GroupChildType.TRACKER, sickDayTracker.componentId, 3),
            GroupChildDisplayIndex(workTracker.groupItemId, GroupChildType.TRACKER, workTracker.componentId, 4),
            GroupChildDisplayIndex(stressTracker.groupItemId, GroupChildType.TRACKER, stressTracker.componentId, 5),
        )
    )

    return group
}

private suspend fun createStressTracker(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Stress",
            groupId = parent,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )
    val featureId = dataInteractor.getTrackerById(tracker.componentId)!!.featureId

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

private suspend fun createWorkTracker(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Work",
            groupId = parent,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )
    val featureId = dataInteractor.getTrackerById(tracker.componentId)!!.featureId

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

private suspend fun createSickDayTracker(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Sick day",
            groupId = parent,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )
    val featureId = dataInteractor.getTrackerById(tracker.componentId)!!.featureId

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

private suspend fun createWeatherTracker(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Weather",
            groupId = parent,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )
    val featureId = dataInteractor.getTrackerById(tracker.componentId)!!.featureId

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

private suspend fun createProductivityTracker(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
    val tracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Productivity",
            groupId = parent,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    )

    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = tracker.componentId,
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

private suspend fun createSleepTracker(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
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

private suspend fun createCaloriesAndExerciseGroup(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Track & Graph                  Calories & Exercise", parentGroupId = parent)
    )

    val caloriesTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Calories",
            groupId = group.componentId,
            description = "I track the number of calories I eat each day."
        )
    )
    val caloriesFeatureId = dataInteractor.getTrackerById(caloriesTracker.componentId)!!.featureId

    dataInteractor.insertDataPoint(
        createDataPoint(
            timestamp = OffsetDateTime.now().minusDays(1),
            featureId = caloriesFeatureId,
            value = 2000.0
        )
    )

    val exerciseTracker = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Exercise",
            groupId = group.componentId,
            description = "I track a value of 1 every time I exercise."
        )
    )

    // Reorder trackers to correct order
    dataInteractor.updateGroupChildOrder(
        group.componentId,
        listOf(
            GroupChildDisplayIndex(caloriesTracker.groupItemId, GroupChildType.TRACKER, caloriesTracker.componentId, 0),
            GroupChildDisplayIndex(exerciseTracker.groupItemId, GroupChildType.TRACKER, exerciseTracker.componentId, 1),
        )
    )

    return group
}

private suspend fun createExerciseGroup(dataInteractor: DataInteractor, parent: Long): CreatedComponent {
    val group = dataInteractor.insertGroup(
        createGroup(name = "Track & Graph                  Exercise", parentGroupId = parent)
    )

    dataInteractor.createTracker(
        createTrackerRequest(
            name = "Exercise",
            groupId = group.componentId,
            description = "I track a value of 1 every time I exercise."
        )
    )

    return group
}
