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


package com.samco.trackandgraph.antlr

import com.samco.trackandgraph.antlr.generated.TnG2Lexer
import com.samco.trackandgraph.antlr.generated.TnG2Parser
import org.antlr.v4.runtime.ANTLRInputStream
import org.antlr.v4.runtime.CommonTokenStream
import java.io.StringReader
import java.util.*
//import kotlin.test.assertEquals

class TnG2ParserTest {

    fun lexerForCode(code: String) = TnG2Lexer(ANTLRInputStream(StringReader(code)))

    fun lexerForResource(resourceName: String) = TnG2Lexer(ANTLRInputStream(this.javaClass.getResourceAsStream("/${resourceName}.TnG2")))

    fun tokens(lexer: TnG2Lexer): List<String> {
        val tokens = LinkedList<String>()
        do {
           val t = lexer.nextToken()
            when (t.type) {
                -1 -> tokens.add("EOF")
                else -> if (t.type != TnG2Lexer.WS) tokens.add(lexer.ruleNames[t.type - 1])
            }
        } while (t.type != -1)
        return tokens
    }

    fun parse(lexer: TnG2Lexer) : TnG2Parser.DatatransformationFunctionContext = TnG2Parser(CommonTokenStream(lexer)).datatransformationFunction()

    fun parseResource(resourceName: String) : TnG2Parser.DatatransformationFunctionContext = TnG2Parser(CommonTokenStream(lexerForResource(resourceName))).datatransformationFunction()

//    @org.junit.Test fun parseAdditionAssignment() {
//        assertEquals(
//"""TnG2File
//  Line
//    AssignmentStatement
//      Assignment
//        T[a]
//        T[=]
//        BinaryOperation
//          IntLiteral
//            T[1]
//          T[+]
//          IntLiteral
//            T[2]
//    T[<EOF>]
//""",
//                toParseTree(parseResource("addition_assignment")).multiLineString())
//    }
//
//    @org.junit.Test fun parseSimplestVarDecl() {
//        assertEquals(
//"""TnG2File
//  Line
//    VarDeclarationStatement
//      VarDeclaration
//        T[var]
//        Assignment
//          T[a]
//          T[=]
//          IntLiteral
//            T[1]
//    T[<EOF>]
//""",
//                toParseTree(parseResource("simplest_var_decl")).multiLineString())
//    }
//
//    @org.junit.Test fun parsePrecedenceExpressions() {
//        assertEquals(
//"""TnG2File
//  Line
//    VarDeclarationStatement
//      VarDeclaration
//        T[var]
//        Assignment
//          T[a]
//          T[=]
//          BinaryOperation
//            BinaryOperation
//              IntLiteral
//                T[1]
//              T[+]
//              BinaryOperation
//                IntLiteral
//                  T[2]
//                T[*]
//                IntLiteral
//                  T[3]
//            T[-]
//            IntLiteral
//              T[4]
//    T[<EOF>]
//""",
//                toParseTree(parseResource("precedence_expression")).multiLineString())
//    }


}
