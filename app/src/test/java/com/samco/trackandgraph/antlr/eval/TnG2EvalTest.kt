package com.samco.trackandgraph.antlr.eval

import com.samco.trackandgraph.antlr.ast.toAst
import com.samco.trackandgraph.antlr.evaluation.EvaluationModel
import com.samco.trackandgraph.antlr.evaluation.NumberValue
import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionAntlrParserFacade
import org.junit.Assert.assertEquals
import org.junit.Test

class TnG2EvalTest {

    @Test
    fun evalSimpleAssignment() {
        val evaluationModel = EvaluationModel()

        val code = "var a = 1 + 1"

        val context = evaluationModel.run(code)

        assertEquals(NumberValue(2), context["a"])


    }

    @Test
    fun evalComplexAssignment() {
        val evaluationModel = EvaluationModel()

        val code = "var a = 0 + 5 - 5 + 7 * (2 / 3)"
        val context = evaluationModel.run(code)

        assertEquals(NumberValue(0 + 5 - 5 + 7.0 * (2.0 / 3.0)), context["a"] )


    }

    @Test
    fun evalMulBeforeSum() {
        // This works (nice) but I am not 100% sure why. But since I'm reusing code I assume the
        // original author implemented it or maybe it's an antlr feature.
        val evaluationModel = EvaluationModel()

        val code = "var a = 2 + 2 * 2"

        val context = evaluationModel.run(code)

        assertEquals(NumberValue(2+2*2.0), context["a"] )
        assertEquals(NumberValue(6.0), context["a"] )

    }

    @Test
    fun evalMulBeforeSum2() {
        val evaluationModel = EvaluationModel()

        val code = "var a = 2 * 2 + 2"

        val context = evaluationModel.run(code)

        assertEquals(NumberValue(2*2+2.0), context["a"] )
        assertEquals(NumberValue(6.0), context["a"] )

    }

    @Test
    fun evalBrackets() {
        val evaluationModel = EvaluationModel()

        val code = "var a = 2 * (2 + 2)"

        val context = evaluationModel.run(code)

        assertEquals(NumberValue(2*(2+2.0)), context["a"] )
        assertEquals(NumberValue(8.0), context["a"] )

    }

    @Test
    fun evalReassignment() {
        val evaluationModel = EvaluationModel()

        val code = """var a = 1
                     |a = 2""".trimMargin("|")

        val context = evaluationModel.run(code)

        assertEquals(NumberValue(2), context["a"])
    }
}