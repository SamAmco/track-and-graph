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

package com.samco.trackandgraph.data.database.dto

import com.samco.trackandgraph.data.database.entity.queryresponse.TrackerWithFeature
import com.samco.trackandgraph.data.interactor.TrackerHelper.DurationNumericConversionMode

data class Tracker(
    val id: Long,
    override val name: String,
    override val groupIds: Set<Long>,
    override val featureId: Long,
    override val displayIndex: Int,
    override val description: String,
    val dataType: DataType,
    val hasDefaultValue: Boolean,
    val defaultValue: Double,
    val defaultLabel: String,
    val suggestionType: TrackerSuggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
    val suggestionOrder: TrackerSuggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
) : Feature {

    companion object {
        internal fun fromTrackerWithFeature(twf: TrackerWithFeature) = Tracker(
            id = twf.id,
            name = twf.name,
            // TODO: Currently features only exist in one group, but this will change
            groupIds = setOf(twf.groupId),
            featureId = twf.featureId,
            displayIndex = twf.displayIndex,
            description = twf.description,
            dataType = twf.dataType,
            hasDefaultValue = twf.hasDefaultValue,
            defaultValue = twf.defaultValue,
            defaultLabel = twf.defaultLabel,
            suggestionType = TrackerSuggestionType.fromEntity(twf.suggestionType),
            suggestionOrder = TrackerSuggestionOrder.fromEntity(twf.suggestionOrder)
        )
    }

    internal fun toEntity() = com.samco.trackandgraph.data.database.entity.Tracker(
        id = id,
        featureId = featureId,
        dataType = dataType,
        hasDefaultValue = hasDefaultValue,
        defaultValue = defaultValue,
        defaultLabel = defaultLabel,
        suggestionType = suggestionType.toEntity(),
        suggestionOrder = suggestionOrder.toEntity()
    )

    internal fun toFeatureEntity(groupId: Long) = com.samco.trackandgraph.data.database.entity.Feature(
        id = featureId,
        name = name,
        groupId = groupId,
        displayIndex = displayIndex,
        description = description,
    )
}

/**
 * Request object for creating a new Tracker.
 *
 * Note: id, featureId, and displayIndex are handled by the data layer and should not be provided.
 */
data class TrackerCreateRequest(
    val name: String,
    val groupId: Long,
    val dataType: DataType,
    val description: String = "",
    val hasDefaultValue: Boolean = false,
    val defaultValue: Double = 0.0,
    val defaultLabel: String = "",
    val suggestionType: TrackerSuggestionType = TrackerSuggestionType.VALUE_AND_LABEL,
    val suggestionOrder: TrackerSuggestionOrder = TrackerSuggestionOrder.VALUE_ASCENDING,
)

/**
 * Request object for updating an existing Tracker.
 *
 * All fields except [id] are optional. A null value means "don't change this field".
 *
 * Note: To move a tracker between groups, use [MoveFeatureRequest] instead.
 *
 * @param durationNumericConversionMode When changing dataType to/from DURATION, specifies
 * how to convert existing data points (to HOURS, MINUTES, or SECONDS).
 */
data class TrackerUpdateRequest(
    val id: Long,
    val name: String? = null,
    val description: String? = null,
    val dataType: DataType? = null,
    val hasDefaultValue: Boolean? = null,
    val defaultValue: Double? = null,
    val defaultLabel: String? = null,
    val suggestionType: TrackerSuggestionType? = null,
    val suggestionOrder: TrackerSuggestionOrder? = null,
    val durationNumericConversionMode: DurationNumericConversionMode? = null,
)

/**
 * Request object for deleting a Tracker.
 *
 * @param groupId If specified, the tracker will only be removed from this group.
 *                If null, the tracker will be deleted entirely from all groups.
 * @param trackerId The ID of the tracker to delete.
 * @param featureId The feature ID associated with the tracker.
 */
data class TrackerDeleteRequest(
    val trackerId: Long,
    val featureId: Long,
    val groupId: Long? = null,
)
