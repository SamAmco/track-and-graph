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

import com.samco.trackandgraph.data.database.dto.DurationPlottingMode
import com.samco.trackandgraph.data.database.dto.GraphEndDate
import com.samco.trackandgraph.data.database.dto.LineGraphAveragingModes
import com.samco.trackandgraph.data.database.dto.LineGraphConfig
import com.samco.trackandgraph.data.database.dto.LineGraphCreateRequest
import com.samco.trackandgraph.data.database.dto.LineGraphFeatureConfig
import com.samco.trackandgraph.data.database.dto.LineGraphPlottingModes
import com.samco.trackandgraph.data.database.dto.LineGraphPointStyle
import com.samco.trackandgraph.data.database.dto.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.dto.YRangeType
import com.samco.trackandgraph.data.interactor.DataInteractor
import com.samco.trackandgraph.data.sampling.DataSampler
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.Period

private data class ScreenshotGroupData(
    val groupId: Long,
    val relaxationTrackerId: Long,
    val stressTrackerId: Long
)

suspend fun createFirstOpenTutorialGroup(dataInteractor: DataInteractor, dataSampler: DataSampler) {
    val mainGroupId = dataInteractor.insertGroup(createGroup("First open tutorial")).componentId

    // Create the three screenshot groups
    val screenshot1Data = createScreenshot1Group(dataInteractor, mainGroupId)
    val screenshot2Data = createScreenshot2Group(dataInteractor, dataSampler, mainGroupId, screenshot1Data)
    createScreenshot3Group(dataInteractor, dataSampler, mainGroupId, screenshot2Data)
}

private suspend fun createScreenshot1Group(
    dataInteractor: DataInteractor,
    parentGroupId: Long
): ScreenshotGroupData {
    val groupId = dataInteractor.insertGroup(
        createGroup(
            name = "Track & Graph",
            parentGroupId = parentGroupId,
        )
    ).componentId

    // Create Relaxation tracker
    val relaxationTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Relaxation",
            groupId = groupId,
        )
    ).componentId

    // Create data for Relaxation tracker
    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = relaxationTrackerId,
        sinTransform = SinTransform(amplitude = 1.0, wavelength = 210.0, yOffset = -20.0),
        randomSeed = 123,
        randomOffsetScalar = 40.0,
        numDataPoints = 500,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 6,
        endPoint = OffsetDateTime.now().minusHours(6),
        roundToInt = false,
    )

    // Create Stress tracker
    val stressTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = "Stress",
            groupId = groupId,
            description = "Track daily stress level (0-10)",
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    ).componentId

    // Create data for Stress tracker - slightly inverse to Relaxation
    createWaveData(
        dataInteractor = dataInteractor,
        trackerId = stressTrackerId,
        sinTransform = SinTransform(amplitude = 1.0, wavelength = 210.0, xOffset = 100.0, yOffset = -20.0),
        randomSeed = 456,
        randomOffsetScalar = 40.0,
        numDataPoints = 500,
        spacing = Duration.ofDays(1),
        spacingRandomisationHours = 6,
        endPoint = OffsetDateTime.now(),
        roundToInt = false,
    )

    return ScreenshotGroupData(groupId, relaxationTrackerId, stressTrackerId)
}

private suspend fun createScreenshot2Group(
    dataInteractor: DataInteractor,
    dataSampler: DataSampler,
    parentGroupId: Long,
    screenshot1Data: ScreenshotGroupData
): ScreenshotGroupData {
    val groupId = dataInteractor.insertGroup(
        createGroup(
            name = "Track & Graph",
            parentGroupId = parentGroupId,
        )
    ).componentId

    // Get the trackers from Screenshot 1
    val relaxationTracker = dataInteractor.getTrackerById(screenshot1Data.relaxationTrackerId)!!
    val stressTracker = dataInteractor.getTrackerById(screenshot1Data.stressTrackerId)!!

    // Create new trackers with same names but in Screenshot 2 group
    val relaxationTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = relaxationTracker.name,
            groupId = groupId,
            description = relaxationTracker.description,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    ).componentId

    val stressTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = stressTracker.name,
            groupId = groupId,
            description = stressTracker.description,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    ).componentId

    // Copy data from original trackers to new ones
    val relaxationFeatureId = dataInteractor.getTrackerById(relaxationTrackerId)!!.featureId
    val stressFeatureId = dataInteractor.getTrackerById(stressTrackerId)!!.featureId

    val originalRelaxationFeatureId = relaxationTracker.featureId
    val originalStressFeatureId = stressTracker.featureId

    val relaxationDataPoints = dataSampler
        .getDataSampleForFeatureId(originalRelaxationFeatureId)
        .getAllRawDataPoints()
    val stressDataPoints = dataSampler
        .getDataSampleForFeatureId(originalStressFeatureId)
        .getAllRawDataPoints()

    for (dataPoint in relaxationDataPoints) {
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = dataPoint.timestamp,
                featureId = relaxationFeatureId,
                value = dataPoint.value,
                label = dataPoint.label,
                note = dataPoint.note
            )
        )
    }

    for (dataPoint in stressDataPoints) {
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = dataPoint.timestamp,
                featureId = stressFeatureId,
                value = dataPoint.value,
                label = dataPoint.label,
                note = dataPoint.note
            )
        )
    }

    // Create Monthly Moving Average Line Graph
    createMonthlyMovingAverageGraph(
        dataInteractor = dataInteractor,
        groupId = groupId,
        relaxationFeatureId = relaxationFeatureId,
        stressFeatureId = stressFeatureId,
    )

    return ScreenshotGroupData(groupId, relaxationTrackerId, stressTrackerId)
}

