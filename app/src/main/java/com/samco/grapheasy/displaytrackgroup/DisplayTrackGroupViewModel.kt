package com.samco.grapheasy.displaytrackgroup

import com.samco.grapheasy.database.DisplayFeature
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.ui.DataPointInputViewModel
import org.threeten.bp.OffsetDateTime

class DisplayTrackGroupViewModel(trackGroupId: Long, dataSource: GraphEasyDatabaseDao)
    : DataPointInputViewModel(),
    InputDataPointDialog.InputDataPointDialogViewModel,
    ExportFeaturesDialog.ExportFeaturesViewModel {

    override var selectedFeatures: MutableList<Feature>? = null

    var currentActionFeature: DisplayFeature? = null
    var currentActionFeatures: List<DisplayFeature>? = null
    val features = dataSource.getDisplayFeaturesForTrackGroup(trackGroupId)
}