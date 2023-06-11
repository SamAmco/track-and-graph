package com.samco.trackandgraph.functions

import com.samco.trackandgraph.base.database.dto.IDataPoint
import com.samco.trackandgraph.base.database.sampling.DataSample
import com.samco.trackandgraph.base.database.sampling.DataSampleProperties

fun fromSequence(
    sequence: Sequence<IDataPoint>,
    dataSampleProperties: DataSampleProperties = DataSampleProperties(),
) = DataSample.fromSequence(
    sequence,
    dataSampleProperties = dataSampleProperties,
    onDispose = {}
)