package com.samco.trackandgraph.antlr.ast

import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionAntlrParserFacade
import org.junit.Assert
import org.junit.Test

class StringTest {

    @Test
    fun stringTest() {
        val code = """a = "string" """
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst(considerPosition = false)
        val expectedAst = DatatransformationFunction(listOf(
            Assignment("a",
            StringLit("string")
            )
        ))
        Assert.assertEquals(expectedAst, ast)
    }
}