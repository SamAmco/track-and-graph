package com.samco.trackandgraph.graphstatview.factories

import com.samco.trackandgraph.base.database.dto.DataPoint
import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.model.DataInteractor
import com.samco.trackandgraph.functions.functions.CompositeFunction
import com.samco.trackandgraph.functions.functions.DataSampleFunction
import com.samco.trackandgraph.functions.functions.FilterLabelFunction
import com.samco.trackandgraph.functions.functions.FilterValueFunction
import javax.inject.Inject

class DataFactoryCommonHelpers @Inject constructor() {
    suspend fun getLastDataPoint(
        dataInteractor: DataInteractor,
        featureId: Long,
        filterByLabels: Boolean,
        labels: List<String>,
        filterByRange: Boolean,
        fromValue: Double,
        toValue: Double,
        onDataSampled: (List<DataPoint>) -> Unit
    ): IDataPoint? {
        val dataSample = dataInteractor.getDataSampleForFeatureId(featureId)

        val filters = mutableListOf<DataSampleFunction>()
        if (filterByLabels) filters.add(FilterLabelFunction(labels.toSet()))
        if (filterByRange) filters.add(FilterValueFunction(fromValue, toValue))

        val sample = CompositeFunction(filters).mapSample(dataSample)
        val first = sample.firstOrNull()

        onDataSampled(sample.getRawDataPoints())
        dataSample.dispose()
        return first
    }
}