package com.samco.trackandgraph.antlr.evaluation


import com.samco.trackandgraph.antlr.ast.*
import com.samco.trackandgraph.antlr.generated.TnG2Parser
import com.samco.trackandgraph.database.entity.DataPoint
import org.antlr.v4.runtime.tree.ParseTree
import org.threeten.bp.Duration
//import java.lang.Exception
import kotlin.Exception
import kotlin.math.roundToLong


fun inferDatatype(data: List<DataPoint>) : DataType {
    return DataType.NUMERICAL
}

class EvaluationModel {

    fun evaluate(parsedTree: DatatransformationFunction, inputFeatures: Map<String, List<DataPoint>> = emptyMap<String, List<DataPoint>>()) : Map<String, Value> {
        var context : Map<String, Value> = inputFeatures.mapValues { list -> DatapointsValue(list.value, inferDatatype(list.value)) }
        for (statement in parsedTree.statements) {
            context = when (statement) {
                is VarDeclaration -> statement.evaluate(context)
                is Assignment -> statement.evaluate(context)
                is Print -> statement.evaluate(context)
                else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
            }

        }

        return context

    }

}

abstract class Value {
    open fun _plus(other: Value) : Value = throw NotImplementedError()

    open fun _minus(other: Value) : Value = throw NotImplementedError()

    open fun _times(other: Value) : Value = throw NotImplementedError()

    open fun _div(other: Value) : Value = throw NotImplementedError()

    operator fun plus (other: Value) : Value{
        try {
            return this._plus(other)
        } catch (e: NotImplementedError) {
            throw Exception()
        }
    }

    operator fun minus (other: Value) : Value {
        try {
            return this._minus(other)
        } catch (e: NotImplementedError) {
            throw Exception()
        }
    }

    operator fun times(other: Value) : Value {
        try {
            return this._times(other)
        } catch (e: NotImplementedError) {
            throw Exception()
        }
    }

    operator fun div(other: Value) : Value {
        try {
            return this._div(other)
        } catch (e: NotImplementedError) {
            throw Exception()
        }
    }
}

private fun emptyPosition() : Position {
    return Position(Point(0,0), Point(0,0))
}

fun throwUnsupportedArgumentTypeError(functionName: String, main: Value, other: Value, allowedTypes: String) : Value {
    throw UnsupportedArgumentTypeError(functionName,
        listOf(main::class.simpleName!!, other::class.simpleName!!),
        "Numbers", emptyPosition())

}

class NumberValue(val number: Number) : Value() {
    fun toDouble() : Double = number.toDouble()

    override fun toString(): String {
        return "NumberValue(number=$number)"
    }

    override fun equals(other: Any?): Boolean {
        if (other is NumberValue) return this.number.toDouble() == other.number.toDouble()
        return super.equals(other)
    }

    override fun _plus(other: Value): Value {
        return when(other) {
            is NumberValue -> NumberValue(this.toDouble() + other.toDouble())
            else -> throwUnsupportedArgumentTypeError("PLUS", this, other, "Number")
        }
    }
    override fun _minus(other: Value): Value {
        return when(other) {
            is NumberValue -> NumberValue(this.toDouble() - other.toDouble())
            else -> throwUnsupportedArgumentTypeError("MINUS", this, other, "Number")
        }
    }
    override fun _times(other: Value): Value {
        return when(other) {
            is NumberValue -> NumberValue(this.toDouble() * other.toDouble())
            is TimeValue   -> other._times(this) //we have to know that this is not recursive (it isn't)
            else -> throwUnsupportedArgumentTypeError("TIMES", this, other, "Number, Timeinterval")
        }
    }
    override fun _div(other: Value): Value {
        return when(other) {
            is NumberValue -> NumberValue(this.toDouble() / other.toDouble())
            else -> throwUnsupportedArgumentTypeError("DIV", this, other, "Number")
        }
    }

}

class StringValue(val string: String): Value() {
    override fun toString(): String {
        return "StringValue(string=\"$string\""
    }

