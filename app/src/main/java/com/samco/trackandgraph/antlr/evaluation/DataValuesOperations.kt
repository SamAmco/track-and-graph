package com.samco.trackandgraph.antlr.evaluation

import kotlin.math.roundToLong

/**
 *  NUMBER VALUE
 */

fun NumberValue.plusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> NumberValue(this.toDouble() + other.toDouble())
        else -> throwUnsupportedArgumentTypeError("PLUS", this, other, "Number")
    }
}

fun NumberValue.minusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> NumberValue(this.toDouble() - other.toDouble())
        else -> throwUnsupportedArgumentTypeError("MINUS", this, other, "Number")
    }
}

fun NumberValue.timesValue(other: Value): Value {
    return when(other) {
        is NumberValue -> NumberValue(this.toDouble() * other.toDouble())
        is TimeValue -> other._times(this) //we have to know that this is not recursive (it isn't)
        else -> throwUnsupportedArgumentTypeError("TIMES", this, other, "Number, Timeinterval")
    }
}

fun NumberValue.divValue(other: Value): Value {
    return when(other) {
        is NumberValue -> NumberValue(this.toDouble() / other.toDouble())
        else -> throwUnsupportedArgumentTypeError("DIV", this, other, "Number")
    }
}

/**
 *  TIME VALUE
 */


fun TimeValue.plusValue(other: Value): Value {
    return when(other) {
        is TimeValue -> TimeValue(this.duration + other.duration)
        else -> throwUnsupportedArgumentTypeError("PLUS", this, other, "Timeinterval")
    }
}

fun TimeValue.minusValue(other: Value): Value {
    return when(other) {
        is TimeValue -> TimeValue(this.duration - other.duration)
        else -> throwUnsupportedArgumentTypeError("MINUS", this, other, "Timeinterval")
    }
}

fun TimeValue.timesValue(other: Value): Value {
    return when(other) {
        is NumberValue -> TimeValue(this.duration.multipliedBy(other.toDouble().roundToLong()))
        else -> throwUnsupportedArgumentTypeError("TIMES", this, other, "Number")
    }
}

fun TimeValue.divValue(other: Value): Value {
    return when(other) {
        is NumberValue -> TimeValue(this.duration.dividedBy(other.toDouble().roundToLong()))
        else -> throwUnsupportedArgumentTypeError("DIV", this, other, "Number")
    }
}

/**
 *  DATAPOINTS VALUE
 */

fun DatapointsValue.plusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints( { it + other.toDouble() } )
        else -> throwUnsupportedArgumentTypeError("PLUS", this, other, "Number")
    }
}

fun DatapointsValue.minusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints( { it - other.toDouble() } )
        else -> throwUnsupportedArgumentTypeError("MINUS", this, other, "Number")
    }
}

fun DatapointsValue.timesValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints({ it * other.toDouble() } )
        is TimeValue -> {
            if (this.dataType != DataType.NUMERICAL) throwUnsupportedArgumentTypeError(
                "TIMES",
                this,
                other,
                "Number"
            )
            else applyToAllPoints( {it * other.duration.seconds }, newDataType = DataType.TIME)
        }
        else -> throwUnsupportedArgumentTypeError("TIMES", this, other, "Number")
    }
}

fun DatapointsValue.divValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints( { it / other.toDouble() } )
        is TimeValue -> {
            if (this.dataType != DataType.TIME) throwUnsupportedArgumentTypeError(
                "DIV",
                this,
                other,
                "Number"
            )
            else applyToAllPoints( {it / other.duration.seconds }, newDataType = DataType.NUMERICAL)
        }
        else -> throwUnsupportedArgumentTypeError("DIV", this, other, "Number")
    }
}