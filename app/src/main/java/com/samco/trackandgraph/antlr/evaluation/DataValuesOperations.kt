package com.samco.trackandgraph.antlr.evaluation

import kotlin.math.roundToLong

/**
 *  NUMBER VALUE
 */

fun NumberValue.plusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> NumberValue(this.toDouble() + other.toDouble())
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("PLUS", this, other, listOf(NumberValue::class))
    }
}

fun NumberValue.minusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> NumberValue(this.toDouble() - other.toDouble())
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("MINUS", this, other, listOf(NumberValue::class))
    }
}

fun NumberValue.timesValue(other: Value): Value {
    return when(other) {
        is NumberValue -> NumberValue(this.toDouble() * other.toDouble())
        is TimeValue -> other._times(this) //we have to know that this is not recursive (it isn't)
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("TIMES", this, other, listOf(NumberValue::class, TimeValue::class))
    }
}

fun NumberValue.divValue(other: Value): Value {
    return when(other) {
        is NumberValue -> NumberValue(this.toDouble() / other.toDouble())
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("DIV", this, other, listOf(NumberValue::class))
    }
}

/**
 *  TIME VALUE
 */


fun TimeValue.plusValue(other: Value): Value {
    return when(other) {
        is TimeValue -> TimeValue(this.duration + other.duration)
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("PLUS", this, other, listOf(TimeValue::class))
    }
}

fun TimeValue.minusValue(other: Value): Value {
    return when(other) {
        is TimeValue -> TimeValue(this.duration - other.duration)
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("MINUS", this, other, listOf(TimeValue::class))
    }
}

fun TimeValue.timesValue(other: Value): Value {
    return when(other) {
        is NumberValue -> TimeValue(this.duration.multipliedBy(other.toDouble().roundToLong()))
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("TIMES", this, other, listOf(NumberValue::class))
    }
}

fun TimeValue.divValue(other: Value): Value {
    return when(other) {
        is NumberValue -> TimeValue(this.duration.dividedBy(other.toDouble().roundToLong()))
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("DIV", this, other, listOf(NumberValue::class))
    }
}

/**
 *  DATAPOINTS VALUE
 */

fun DatapointsValue.plusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints( { it + other.toDouble() } )
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("PLUS", this, other, listOf(NumberValue::class))
    }
}

fun DatapointsValue.minusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints( { it - other.toDouble() } )
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("MINUS", this, other, listOf(NumberValue::class))
    }
}

fun DatapointsValue.timesValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints({ it * other.toDouble() } )
        is TimeValue -> {
            if (this.dataType != DataType.NUMERICAL) throwUnsupportedArgumentTypeErrorBinaryOperation(
                "TIMES",
                this,
                other,
                listOf(NumberValue::class)
            )
            else applyToAllPoints( {it * other.duration.seconds }, newDataType = DataType.TIME)
        }
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("TIMES", this, other, listOf(NumberValue::class))
    }
}

fun DatapointsValue.divValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints( { it / other.toDouble() } )
        is TimeValue -> {
            if (this.dataType != DataType.TIME) throwUnsupportedArgumentTypeErrorBinaryOperation(
                "DIV",
                this,
                other,
                listOf(NumberValue::class)
            )
            else applyToAllPoints( {it / other.duration.seconds }, newDataType = DataType.NUMERICAL)
        }
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("DIV", this, other, listOf(NumberValue::class))
    }
}