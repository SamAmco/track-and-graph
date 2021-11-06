package com.samco.trackandgraph.antlr.eval

import com.samco.trackandgraph.antlr.evaluation.*
import com.samco.trackandgraph.antlr.someData
import com.samco.trackandgraph.antlr.someDataRandom
import com.samco.trackandgraph.antlr.*
import com.samco.trackandgraph.database.entity.DataPoint
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.functionslib.DataSample
import com.samco.trackandgraph.functionslib.MovingAverageFunction
import kotlinx.coroutines.runBlocking
import org.junit.Assert
import org.junit.Test
import org.threeten.bp.DayOfWeek
import org.threeten.bp.Duration
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.temporal.TemporalAdjusters

class FunctionsOnDataTest {
    // Most of these test are pretty cyclical. I guess one could copy the function implementation into this
    // file to make sure the behavior doesn't change, but that's not that much better i feel like
    // for now i manually checked if the results are the expected ones


    @Test
    fun deltaFunctionTest() {
        val datapoints = someData()

        val evaluationModel = EvaluationModel()

        val code = "var a = Delta(data)"
        val context = evaluationModel.run(code, mapOf("data" to datapoints))

        val output = context["a"] as DatapointsValue

        Assert.assertEquals(
            runBlocking { DeltaFunction().main(listOf(DatapointsValue(datapoints))) },
            output
        )

    }

    @Test
    fun accumulateFunctionTest() {
        val datapoints = someData()

        val evaluationModel = EvaluationModel()

        val code = "var a = Accumulate(data)"
        val context = evaluationModel.run(code, mapOf("data" to datapoints))

        val output = context["a"] as DatapointsValue

        Assert.assertEquals(
            runBlocking { AccumulateFunction().main(listOf(DatapointsValue(datapoints))) },
            output
        )

    }

    @Test
    fun derivativeFunctionTest() {
        val datapoints = generateDataPoints2(
            listOf(
                Triple(DayOfWeek.MONDAY, 3 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 13 * 60, 10.0), // 10 in 10 hours // 1
                Triple(DayOfWeek.MONDAY, 18 * 60, 15.0), // 5 in 5 hours   // 1
                Triple(DayOfWeek.MONDAY, 20 * 60, 25.0), // 10 in 2 hours  // 5
            )
        )

        val evaluationModel = EvaluationModel()

        val code = "var a = Derivative(data, HOUR)"
        val context = evaluationModel.run(code, mapOf("data" to datapoints))

        val output = context["a"] as DatapointsValue

        Assert.assertEquals(
            runBlocking {
                DerivativeFunction().main(
                    listOf(
                        DatapointsValue(datapoints),
                        TimeValue(Duration.ofHours(1))
                    )
                )
            },
            output
        )

    }

    @Test
    fun timeBetweenFunctionTest() {
        val datapoints = generateDataPoints2(
            listOf(
                Triple(DayOfWeek.MONDAY, 3 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 13 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 18 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 20 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 24 * 60, 0.0),
            )
        )

        val evaluationModel = EvaluationModel()

        val code = "var output = TimeBetween(data) / HOUR"
        val context = evaluationModel.run(code, mapOf("data" to datapoints))

        val output = context["output"] as DatapointsValue

        Assert.assertEquals(
            runBlocking {
                TimeBetweenFunction().main(
                    listOf(
                        DatapointsValue(datapoints),
                    )
                ) / TimeValue(Duration.ofHours(1))
            },
            output
        )

    }

    @Test
    fun timeBetween2FunctionTest() {
        val datapointsMain = generateDataPoints2(
            listOf(
                Triple(DayOfWeek.MONDAY, 3 * 60, 0.0), // no unique ref, gets dropped
                Triple(DayOfWeek.MONDAY, 13 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 18 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 20 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 24 * 60, 0.0), // no follow up ref, gets dropped
            )
        )

        val datapointsRef = generateDataPoints2(
            listOf(
//                Triple(DayOfWeek.MONDAY, 3 * 60, 0.0),
                Triple(DayOfWeek.MONDAY, 13 * 60 + 30, 0.0),
                Triple(DayOfWeek.MONDAY, 18 * 60 + 60, 0.0),
                Triple(DayOfWeek.MONDAY, 20 * 60 + 120, 0.0),
            )
        )

        val evaluationModel = EvaluationModel()

        val code = "var output = TimeBetween2(main, ref) / HOUR"
        val context =
            evaluationModel.run(code, mapOf("main" to datapointsMain, "ref" to datapointsRef))

        val output = context["output"] as DatapointsValue

        Assert.assertEquals(
            runBlocking {
                TimeBetween2Function().main(
                    listOf(
                        DatapointsValue(datapointsMain),
                        DatapointsValue(datapointsRef),
                    )
                ) / TimeValue(Duration.ofHours(1))
            },
            output
        )

    }

