package com.samco.trackandgraph.antlr.eval

import com.samco.trackandgraph.antlr.ast.toAst
import com.samco.trackandgraph.antlr.evaluation.DatapointsValue
import com.samco.trackandgraph.antlr.evaluation.EvaluationModel
import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionAntlrParserFacade
import com.samco.trackandgraph.antlr.someDataAllTen
import org.junit.Assert
import org.junit.Test

class FunctionsOnDataTest {

//    @Test
//    fun filterTest() {
//        val datapoints = someDataAllTen()
//
//        val evaluationModel = EvaluationModel()
//
//        val code = "var b = Filter(data, 5)"
//        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst()
//
//        val context = evaluationModel.evaluate(ast, mapOf("data" to datapoints))
//
//        val processedData = context["b"] as DatapointsValue
//        Assert.assertEquals(datapoints.map { dp -> dp.copy(value=dp.value+5)}, processedData.datapoints)
//
//    }
}