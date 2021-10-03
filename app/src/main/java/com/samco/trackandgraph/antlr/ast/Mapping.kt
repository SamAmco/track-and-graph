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

import org.antlr.v4.runtime.ParserRuleContext
import org.antlr.v4.runtime.Token
import org.antlr.v4.runtime.Token.EOF
import com.samco.trackandgraph.antlr.generated.TnG2Parser.*

interface ParseTreeToAstMapper<in PTN : ParserRuleContext, out ASTN : Node> {
    fun map(parseTreeNode: PTN) : ASTN
}

fun DatatransformationFunctionContext.toAst(considerPosition: Boolean = true) : DatatransformationFunction = DatatransformationFunction(this.line().map { it.statement().toAst(considerPosition) }, toPosition(considerPosition))

fun Token.startPoint() = Point(line, charPositionInLine)

fun Token.endPoint() = Point(line, charPositionInLine + (if (type == EOF) 0 else text.length))

fun ParserRuleContext.toPosition(considerPosition: Boolean) : Position? {
    return if (considerPosition) Position(start.startPoint(), stop.endPoint()) else null
}

fun StatementContext.toAst(considerPosition: Boolean = false) : Statement = when (this) {
    is VarDeclarationStatementContext -> VarDeclaration(varDeclaration().assignment().ID().text, varDeclaration().assignment().expression().toAst(considerPosition), toPosition(considerPosition))
    is AssignmentStatementContext -> Assignment(assignment().ID().text, assignment().expression().toAst(considerPosition), toPosition(considerPosition))
    is PrintStatementContext -> Print(print().expression().toAst(considerPosition), toPosition(considerPosition))
    else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
}

fun ExpressionContext.toAst(considerPosition: Boolean = false) : Expression = when (this) {
    is BinaryOperationContext -> toAst(considerPosition)
    is IntLiteralContext -> IntLit(text, toPosition(considerPosition))
    is DecimalLiteralContext -> DecLit(text, toPosition(considerPosition))
    is StringLiteralContext -> StringLit(text.replace("\"", ""), toPosition(considerPosition))
    is TimePeriodContext -> TimeperiodLit(this.getChild(0)!!, toPosition(considerPosition))
    is ParenExpressionContext -> expression().toAst(considerPosition)
    is VarReferenceContext -> VarReference(text, toPosition(considerPosition))
//    is TypeConversionContext -> TypeConversion(expression().toAst(considerPosition), targetType.toAst(considerPosition), toPosition(considerPosition))
    is FunctionCallContext -> FunctionCall(this.functionName.text, this.args.toAst())

    else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
}

//fun TypeContext.toAst(considerPosition: Boolean = false) : Type = when (this) {
//    is IntegerContext -> IntType(toPosition(considerPosition))
//    is DecimalContext -> DecimalType(toPosition(considerPosition))
//    else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
//}

fun BinaryOperationContext.toAst(considerPosition: Boolean = false) : Expression = when (operator.text) {
    "+" -> SumExpression(left.toAst(considerPosition), right.toAst(considerPosition), toPosition(considerPosition))
    "-" -> SubtractionExpression(left.toAst(considerPosition), right.toAst(considerPosition), toPosition(considerPosition))
    "*" -> MultiplicationExpression(left.toAst(considerPosition), right.toAst(considerPosition), toPosition(considerPosition))
    "/" -> DivisionExpression(left.toAst(considerPosition), right.toAst(considerPosition), toPosition(considerPosition))
    else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
}

fun ArgumentListContext.toAst(considerPosition: Boolean = false) : List<Expression> {
    if (this.childCount == 0) {
        return emptyList()
    }
    val expressions = this.children.filter { it.text != "," }.map { it -> (it as ExpressionContext).toAst() }
    return expressions
}

//class SandyParseTreeToAstMapper : ParseTreeToAstMapper<DatatransformationFunctionContext, DatatransformationFunction> {
//    override fun map(parseTreeNode: DatatransformationFunctionContext): DatatransformationFunction = parseTreeNode.toAst()
//}
