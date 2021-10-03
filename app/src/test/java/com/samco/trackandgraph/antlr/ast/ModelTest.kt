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

//import com.samco.trackandgraph.antlr.generated.
import org.junit.Assert.assertEquals
//import com.samco.trackandgraph.antlr.generated.SandyFile
//import com.samco.trackandgraph.antlr.generated.VarDeclaration
//import me.tomassetti.sandy.parsing.SandyAntlrParserFacade
import java.util.*
//import kotlin.test.assertEquals
import org.junit.Test as test

class ModelTest {

    @test fun transformVarName() {
        val startTree = DatatransformationFunction(listOf(
                VarDeclaration("A", IntLit("10")),
                Assignment("A", IntLit("11")),
                Print(VarReference("A"))
        ))
        val expectedTransformedTree = DatatransformationFunction(listOf(
                VarDeclaration("B", IntLit("10")),
                Assignment("B", IntLit("11")),
                Print(VarReference("B"))
        ))
        assertEquals(expectedTransformedTree, startTree.transform {
            when (it) {
                is VarDeclaration -> VarDeclaration("B", it.value)
                is VarReference -> VarReference("B")
                is Assignment -> Assignment("B", it.value)
                else -> it
            }
        })
    }

    fun toAst(code: String) : DatatransformationFunction = DatatransformationFunctionAntlrParserFacade.parse(code).root!!.toAst()

    @test fun processAllVarDeclarations() {
        val ast = toAst("""var a = 1
                          |a = 2 * 5
                          |var b = a
                          |print(b)
                          |var c = b * b""".trimMargin("|"))
        val varDeclarations = LinkedList<String>()
        ast.specificProcess(VarDeclaration::class.java, { varDeclarations.add(it.varName) })
        assertEquals(listOf("a", "b", "c"), varDeclarations)
    }

}