    override fun equals(other: Any?): Boolean {
        if (other is StringValue) return this.string == other.string
        return super.equals(other)
    }
}

class TimeValue(val duration: Duration) : Value() {
    constructor(context: ParseTree)  : this(when(context) {
        is TnG2Parser.SecondContext -> Duration.ofSeconds(1) // mostly for converting
        is TnG2Parser.MinuteContext -> Duration.ofMinutes(1) // mostly for converting
        is TnG2Parser.HourlyContext -> Duration.ofHours(1)    // mostly for converting, but mixed
        is TnG2Parser.DailyContext -> Duration.ofDays(1)
        is TnG2Parser.WeeklyContext -> Duration.ofDays(7)
        is TnG2Parser.MonthlyContext -> Duration.ofDays(31)
        is TnG2Parser.YearlyContext -> Duration.ofDays(365)
        else -> throw UnsupportedOperationException("Unexpected Context ${context::class.simpleName}!")})

    override fun equals(other: Any?): Boolean {
        if (other is TimeValue) return this.duration == other.duration
        return super.equals(other)
    }

    override fun _plus(other: Value): Value {
        return when(other) {
            is TimeValue ->TimeValue(this.duration + other.duration)
            else -> throwUnsupportedArgumentTypeError("PLUS", this, other, "Timeinterval")
        }
    }
    override fun _minus(other: Value): Value {
        return when(other) {
            is TimeValue -> TimeValue(this.duration - other.duration)
            else -> throwUnsupportedArgumentTypeError("MINUS", this, other, "Timeinterval")
        }
    }
    override fun _times(other: Value): Value {
        return when(other) {
            is NumberValue -> TimeValue(this.duration.multipliedBy(other.toDouble().roundToLong()))
            else -> throwUnsupportedArgumentTypeError("TIMES", this, other, "Number")
        }
    }
    override fun _div(other: Value): Value {
        return when(other) {
            is NumberValue -> TimeValue(this.duration.dividedBy(other.toDouble().roundToLong()))
            else -> throwUnsupportedArgumentTypeError("DIV", this, other, "Number")
        }
    }
}
enum class Regularity {
    NONE, DAILY, WEEKLY, MONTHLY, YEARLY
}

enum class DataType {
    NUMERICAL, TIME, CATEGORICAL,
}

class DatapointsValue(val datapoints: List<DataPoint>, val dataType: DataType, val regularity: Regularity = Regularity.NONE) : Value() {
    override fun equals(other: Any?): Boolean {
        if (other is DatapointsValue) return this.datapoints == other.datapoints
        return super.equals(other)
    }

    fun applyToAllPoints(function: (Double) -> Double, checkNumerical: Boolean = true) : DatapointsValue {
        if (checkNumerical && this.dataType != DataType.NUMERICAL) throw WrongDatatypeError(this.dataType, listOf(DataType.NUMERICAL))
        return DatapointsValue(this.datapoints.map { dp -> dp.copy(value=function(dp.value)) },
            dataType=this.dataType, regularity=this.regularity)
    }

    override fun _plus(other: Value): Value {
        return when(other) {
            is NumberValue -> applyToAllPoints( { it + other.toDouble() } )
            else -> throwUnsupportedArgumentTypeError("PLUS", this, other, "Number")
        }
    }
    override fun _minus(other: Value): Value {
        return when(other) {
            is NumberValue -> applyToAllPoints( { it - other.toDouble() } )
            else -> throwUnsupportedArgumentTypeError("MINUS", this, other, "Number")
        }
    }
    override fun _times(other: Value): Value {
        return when(other) {
            is NumberValue -> applyToAllPoints({ it * other.toDouble() } )
            else -> throwUnsupportedArgumentTypeError("TIMES", this, other, "Number")
        }
    }
    override fun _div(other: Value): Value {
        return when(other) {
            is NumberValue -> applyToAllPoints( { it / other.toDouble() } )
            else -> throwUnsupportedArgumentTypeError("DIV", this, other, "Number")
        }
    }

}

