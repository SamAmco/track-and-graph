package com.samco.grapheasy.featurehistory

import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.displaytrackgroup.InputDataPointDialog
import com.samco.grapheasy.ui.DataPointInputViewModel

class FeatureHistoryViewModel(featureId: Long, dataSource: GraphEasyDatabaseDao)
    : DataPointInputViewModel(), InputDataPointDialog.InputDataPointDialogViewModel {
    var feature: Feature? = null
    val dataPoints = dataSource.getDataPointsForFeature(featureId)
    var currentActionDataPoint: DataPoint? = null
}