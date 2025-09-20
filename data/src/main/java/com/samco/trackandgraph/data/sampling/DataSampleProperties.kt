package com.samco.trackandgraph.data.sampling

import org.threeten.bp.temporal.TemporalAmount

data class DataSampleProperties(
    val regularity: TemporalAmount? = null,
    val isDuration: Boolean = false
)