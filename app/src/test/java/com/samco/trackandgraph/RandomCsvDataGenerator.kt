package com.samco.trackandgraph

/*
class RandomCsvDataGenerator {

    private val databaseFormatter: DateTimeFormatter = DateTimeFormatter.ISO_OFFSET_DATE_TIME

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

    //@Test
    fun generate_random_data() {
        val numRecords = 1000000
        PrintWriter(File("./$numRecords-test.csv")).use {
            it.println(CSVReadWriter.HEADERS.values().joinToString(",") { h -> h.name } )
            var currTime = OffsetDateTime.now()
            for (i in 0..numRecords) {
                val lineData = randLineData()
                currTime = currTime.plusMinutes(1)
                it.println("${lineData.featureName},${databaseFormatter.format(currTime)},${lineData.featureValue}")
            }
        }
    }

    private class SinTransform(val amplitude: Double, val wavelength: Double) {
        fun transform(index: Int): Double {
            val sinTransform = sin((index.toDouble() / wavelength) * Math.PI * 2.0)
            return ((sinTransform + 1.0) / 2.0) * amplitude
        }
    }

    private val wpt1 = SinTransform(1.0, 3.0)
    private val wpt2 = SinTransform(0.5, 21.0)
    private val wpt3 = SinTransform(0.25, 90.0)
    private val wpTotalAmp = 1.75
    private val randomRatio = 0.5
    private val sinRatio = 0.5

    private fun generateWheatPainData_Wheat(index: Int): LineData {
        val sinTransform = (wpt1.transform(index) + wpt2.transform(index) + wpt3.transform(index)) / wpTotalAmp
        val randAdjusted = (sinTransform * sinRatio) + (Random.nextDouble() * randomRatio)
        val value = (randAdjusted * 2.0).roundToInt()
        println("$sinTransform -> $value")
        val valueString = when (value) {
            0 -> "0:None"
            1 -> "1:Some"
            2 -> "2:Lots"
            else -> throw Exception("unrecognised int generated")
        }
        return LineData("Wheat", valueString)
    }

    private fun generateWheatPainData_Pain(index: Int): LineData {
        val sinTransform = (wpt1.transform(index) + wpt2.transform(index) + wpt3.transform(index)) / wpTotalAmp
        val randAdjusted = (sinTransform * sinRatio) + (Random.nextDouble() * randomRatio)
        val valueString = (randAdjusted * 10.0).roundToInt().toString()
        return LineData("Stomach Pain", valueString)
    }

    private fun printLineData(lineData: List<LineData>, currTime: OffsetDateTime, printWriter: PrintWriter) {
        lineData.forEach { lineData ->
            printWriter.println("${lineData.featureName},${databaseFormatter.format(currTime)},${lineData.featureValue}")
        }
    }

    //@Test
    fun generate_wheat_pain_correlated_data() {
        val numRecords = 100
        PrintWriter(File("./wheat-pain-test.csv")).use {
            it.println(CSVReadWriter.HEADERS.values().joinToString(",") { h -> h.name } )
            var currTime = OffsetDateTime.now()
                .withHour(0)
                .withMinute(0)
                .withSecond(0)
                .withNano(0)
                .minusDays(numRecords / 3L)
            for (x in 0..(numRecords / 3)) {
                var index = x * 3
                currTime = currTime.withHour(9)
                val wheatData1 = generateWheatPainData_Wheat(index)
                val painData1 = generateWheatPainData_Pain(index)
                printLineData(listOf(wheatData1, painData1), currTime, it)
                index++

                currTime = currTime.withHour(13)
                val wheatData2 = generateWheatPainData_Wheat(index)
                val painData2 = generateWheatPainData_Pain(index)
                printLineData(listOf(wheatData2, painData2), currTime, it)
                index++

                currTime = currTime.withHour(19)
                val wheatData3 = generateWheatPainData_Wheat(index)
                val painData3 = generateWheatPainData_Pain(index)
                printLineData(listOf(wheatData3, painData3), currTime, it)

                currTime = currTime.plusDays(1)
            }
        }
    }
}

 */