    @Test
    fun filterFunctionTest() {
        val datapoints = generateDataPoints2Categorical(
            listOf(
                Triple(DayOfWeek.MONDAY, 12 * 60, 0),
                Triple(DayOfWeek.MONDAY, 19 * 60, 1),
                Triple(DayOfWeek.TUESDAY, 12 * 60, 0),
                Triple(DayOfWeek.TUESDAY, 19 * 60, 1),
                Triple(DayOfWeek.WEDNESDAY, 12 * 60, 0),
            ),
            mapOf(0 to "Lunch", 1 to "Dinner")
        )

        val datapointsFiltered = datapoints.dataPoints.filter { it.label == "Dinner" }

        val evaluationModel = EvaluationModel()

        val code = """var output = Filter(data, "Dinner")"""
        val context = evaluationModel.run(code, mapOf("data" to datapoints))

        val output = context["output"] as DatapointsValue

        Assert.assertEquals(
            DatapointsValue(datapointsFiltered, DataType.CATEGORICAL, featureId = 0L),
            output
        )

    }

    @Test
    fun excludeFunctionTest() {
        val datapoints = generateDataPoints2Categorical(
            listOf(
                Triple(DayOfWeek.MONDAY, 12 * 60, 0),
                Triple(DayOfWeek.MONDAY, 19 * 60, 1),
                Triple(DayOfWeek.TUESDAY, 12 * 60, 0),
                Triple(DayOfWeek.TUESDAY, 19 * 60, 1),
                Triple(DayOfWeek.WEDNESDAY, 12 * 60, 0),
            ),
            mapOf(0 to "Lunch", 1 to "Dinner")
        )

        val datapointsFiltered = datapoints.dataPoints.filterNot { it.label == "Lunch" }

        val evaluationModel = EvaluationModel()

        val code = """var output = Exclude(data, "Lunch")"""
        val context = evaluationModel.run(code, mapOf("data" to datapoints))

        val output = context["output"] as DatapointsValue

        Assert.assertEquals(
            DatapointsValue(datapointsFiltered, DataType.CATEGORICAL, featureId = 0L),
            output
        )

    }

    @Test
    fun mergeFunctionTest() {
        val d1 = someDataRandom()
        val d2 = someDataRandom()
        val d3 = someDataRandom()

        val expected = (d1.dataPoints + d2.dataPoints + d3.dataPoints).sortedBy { it.timestamp }

        val evaluationModel = EvaluationModel()

        val code = "var output = Merge(d1, d2, d3)"
        val context =
            evaluationModel.run(code, mapOf("d1" to d1, "d2" to d2, "d3" to d3))

        val output = context["output"] as DatapointsValue

        Assert.assertEquals(
            expected,
            output.datapoints
        )

    }

    @Test
    fun calculateMovingAverages() {
        /* The test assumes that datapoints will only be averages when the time between is
           smaller than the given window, NOT smaller or equal, e.g. the last two points will not be
           averaged together since they are exactly 10 hours apart, not less.
         */
        // copied from Statistics_calculateMovingAverages_KtTest
        runBlocking {
            //GIVEN
            val data = someData()
            val averagingDuration = Duration.ofHours(10)

            //WHEN
            val evaluationModel = EvaluationModel()

            val code = "var output = Moving(data, AVERAGE, HOUR * 10)"
            val context =
                evaluationModel.run(code, mapOf("data" to data))

            val answer = context["output"] as DatapointsValue

            //THEN
            val expected = listOf(5.0, 0.0, 2.0, 2.0, 1.5, 2.0, 8.0, 7.0, 3.0) // correct for fixed values in someData()
            val actual = answer.datapoints.map { dp -> dp.value }
            junit.framework.Assert.assertEquals(expected, actual)
        }
    }

    @Test
    fun fixedBinAggregationFunctionTest() {
        val d1 = someDataRandom()
        val d2SameYear = someDataRandom()
        val d2 = DataSample(d2SameYear.dataPoints.map {
            it.copyPoint(
                timestamp = it.timestamp.minusYears(1)
            )
        }, d2SameYear.featureType, d2SameYear.featureId)


        val evaluationModel = EvaluationModel()

        val code = """var c1 = Bin(d1, COUNT, DAY)
                     |var c2 = Bin(d2, COUNT, DAY)
                     |var nf1 = Bin(d1, MIN, DAY)
                     |var nf2 = Bin(d2, MIN, DAY)
                     |var f1 = Bin(d1, MIN, DAY, 0)
                     |var f2 = Bin(d2, MIN, DAY, 0)""".trimMargin("|")


        val context = evaluationModel.run(code, mapOf("d1" to d1, "d2" to d2))

        val c1 = context["c1"] as DatapointsValue // SUM automatically has a fallback, so none needed
        val c2 = context["c2"] as DatapointsValue
        val nf1 = context["nf1"] as DatapointsValue // no fallback
        val nf2 = context["nf2"] as DatapointsValue
        val f1 = context["f1"] as DatapointsValue // fallback
        val f2 = context["f2"] as DatapointsValue

        Assert.assertEquals(c1.datapoints.map { it.timestamp }, c2.datapoints.map { it.timestamp })
        Assert.assertNotEquals(nf1.datapoints.map { it.timestamp }, nf2.datapoints.map { it.timestamp })
        Assert.assertEquals(f1.datapoints.map { it.timestamp }, f2.datapoints.map { it.timestamp })

    }

}