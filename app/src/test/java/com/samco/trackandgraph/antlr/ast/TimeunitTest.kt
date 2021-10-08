package com.samco.trackandgraph.antlr.ast

import com.samco.trackandgraph.antlr.evaluation.EvaluationModel
import com.samco.trackandgraph.antlr.evaluation.TimeValue
import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionAntlrParserFacade
import org.junit.Assert
import org.junit.Test
import org.threeten.bp.Duration

class TimeunitTest {
//    @Test
//    fun timeunitTest() {
//        val code = """ a = MONTHLY """
//        val ast = SandyAntlrParserFacade.parse(code).root!!.toAst(considerPosition = false)
//        val expectedAst = SandyFile(listOf(
//            Assignment("a", TimeunitLit(TnG2Parser.MonthlyContext(null))
//            )
//        ))
//        Assert.assertEquals(expectedAst, ast)
//    }

    @Test
    fun evalSimpleAssignment() {
        val evaluationModel = EvaluationModel()

        val code = "var a = DAY * 5"

        val context = evaluationModel.run(code)

        Assert.assertEquals(TimeValue(Duration.ofDays(5)), context["a"])


    }
}