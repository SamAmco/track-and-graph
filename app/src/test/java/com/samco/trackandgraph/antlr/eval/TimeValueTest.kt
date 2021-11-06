package com.samco.trackandgraph.antlr.eval

import com.samco.trackandgraph.antlr.evaluation.EvaluationModel
import com.samco.trackandgraph.antlr.evaluation.NumberValue
import com.samco.trackandgraph.antlr.evaluation.TimeValue
import org.junit.Assert
import org.junit.Test
import org.threeten.bp.Duration
import org.threeten.bp.Period

class TimeValueTest {

    @Test
    fun someTimeMaths() {
        val evaluationModel = EvaluationModel()

        val code = """var a = WEEK * 2
                     |var b = WEEK + WEEK
                     |var c = WEEK * 4
                     |c = c / 2""".trimMargin("|")

        val context = evaluationModel.run(code)

        Assert.assertEquals(context["a"], context["b"])
        Assert.assertEquals(context["a"], context["c"])
        val outA = context["a"] as TimeValue
        val outB = context["b"] as TimeValue
        val outC = context["c"] as TimeValue
        Assert.assertTrue(outA.temporalAmount is Period)
        Assert.assertTrue(outB.temporalAmount is Period)
        Assert.assertTrue(outC.temporalAmount is Duration) // when we divide we convert, still equal amount of time
    }

    @Test
    fun periodToDurationTest() {
        val evaluationModel = EvaluationModel()

        val code = """var a = DAY + MONTH
                     |var b = a * 2
                     |var c = b / 2""".trimMargin("|")

        val context = evaluationModel.run(code)

        val outA = context["a"] as TimeValue
        val outB = context["b"] as TimeValue
        val outC = context["c"] as TimeValue
        Assert.assertTrue(outA.temporalAmount is Period)
        Assert.assertTrue(outB.temporalAmount is Period)
        Assert.assertTrue(outC.temporalAmount is Duration) // when we divide we convert, still equal amount of time

    }
}