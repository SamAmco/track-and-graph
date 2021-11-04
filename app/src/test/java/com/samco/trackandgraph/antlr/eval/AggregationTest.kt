package com.samco.trackandgraph.antlr.eval

import com.samco.trackandgraph.antlr.evaluation.AggregationEnum
import com.samco.trackandgraph.antlr.evaluation.AggregationEnumValue
import com.samco.trackandgraph.antlr.evaluation.DatapointsValue
import com.samco.trackandgraph.antlr.evaluation.EvaluationModel
import com.samco.trackandgraph.antlr.someDataAllTen
import org.junit.Assert
import org.junit.Test

class AggregationTest {

    @Test
    fun aggregationEnumTest() {
        val evaluationModel = EvaluationModel()

        val code = "var b = MIN"
        val context = evaluationModel.run(code)

        val output = context["b"] as AggregationEnumValue
        Assert.assertEquals(output, AggregationEnumValue(AggregationEnum.MIN))

    }

    /**
     * Code copied from RawAggregatedDatapoints.kt
     */
    fun median_internal(list: List<Double>): Double {
        val sorted = list.sortedBy { it }
        return when (sorted.size % 2) {
            1 -> return sorted[sorted.size.div(2)]
            0 -> return sorted.subList(sorted.size.div(2)-1, sorted.size.div(2)+1).average() // sublist 2nd arg is exclusive
            else -> throw Exception("unreachable. positive int modulo 2 is always 1 or 0")
        }
    }

    @Test
    fun medianInternalTest() {
        Assert.assertEquals(5.0, median_internal(listOf(5.0,15.0,-4.0)), 0.001)

        Assert.assertEquals(7.5, median_internal(listOf(5.0,15.0,-4.0, 10.0)), 0.001)
    }
}