fun Expression.evaluate(context: Map<String, Value>) : Value {
    try {
        return when (this) {
            is IntLit -> NumberValue(this.value.toInt())
            is DecLit -> NumberValue(this.value.toDouble())
            is StringLit -> StringValue(this.value)
            is TimeperiodLit -> TimeValue(this.context)

            is MultiplicationExpression -> this.left.evaluate(context) * this.right.evaluate(context)
            is DivisionExpression       -> this.left.evaluate(context) / this.right.evaluate(context)
            is SumExpression            -> this.left.evaluate(context) + this.right.evaluate(context)
            is SubtractionExpression    -> this.left.evaluate(context) - this.right.evaluate(context)

            is FunctionCall             -> callFunction(functionName, args.map { it.evaluate(context) })

            is VarReference -> context.get(this.varName) ?: throw NotDeclaredError(this.varName, this.position!!)
            else -> throw UnsupportedOperationException(this.javaClass.canonicalName)
//            else -> throw UnsupportedArgumentTypeError(
//                this.toString(), this.javaClass.canonicalName!!,
//                "Number, Variable Reference, Arithmetic", this.position!!
//            )
        }
    } catch (e: ThrowableError) {
        e.position = this.position!!
        throw e
    }

}



fun VarDeclaration.evaluate(context: Map<String, Value>) : HashMap<String, Value> {
    val context = context.toMutableMap()

    if (context.containsKey(this.varName))
        throw DoubleDeclarationError(this.varName, this.position!!)

    context[this.varName] = this.value.evaluate(context as HashMap<String, Value>)

    return context
}

class DoubleDeclarationError(val varName: String, position: Position) :
    ThrowableError("Variable $varName was already declared!", position)

class NotDeclaredError(val varName: String, position: Position) :
    ThrowableError("Variable $varName was never declared!", position)

//class UnsupportedArgumentTypeError(val argumentName: String, val argumentType: String, val allowedTypes: String, position: Position) :
//        ThrowableError("Expected argument of type $allowedTypes, got $argumentType [$argumentName]", position)

class UnsupportedArgumentTypeError(val functionName: String, val argumentTypes: List<String>, val allowedTypes: String, position: Position) :
    ThrowableError("Function $functionName expected argument(s) of type $allowedTypes, got $argumentTypes", position)

class WrongDatatypeError(actual: DataType, expected: List<DataType>) :
        ThrowableError("The data has to be of type ${expected.map { it.name }}, but was of type ${actual.name}",
        position= emptyPosition())

class UnkownFunctionName(functionName: String) :
        ThrowableError("Unknown function $functionName", emptyPosition())



fun Assignment.evaluate(context: Map<String, Value>) : HashMap<String, Value> {
    val context = context.toMutableMap()

    if (!context.containsKey(this.varName))
        throw NotDeclaredError(this.varName, this.position!!)

    context[this.varName] = this.value.evaluate(context as HashMap<String, Value>)

    return context
}

fun Print.evaluate(context: Map<String, Value>) : Map<String, Value> {
    return context
}

fun callFunction(functionName: String, args: List<Value>) : Value {
    return when(functionName) {
//        "FromTime"  -> fromTime(args)
//        "ToTime"    -> toTime(args)
        else -> throw UnkownFunctionName(functionName)
    }
    return NumberValue(0)
}

class ArgMissingError(message: String) :
        ThrowableError(message, emptyPosition())

class ArgWrongTypeError(functionName: String, actual: String, expected: String) :
        ThrowableError("Function $functionName expected an arg of type $expected, but got $actual", emptyPosition())

//fun fromTime(args: List<Value>) : Value {
//    val data = args.getOrNull(0) ?: throw ArgMissingError("FromTime needs two arguments")
//    if (!(data is DatapointsValue ) )
//        throw ArgWrongTypeError("FromTime", data::class.simpleName!!, "Data of type Numerical")
//    if (data.dataType != DataType.TIME)
//        throw ArgWrongTypeError("FromTime", "Data of type ${data.dataType.name}", "Data of type Numerical")
//
//    val
//}
//
//fun toTime(args: List<Value>) : Value {
//
//}