package com.samco.trackandgraph.antlr.evaluation

import com.samco.trackandgraph.antlr.ast.DatatransformationFunctionError
import com.samco.trackandgraph.antlr.ast.Point
import com.samco.trackandgraph.antlr.ast.Position

private fun emptyPosition() : Position {
    return Position(Point(0, 0), Point(0, 0))
}

fun throwUnsupportedArgumentTypeError(functionName: String, main: Value, other: Value, allowedTypes: String) : Nothing {
    throw UnsupportedArgumentTypeError(
        functionName,
        listOf(main::class.simpleName!!, other::class.simpleName!!),
        allowedTypes, emptyPosition()
    )

}

class DoubleDeclarationError(val varName: String, position: Position) :
    DatatransformationFunctionError("Variable '$varName' was already declared!", position)

class NotDeclaredError(val varName: String, position: Position) :
    DatatransformationFunctionError("Variable '$varName' was never declared!", position)

class UnexistingVariableError(val varName: String, position: Position) :
    DatatransformationFunctionError("There is no variable named '$varName'!", position)

class ReferencedBeforeDeclarationError(val varName: String, position: Position) :
    DatatransformationFunctionError("You cannot refer to variable '$varName' before its declaration", position)

class UnsupportedArgumentTypeError(val functionName: String, val argumentTypes: List<String>, val allowedTypes: String, position: Position) :
    DatatransformationFunctionError("Function '$functionName' expected argument(s) of type $allowedTypes, got $argumentTypes", position)


// These Errors use emptyPosition bc they occur during computation. They are being catched, their position added and rethrown
class WrongDatatypeError(actual: DataType, expected: List<DataType>) :
        DatatransformationFunctionError("The data has to be of type ${expected.map { it.name }}, but was of type ${actual.name}",
        position= emptyPosition()
        )

class UnkownFunctionName(functionName: String) :
        DatatransformationFunctionError("Unknown function '$functionName'", emptyPosition())

class ArgMissingError(message: String) :
        DatatransformationFunctionError(message, emptyPosition())

class ArgWrongTypeError(functionName: String, actual: String, expected: String) :
        DatatransformationFunctionError("Function $functionName expected an arg of type $expected, but got $actual",
            emptyPosition()
        )




//class UnsupportedArgumentTypeError(val argumentName: String, val argumentType: String, val allowedTypes: String, position: Position) :
//        ThrowableError("Expected argument of type $allowedTypes, got $argumentType [$argumentName]", position)
