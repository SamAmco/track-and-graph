package com.samco.trackandgraph.base.database.sampling

import org.threeten.bp.temporal.TemporalAmount

data class DataSampleProperties(
    val regularity: TemporalAmount? = null,
    val isDuration: Boolean = false
)