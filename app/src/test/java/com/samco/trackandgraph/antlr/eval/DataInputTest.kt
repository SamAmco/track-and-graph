package com.samco.trackandgraph.antlr.eval

import com.samco.trackandgraph.antlr.ast.toAst
import com.samco.trackandgraph.antlr.evaluation.DataType
import com.samco.trackandgraph.antlr.evaluation.DatapointsValue
import com.samco.trackandgraph.antlr.evaluation.EvaluationModel
import com.samco.trackandgraph.antlr.evaluation.NumberValue
import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionAntlrParserFacade
import com.samco.trackandgraph.antlr.someData
import org.junit.Assert.assertEquals
import org.junit.Test

class DataInputTest {

    @Test
    fun dataInputTest() {
        val datapoints = someData()

        val evaluationModel = EvaluationModel()

        val code = "var a = 1"
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst()

        val context = evaluationModel.evaluate(ast, mapOf("data" to datapoints))

        assertEquals(NumberValue(1), context["a"])
        assertEquals(DatapointsValue(datapoints, DataType.NUMERICAL), context["data"])

    }


}