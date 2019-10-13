package com.samco.grapheasy

import com.samco.grapheasy.database.databaseFormatter
import com.samco.grapheasy.util.CSVReadWriter
import org.junit.Test

import org.threeten.bp.OffsetDateTime
import java.io.File
import java.io.PrintWriter
import kotlin.random.Random

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * See [testing documentation](http://d.android.com/tools/testing).
 */
class RandomCsvDataGenerator {

    data class LineData(val featureName: String, val featureValue: String)

    fun randFeatureValue() = when (Random.nextInt(4)) {
        0 -> "0:small"
        1 -> "1:medium"
        2 -> "2:large"
        else -> "3:very large"
    }

    fun randLineData() = when (Random.nextInt(4)) {
        0 -> LineData("cont1", Random.nextDouble().toString())
        1 -> LineData("cont2", Random.nextDouble().toString())
        2 -> LineData("disc1", randFeatureValue())
        else -> LineData("disc2", randFeatureValue())
    }

    @Test
    fun addition_isCorrect() {
        val numRecords = 1000000
        PrintWriter(File("./$numRecords-test.txt")).use {
            it.println(CSVReadWriter.HEADERS.values().joinToString(",") { h -> h.name } )
            var currTime = OffsetDateTime.now()
            for (i in 0..numRecords) {
                val lineData = randLineData()
                currTime = currTime.plusMinutes(1)
                it.println("${lineData.featureName},${databaseFormatter.format(currTime)},${lineData.featureValue}")
            }
        }
    }
}
