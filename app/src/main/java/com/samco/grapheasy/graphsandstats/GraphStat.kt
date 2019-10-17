package com.samco.grapheasy.graphsandstats

import com.samco.grapheasy.database.DataSamplerSpec

class GraphStat(val id: Long, val name: String) {
    companion object {
        fun fromDataSamplerSpec(dataSamplerSpec: DataSamplerSpec): GraphStat {
            return GraphStat(dataSamplerSpec.id, dataSamplerSpec.name)
        }
    }
}