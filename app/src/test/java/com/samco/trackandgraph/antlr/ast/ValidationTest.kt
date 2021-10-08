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

import com.samco.trackandgraph.antlr.evaluation.DoubleDeclarationError
import com.samco.trackandgraph.antlr.evaluation.NotDeclaredError
import com.samco.trackandgraph.antlr.evaluation.ReferencedBeforeDeclarationError
import com.samco.trackandgraph.antlr.evaluation.UnexistingVariableError
import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionParserFacade
import org.junit.Assert.assertEquals
import kotlin.reflect.typeOf
import org.junit.Test as test

class ValidationTest {

    @test fun duplicateVar() {
        val errors = DatatransformationFunctionParserFacade.parse("""var a = 1
                                               |var a =2""".trimMargin("|")).errors

        assertEquals(1, errors.size)
        assertEquals(DoubleDeclarationError::class.java, errors[0].javaClass)
        assertEquals(Position(Point(2,0), Point(2,8)), errors[0].position )
    }

    @test fun unexistingVarReference() {
        val errors = DatatransformationFunctionParserFacade.parse("var a = b + 2").errors

        assertEquals(1, errors.size)
        assertEquals(UnexistingVariableError::class.java, errors[0].javaClass)
        assertEquals(Position(Point(1,8), Point(1,9)), errors[0].position )
    }

    @test fun varReferenceBeforeDeclaration() {
        val errors = DatatransformationFunctionParserFacade.parse("""var a = b + 2
                                               |var b = 2""".trimMargin("|")).errors

        assertEquals(1, errors.size)
        assertEquals(ReferencedBeforeDeclarationError::class.java, errors[0].javaClass)
        assertEquals(Position(Point(1,8), Point(1,9)), errors[0].position )
    }

    @test fun unexistingVarAssignment() {
        val errors = DatatransformationFunctionParserFacade.parse("a = 3").errors

        assertEquals(1, errors.size)
        assertEquals(NotDeclaredError::class.java, errors[0].javaClass)
        assertEquals(Position(Point(1,0), Point(1,5)), errors[0].position )
    }

    @test fun varAssignmentBeforeDeclaration() {
        val errors = DatatransformationFunctionParserFacade.parse("""a = 1
                                               |var a =2""".trimMargin("|")).errors

        assertEquals(1, errors.size)
        assertEquals(ReferencedBeforeDeclarationError::class.java, errors[0].javaClass)
        assertEquals(Position(Point(1,0), Point(1,5)), errors[0].position )

    }

}
