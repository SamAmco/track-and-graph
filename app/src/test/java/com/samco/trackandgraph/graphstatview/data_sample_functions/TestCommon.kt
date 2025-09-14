package com.samco.trackandgraph.graphstatview.data_sample_functions

import com.samco.trackandgraph.data.database.dto.IDataPoint
import com.samco.trackandgraph.data.database.sampling.DataSample
import com.samco.trackandgraph.data.database.sampling.DataSampleProperties

fun fromSequence(
    sequence: Sequence<IDataPoint>,
    dataSampleProperties: DataSampleProperties = DataSampleProperties(),
) = DataSample.fromSequence(
    sequence,
    dataSampleProperties = dataSampleProperties,
    onDispose = {}
)