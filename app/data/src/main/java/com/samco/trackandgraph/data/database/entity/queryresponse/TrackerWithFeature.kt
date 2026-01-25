package com.samco.trackandgraph.data.database.entity.queryresponse

import androidx.room.ColumnInfo
import com.samco.trackandgraph.data.database.dto.DataType
import com.samco.trackandgraph.data.database.entity.Feature
import com.samco.trackandgraph.data.database.entity.TrackerSuggestionOrder
import com.samco.trackandgraph.data.database.entity.TrackerSuggestionType

internal data class TrackerWithFeature(
    @ColumnInfo(name = "id")
    val id: Long,

    @ColumnInfo(name = "name")
    val name: String,

    @ColumnInfo(name = "group_id")
    val groupId: Long,

    @ColumnInfo(name = "feature_id")
    val featureId: Long,

    @ColumnInfo(name = "display_index")
    val displayIndex: Int,

    @ColumnInfo(name = "feature_description")
    val description: String,

    @ColumnInfo(name = "type")
    val dataType: DataType,

    @ColumnInfo(name = "has_default_value")
    val hasDefaultValue: Boolean,

    @ColumnInfo(name = "default_value")
    val defaultValue: Double,

    @ColumnInfo(name = "default_label")
    val defaultLabel: String,

    @ColumnInfo(name = "suggestion_type")
    val suggestionType: TrackerSuggestionType,

    @ColumnInfo(name = "suggestion_order")
    val suggestionOrder: TrackerSuggestionOrder
) {
    fun toFeatureEntity() = Feature(
        id = featureId,
        name = name,
        groupId = groupId,
        displayIndex = displayIndex,
        description = description
    )
}