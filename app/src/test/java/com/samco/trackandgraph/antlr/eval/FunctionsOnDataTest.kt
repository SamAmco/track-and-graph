package com.samco.trackandgraph.antlr.eval

import com.samco.trackandgraph.antlr.evaluation.*
import com.samco.trackandgraph.antlr.someData
import com.samco.trackandgraph.antlr.someDataRandom
import com.samco.trackandgraph.antlr.*
import com.samco.trackandgraph.database.entity.DataPoint
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
            DeltaFunction().main(listOf(DatapointsValue(datapoints, DataType.NUMERICAL))),
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
            AccumulateFunction().main(listOf(DatapointsValue(datapoints, DataType.NUMERICAL))),
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
            DerivativeFunction().main(
                listOf(
                    DatapointsValue(datapoints, DataType.NUMERICAL),
                    TimeValue(Duration.ofHours(1))
                )
            ),
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
            TimeBetweenFunction().main(
                listOf(
                    DatapointsValue(datapoints, DataType.NUMERICAL),
                )
            ) / TimeValue(Duration.ofHours(1)),
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
            TimeBetween2Function().main(
                listOf(
                    DatapointsValue(datapointsMain, DataType.NUMERICAL),
                    DatapointsValue(datapointsRef, DataType.NUMERICAL),
                )
            ) / TimeValue(Duration.ofHours(1)),
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

        val datapointsFiltered = datapoints.filter { it.label == "Dinner" }

        val evaluationModel = EvaluationModel()

        val code = """var output = Filter(data, "Dinner")"""
        val context = evaluationModel.run(code, mapOf("data" to datapoints))

        val output = context["output"] as DatapointsValue

        Assert.assertEquals(
            DatapointsValue(datapointsFiltered, DataType.CATEGORICAL),
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

        val datapointsFiltered = datapoints.filterNot { it.label == "Lunch" }

        val evaluationModel = EvaluationModel()

        val code = """var output = Exclude(data, "Lunch")"""
        val context = evaluationModel.run(code, mapOf("data" to datapoints))

        val output = context["output"] as DatapointsValue

        Assert.assertEquals(
            DatapointsValue(datapointsFiltered, DataType.CATEGORICAL),
            output
        )

    }

    @Test
    fun mergeFunctionTest() {
        val d1 = someDataRandom()
        val d2 = someDataRandom()
        val d3 = someDataRandom()

        val expected = (d1 + d2 + d3).sortedBy { it.timestamp }

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

}