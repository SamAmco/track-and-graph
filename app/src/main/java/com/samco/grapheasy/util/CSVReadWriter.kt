package com.samco.grapheasy.util

import com.samco.grapheasy.database.Feature
import com.samco.grapheasy.database.GraphEasyDatabaseDao
import org.apache.commons.csv.CSVFormat
import org.apache.commons.csv.CSVPrinter
import java.io.OutputStream


object CSVReadWriter {
    private enum class HEADERS(val displayName: String) {
        Name("Feature name"),
        Time("timestamp"),
        Value("value")
    }

    fun WriteFeaturesToCSV(features: List<Feature>, dataSource: GraphEasyDatabaseDao, outStream: OutputStream) {
        outStream.writer().use {
            val headerNames = HEADERS.values().map { h -> h.displayName }.toTypedArray()
            val csvWriter = CSVPrinter(it, CSVFormat.DEFAULT.withHeader(*headerNames))
            for (feature in features) {
                val dataPoints = dataSource.getDataPointsForFeatureSync(feature.id)
                dataPoints.forEach { dp -> csvWriter.printRecord(feature.name, dp.timestamp.toString(), dp.value) }
            }
            it.flush()
        }
    }
}
