package com.samco.grapheasy.displaytrackgroup

import androidx.lifecycle.ViewModel
import com.samco.grapheasy.database.DisplayFeature
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import org.threeten.bp.OffsetDateTime

class DisplayTrackGroupViewModel(
    private val trackGroupId: Long,
    private val dataSource: GraphEasyDatabaseDao
): ViewModel(), DataPointInputFragment.InputDataPointViewModel {

    override var selectedDateTime: OffsetDateTime? = null
    override var currentValue: String? = null

    var currentActionFeature: DisplayFeature? = null

    val features = dataSource.getDisplayFeaturesForTrackGroup(trackGroupId)
}