private suspend fun createScreenshot3Group(
    dataInteractor: DataInteractor,
    dataSampler: DataSampler,
    parentGroupId: Long,
    screenshot2Data: ScreenshotGroupData
): ScreenshotGroupData {
    val groupId = dataInteractor.insertGroup(
        createGroup(
            name = "Track & Graph",
            parentGroupId = parentGroupId,
        )
    ).componentId

    // Get the trackers from Screenshot 2
    val relaxationTracker = dataInteractor.getTrackerById(screenshot2Data.relaxationTrackerId)!!
    val stressTracker = dataInteractor.getTrackerById(screenshot2Data.stressTrackerId)!!

    // Create new trackers with same names but in Screenshot 3 group
    val relaxationTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = relaxationTracker.name,
            groupId = groupId,
            description = relaxationTracker.description,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    ).componentId

    val stressTrackerId = dataInteractor.createTracker(
        createTrackerRequest(
            name = stressTracker.name,
            groupId = groupId,
            description = stressTracker.description,
            suggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING
        )
    ).componentId

    // Copy data from original trackers to new ones
    val relaxationFeatureId = dataInteractor.getTrackerById(relaxationTrackerId)!!.featureId
    val stressFeatureId = dataInteractor.getTrackerById(stressTrackerId)!!.featureId

    val originalRelaxationFeatureId = relaxationTracker.featureId
    val originalStressFeatureId = stressTracker.featureId

    val relaxationDataPoints = dataSampler
        .getDataSampleForFeatureId(originalRelaxationFeatureId)
        .getAllRawDataPoints()

    val stressDataPoints = dataSampler
        .getDataSampleForFeatureId(originalStressFeatureId)
        .getAllRawDataPoints()

    for (dataPoint in relaxationDataPoints) {
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = dataPoint.timestamp,
                featureId = relaxationFeatureId,
                value = dataPoint.value,
                label = dataPoint.label,
                note = dataPoint.note
            )
        )
    }

    for (dataPoint in stressDataPoints) {
        dataInteractor.insertDataPoint(
            createDataPoint(
                timestamp = dataPoint.timestamp,
                featureId = stressFeatureId,
                value = dataPoint.value,
                label = dataPoint.label,
                note = dataPoint.note
            )
        )
    }

    // Create Monthly Moving Average Line Graph
    createMonthlyMovingAverageGraph(
        dataInteractor = dataInteractor,
        groupId = groupId,
        relaxationFeatureId = relaxationFeatureId,
        stressFeatureId = stressFeatureId,
    )

    // Add empty Health group
    dataInteractor.insertGroup(
        createGroup(name = "Health", parentGroupId = groupId)
    )

    return ScreenshotGroupData(groupId, relaxationTrackerId, stressTrackerId)
}

private suspend fun createMonthlyMovingAverageGraph(
    dataInteractor: DataInteractor,
    groupId: Long,
    relaxationFeatureId: Long,
    stressFeatureId: Long,
): Long {
    return dataInteractor.createLineGraph(
        LineGraphCreateRequest(
            name = "Relaxation Vs Stress (Monthly moving averages)",
            groupId = groupId,
            config = LineGraphConfig(
                features = listOf(
                    LineGraphFeatureConfig(
                        featureId = relaxationFeatureId,
                        name = "Relaxation",
                        colorIndex = 7,
                        averagingMode = LineGraphAveragingModes.MONTHLY_MOVING_AVERAGE,
                        plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                        pointStyle = LineGraphPointStyle.NONE,
                        offset = 0.0,
                        scale = 1.0,
                        durationPlottingMode = DurationPlottingMode.NONE
                    ),
                    LineGraphFeatureConfig(
                        featureId = stressFeatureId,
                        name = "Stress",
                        colorIndex = 1,
                        averagingMode = LineGraphAveragingModes.MONTHLY_MOVING_AVERAGE,
                        plottingMode = LineGraphPlottingModes.WHEN_TRACKED,
                        pointStyle = LineGraphPointStyle.NONE,
                        offset = 0.0,
                        scale = 1.0,
                        durationPlottingMode = DurationPlottingMode.NONE
                    )
                ),
                sampleSize = Period.ofMonths(6),
                yRangeType = YRangeType.FIXED,
                yFrom = 0.0,
                yTo = 10.0,
                endDate = GraphEndDate.Latest
            )
        )
    ).componentId
}
