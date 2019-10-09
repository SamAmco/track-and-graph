package com.samco.grapheasy.ui

import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.displaytrackgroup.DataPointInputFragment
import com.samco.grapheasy.displaytrackgroup.InputDataPointDialog

open class DataPointInputViewModel : ViewModel(), InputDataPointDialog.InputDataPointDialogViewModel{
    private var dataPointDisplayDataByFeature = mutableMapOf<Long, DataPointInputFragment.DataPointDisplayData>()

    override var currentDataPointInputFeature: Feature? = null

    override fun clearDataPointDisplayData() = dataPointDisplayDataByFeature.clear()

    override fun putDataPointDisplayData(displayData: DataPointInputFragment.DataPointDisplayData) {
        dataPointDisplayDataByFeature[displayData.featureId] = displayData
    }

    override fun getDataPointDisplayData(featureId: Long): DataPointInputFragment.DataPointDisplayData {
        return dataPointDisplayDataByFeature[featureId]!!
    }

    override fun isDataPointDisplayDataInitialized() = dataPointDisplayDataByFeature.isNotEmpty()

}