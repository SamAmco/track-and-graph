package com.samco.trackandgraph.antlr.evaluation

import com.samco.trackandgraph.R
import com.samco.trackandgraph.antlr.ast.DatatransformationFunctionError
import com.samco.trackandgraph.antlr.ast.Point
import com.samco.trackandgraph.antlr.ast.Position
import kotlin.reflect.KClass
import kotlin.reflect.KFunction2

private fun emptyPosition() : Position {
    return Position(Point(0, 0), Point(0, 0))
}

fun throwUnsupportedArgumentTypeErrorBinaryOperation(
    functionName: String,
    main: Value,
    other: Value,
    allowedTypes: List<KClass<*>>
): Nothing {
    throw UnsupportedArgumentTypeErrorBinaryOperation(
        functionName,
        other::class,
        allowedTypes, emptyPosition()
    )
}

class DoubleDeclarationError(val varName: String, position: Position) :
    DatatransformationFunctionError(
        "Variable '$varName' was already declared!", position,
        R.string.errormsg_double_declaration, varName
    )

class NotDeclaredError(val varName: String, position: Position) :
    DatatransformationFunctionError(
        "Variable '$varName' was never declared!", position,
        R.string.errormsg_not_declared, varName
    )

class UnexistingVariableError(val varName: String, position: Position) :
    DatatransformationFunctionError(
        "There is no variable named '$varName'!", position,
        R.string.errormsg_unexisting_var, varName
    )

class ReferencedBeforeDeclarationError(val varName: String, position: Position) :
    DatatransformationFunctionError(
        "You cannot refer to variable '$varName' before its declaration", position,
        R.string.errormsg_reference_before_declaration, varName
    )

class UnsupportedArgumentTypeErrorBinaryOperation(
    val functionName: String,
    val argumentType: KClass<*>,
    val allowedTypes: List<KClass<*>>,
    position: Position
) :
    DatatransformationFunctionError(
        "Function '$functionName' expected argument(s) of type $allowedTypes, got $argumentType",
        position,
        { getString: KFunction2<Int, Array<out Any?>, String> ->
            BetterGetString(getString)(
                R.string.errormsg_unsupported_argumenttype,
                functionName, allowedTypes,
                argumentType
            )
        }
    )


// These Errors use emptyPosition bc they occur during computation. They are being catched, their position added and rethrown
class WrongArgDatatypeError(
    functionName: String,
    index: Int,
    actual: KClass<*>,
    expected: List<KClass<*>>
) :
    DatatransformationFunctionError(
        "Argument ${index + 1} of function '$functionName' has to be of type ${expected.map { it.simpleName }}, but was of type ${actual.simpleName}",
        position = emptyPosition(),
        { getString: KFunction2<Int, Array<out Any?>, String> ->
            BetterGetString(getString)(
                R.string.errormsg_arg_wrong_type,
                index + 1, functionName,
                expected,
                actual
            )
        }
    )

class UnknownFunctionName(functionName: String) :
    DatatransformationFunctionError(
        "Unknown function '$functionName'", emptyPosition(),
        R.string.errormsg_unknown_function, functionName
    )

class ArgMissingError(functionName: String, index: Int, type: KClass<*>) :
    DatatransformationFunctionError("Missing argument for function '$functionName'. Expected an argument of type ${
        type.simpleName
    } at position ${index + 1}",
        emptyPosition(),
        { getString: KFunction2<Int, Array<out Any?>, String> ->
            BetterGetString(getString)(
                R.string.errormsg_arg_missing,
                functionName, type, index + 1
            )
        }
    )

// TODO: Maybe have function signatures ready to print out?
class TooManyArgsError(functionName: String, expectedNumber: Number, actualNumber: Number) :
    DatatransformationFunctionError("Function '$functionName' got $actualNumber argument, but expected only $expectedNumber.",
        emptyPosition(),
        R.string.errormsg_too_many_args,
        functionName, actualNumber, expectedNumber)


class DatapointsWrongTypeError(expectedTypes: List<DataType>, actualType: DataType) :
    DatatransformationFunctionError("Datapoints had the wrong type! Expected $expectedTypes, but got $actualType.",
    emptyPosition(),
    R.string.errormsg_datapoints_wrong_type,
    expectedTypes, actualType)



/**
 * This class wraps the getString function handed down from the app-context.
 * It has the following features for the arguments:
 *  - Converts KClass types to their localized names
 *  - Concatenates Lists with " or " (localized)
 *  - uses vararg instead of Array<Any>
 */
class BetterGetString(val getString: KFunction2<Int, Array<Any>, String>) {
    operator fun invoke(mainId: Int, vararg args: Any): String {
        val betterArgs = args.map {
            this.improveArg(it)
        }.toTypedArray()
        return getString(mainId, betterArgs)
    }

    private fun improveArg(arg: Any): Any {
        return when (arg) {
            is KClass<*> -> getString(valueClassToStringResId(arg), arrayOf())
            is DataType -> arg.toLocalizedString(getString)
            
            // This case recursively calls improveArg on all list components and then concatenates them with " or "
            is List<*> -> {
                arg.filterNotNull().map {
                    this.improveArg(it)
                }.joinToString(separator = " %s ".format(getString(R.string.or, arrayOf())))
            }

            else -> arg
        }
    }
}
