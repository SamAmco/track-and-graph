package com.samco.trackandgraph.antlr.eval

import com.samco.trackandgraph.antlr.ast.toAst
import com.samco.trackandgraph.antlr.evaluation.*
import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionAntlrParserFacade
import org.junit.Assert.assertEquals
import org.junit.Test

class ErrorTest {

    @Test
    fun notDeclaredTest() {
        val evaluationModel = EvaluationModel()

        val code = "a = 1"
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst()

        try {
            evaluationModel.evaluate(ast)

            // should never get here bc of the error
            assert(false)
        } catch (e: NotDeclaredError) {
            assertEquals("a", e.varName)
        } catch (e: Exception){
            // should never get here bc there is no other error
            assert(false)
        }
    }

    @Test
    fun doubleDeclaredTest() {
        val evaluationModel = EvaluationModel()

        val code = """var a = 1
                     |var a =2""".trimMargin("|")
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst()

        try {
            evaluationModel.evaluate(ast)

            // should never get here bc of the error
            assert(false)
        } catch (e: DoubleDeclarationError) {
            assertEquals("a", e.varName)
            assertEquals(2, e.position.start.line) // locates error on second line
        } catch (e: Exception){
            // should never get here bc there is no other error
            assert(false)
        }
    }

//    @Test
//    fun argTypeTest() {
//        val datapoints = someData()
//
//        val evaluationModel = EvaluationModel()
//
//        val dataContext = mapOf("data" to datapoints)
//
//        val code = "var a = 5 * data"
//        val ast = SandyAntlrParserFacade.parse(code).root!!.toAst()
//
//
//        val context = evaluationModel.evaluate(ast, dataContext)
//
//        assertEquals(NumberValue(1), context["a"])
//        assertEquals(DatapointsValue(datapoints), context["data"])
//
//    }
}