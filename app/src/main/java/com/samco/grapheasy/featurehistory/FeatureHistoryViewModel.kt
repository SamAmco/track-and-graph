package com.samco.grapheasy.featurehistory

import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.DataPoint
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import com.samco.grapheasy.displaytrackgroup.DataPointInputFragment
import org.threeten.bp.OffsetDateTime

class FeatureHistoryViewModel(
    featureId: Long,
    dataSource: GraphEasyDatabaseDao
) : ViewModel(), DataPointInputFragment.InputDataPointViewModel {

    var feature: Feature? = null
    val dataPoints = dataSource.getDataPointsForFeature(featureId)
    var currentActionDataPoint: DataPoint? = null

    override var selectedDateTime: OffsetDateTime? = null
    override var currentValue: String? = null
}