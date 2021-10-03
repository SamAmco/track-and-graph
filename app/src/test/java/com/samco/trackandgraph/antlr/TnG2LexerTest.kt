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


import com.samco.trackandgraph.antlr.generated.*
import org.antlr.v4.runtime.CharStreams
import org.junit.Assert.assertEquals
import java.util.*
//import java.util.*
//import kotlin.Test.assertEquals
import org.junit.Test


class TnG2LexerTest {

    fun lexerForCode(code: String) = TnG2Lexer(CharStreams.fromString(code))

    fun tokens(lexer: TnG2Lexer): List<String> {
        val tokens = LinkedList<String>()
        do {
           val t = lexer.nextToken()
            when (t.type) {
                -1 -> tokens.add("EOF")
                else -> if (t.type != TnG2Lexer.WS) tokens.add(TnG2Lexer.VOCABULARY.getSymbolicName(t.type))
            }
        } while (t.type != -1)
        return tokens
    }

    @Test
    fun parseVarDeclarationAssignedAnIntegerLiteral() {
        assertEquals(listOf("VAR", "ID", "ASSIGN", "INTLIT", "EOF"),
                tokens(lexerForCode("var a = 1")))
    }

    @Test fun parseVarDeclarationAssignedADecimalLiteral() {
        assertEquals(listOf("VAR", "ID", "ASSIGN", "DECLIT", "EOF"),
                tokens(lexerForCode("var a = 1.23")))
    }

    @Test
    fun parseVarDeclarationAssignedASum() {
        assertEquals(listOf("VAR", "ID", "ASSIGN", "INTLIT", "PLUS", "INTLIT", "EOF"),
                tokens(lexerForCode("var a = 1 + 2")))
    }

    @Test fun parseMathematicalExpression() {
        assertEquals(listOf("INTLIT", "PLUS", "ID", "ASTERISK", "INTLIT", "DIVISION", "INTLIT", "MINUS", "INTLIT", "EOF"),
                tokens(lexerForCode("1 + a * 3 / 4 - 5")))
    }

    @Test fun parseMathematicalExpressionWithParenthesis() {
        assertEquals(listOf("INTLIT", "PLUS", "LPAREN", "ID", "ASTERISK", "INTLIT", "RPAREN", "MINUS", "DECLIT", "EOF"),
                tokens(lexerForCode("1 + (a * 3) - 5.12")))
    }

    @Test fun parseCast() {
        assertEquals(listOf("ID", "ASSIGN", "ID", "AS", "INT", "EOF"),
                tokens(lexerForCode("a = b as Int")))
    }

    @Test fun parseFunction() {
        assertEquals(listOf("ID", "ASSIGN", "FUNCTION_NAME", "LPAREN", "ID", "COMMA", "ID", "RPAREN", "EOF"),
            tokens(lexerForCode("a = Fun(a, b)")))
    }
}
