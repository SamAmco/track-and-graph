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

//import me.tomassetti.sandy.parsing.SandyParserFacade
//import kotlin.test.assertEquals
//import com.samco.trackandgraph.antlr.generated.SandyParserFacade
import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionParserFacade
import org.junit.Assert.assertEquals
import org.junit.Test as test

class ValidationTest {

    @test fun duplicateVar() {
        val errors = DatatransformationFunctionParserFacade.parse("""var a = 1
                                               |var a =2""".trimMargin("|")).errors
        assertEquals(listOf(Error("A variable named 'a' has been already declared at Line 1, Column 0",
            Position(Point(2,0), Point(2,8) ))), errors)
//        assertEquals(listOf(Error("A variable named 'a' has been already declared at Line 1, Column 0")), errors)
    }

    @test fun unexistingVarReference() {
        val errors = DatatransformationFunctionParserFacade.parse("var a = b + 2").errors
        assertEquals(listOf(Error("There is no variable named 'b'",
            Position(Point(1,8), Point(1,9)))), errors)
//        assertEquals(listOf(Error("There is no variable named 'b'")), errors)
    }

    @test fun varReferenceBeforeDeclaration() {
        val errors = DatatransformationFunctionParserFacade.parse("""var a = b + 2
                                               |var b = 2""".trimMargin("|")).errors
        assertEquals(listOf(Error("You cannot refer to variable 'b' before its declaration",
            Position(Point(1,8), Point(1,9)))), errors)
//        assertEquals(listOf(Error("You cannot refer to variable 'b' before its declaration")), errors)
    }

    @test fun unexistingVarAssignment() {
        val errors = DatatransformationFunctionParserFacade.parse("a = 3").errors
        assertEquals(listOf(Error("There is no variable named 'a'",
            Position(Point(1,0), Point(1,5)))), errors)
//        assertEquals(listOf(Error("There is no variable named 'a'")), errors)
    }

    @test fun varAssignmentBeforeDeclaration() {
        val errors = DatatransformationFunctionParserFacade.parse("""a = 1
                                               |var a =2""".trimMargin("|")).errors
        assertEquals(listOf(Error("You cannot refer to variable 'a' before its declaration",
            Position(Point(1,0), Point(1,5)))), errors)
//        assertEquals(listOf(Error("You cannot refer to variable 'a' before its declaration")), errors)

    }

}
