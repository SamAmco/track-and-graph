package com.samco.grapheasy.displaytrackgroup

import android.net.Uri
import com.samco.grapheasy.database.*
import com.samco.grapheasy.ui.DataPointInputViewModel

class DisplayTrackGroupViewModel(trackGroupId: Long, dataSource: GraphEasyDatabaseDao)
    : DataPointInputViewModel(),
    InputDataPointDialog.InputDataPointDialogViewModel,
    ExportFeaturesDialog.ExportFeaturesViewModel,
    ImportFeaturesDialog.ImportFeaturesViewModel,
    AddFeatureDialogFragment.AddFeatureDialogViewModel
{
    override var featureName: String? = null
    override var featureType: FeatureType? = null
    override var discreteValues: MutableList<DiscreteValue>? = null

    override var selectedFeatures: MutableList<Feature>? = null
    override var selectedFileUri: Uri? = null

    var currentActionFeature: DisplayFeature? = null
    var currentActionFeatures: List<DisplayFeature>? = null
    val features = dataSource.getDisplayFeaturesForTrackGroup(trackGroupId)
}