//   Copyright 2021 Federico Tomassetti
//
//   Licensed under the Apache License, Version 2.0 (the "License");
//   you may not use this file except in compliance with the License.
//   You may obtain a copy of the License at
//
//       http://www.apache.org/licenses/LICENSE-2.0
//
//   Unless required by applicable law or agreed to in writing, software
//   distributed under the License is distributed on an "AS IS" BASIS,
//   WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
//   See the License for the specific language governing permissions and
//   limitations under the License.

//  Modified


package com.samco.trackandgraph.antlr.ast

import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionAntlrParserFacade
import org.junit.Assert.assertEquals
//import kotlin.test.assertEquals
import org.junit.Test as test

class MappingTest {

    @test fun mapSimpleFileWithoutPositions() {
        val code = """var a = 1 + 2
                     |a = 7 * (2 / 3)""".trimMargin("|")
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst(considerPosition = false)
        val expectedAst = DatatransformationFunction(listOf(
                VarDeclaration("a", SumExpression(IntLit("1"), IntLit("2"))),
                Assignment("a", MultiplicationExpression(
                        IntLit("7"),
                        DivisionExpression(
                                IntLit("2"),
                                IntLit("3"))))))
        assertEquals(expectedAst, ast)
    }

    @test fun mapSimpleFileWithPositions() {
        val code = """var a = 1 + 2
                     |a = 7 * (2 / 3)""".trimMargin("|")
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst(considerPosition = true)
        val expectedAst = DatatransformationFunction(listOf(
                VarDeclaration("a",
                        SumExpression(
                                IntLit("1", pos(1,8,1,9)),
                                IntLit("2", pos(1,12,1,13)),
                                pos(1,8,1,13)),
                        pos(1,0,1,13)),
                Assignment("a",
                        MultiplicationExpression(
                            IntLit("7", pos(2,4,2,5)),
                            DivisionExpression(
                                    IntLit("2", pos(2,9,2,10)),
                                    IntLit("3", pos(2,13,2,14)),
                                    pos(2,9,2,14)),
                            pos(2,4,2,15)),
                        pos(2,0,2,15))),
                pos(1,0,2,15))
        assertEquals(expectedAst, ast)
    }

//    @test fun mapCastInt() {
//        val code = "a = 7 as Int"
//        val ast = SandyAntlrParserFacade.parse(code).root!!.toAst(considerPosition = false)
//        val expectedAst = DatatransformationFunction(listOf(Assignment("a", TypeConversion(IntLit("7"), IntType()))))
//        assertEquals(expectedAst, ast)
//    }

//    @test fun mapCastDecimal() {
//        val code = "a = 7 as Decimal"
//        val ast = SandyAntlrParserFacade.parse(code).root!!.toAst(considerPosition = false)
//        val expectedAst = DatatransformationFunction(listOf(Assignment("a", TypeConversion(IntLit("7"), DecimalType()))))
//        assertEquals(expectedAst, ast)
//    }

    @test fun mapPrint() {
        val code = "print(a)"
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst(considerPosition = false)
        val expectedAst = DatatransformationFunction(listOf(Print(VarReference("a"))))
        assertEquals(expectedAst, ast)
    }

    @test fun mapFunction() {
        val code = "a = Fun(7, b)"
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst(considerPosition = false)
        val expectedAst = DatatransformationFunction(listOf(Assignment("a",
            FunctionCall("Fun", listOf(IntLit("7"), VarReference("b"))))))
        assertEquals(expectedAst, ast)
    }

    @test fun mapFunctionNoArgs() {
        val code = "a = FunNoArgs()"
        val ast = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst(considerPosition = false)
        val expectedAst = DatatransformationFunction(listOf(Assignment("a",
            FunctionCall("FunNoArgs", emptyList() ))))
        assertEquals(expectedAst, ast)
    }

}
