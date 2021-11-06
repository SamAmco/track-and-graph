package com.samco.trackandgraph.antlr.evaluation

import org.threeten.bp.Duration
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount
import kotlin.math.roundToInt
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
    return when (other) {
        is NumberValue -> NumberValue(this.toDouble() / other.toDouble())
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation(
            "DIV",
            this,
            other,
            listOf(NumberValue::class)
        )
    }
}

/**
 *  TIME VALUE
 */

fun TemporalAmount.toDuration(): Duration {
    return when (this) {
        is Duration -> this
        is Period -> this.toDuration()
        else -> throw Exception("unreachable")
    }
}

private fun Period.toDuration(): Duration {
    val days = this.days + this.months * 30 + this.years * 365
    return Duration.ofDays(days.toLong())
}

private operator fun TemporalAmount.plus(other: TemporalAmount): TemporalAmount {
    return when (this) {
        is Period -> when (other) {
            is Period -> this + other
            is Duration -> this.toDuration() + other
            else -> throw Exception("unreachable")
        }
        is Duration -> when (other) {
            is Duration -> this + other
            is Period -> this + other.toDuration()
            else -> throw Exception("unreachable")
        }
        else -> throw Exception("unreachable")
    }
}

private operator fun TemporalAmount.minus(other: TemporalAmount): TemporalAmount {
    return when (this) {
        is Period -> when (other) {
            is Period -> this - other
            is Duration -> this.toDuration() - other
            else -> throw Exception("unreachable")
        }
        is Duration -> when (other) {
            is Duration -> this - other
            is Period -> this - other.toDuration()
            else -> throw Exception("unreachable")
        }
        else -> throw Exception("unreachable")
    }
}

private fun TemporalAmount._multipliedBy(other: Int): TemporalAmount {
    return when (this) {
        is Period -> Period.of(
            this.years * other, // only one is not zero so this works
            this.months * other,
            this.days * other
        )

        is Duration -> this.multipliedBy(other.toLong())

        else -> throw Exception("unreachable")
    }
}

private fun TemporalAmount._dividedBy(other: Long): TemporalAmount {
    return this.toDuration().dividedBy(other)
}

fun TimeValue.plusValue(other: Value): Value {
    return when (other) {
        is TimeValue -> TimeValue(this.temporalAmount + other.temporalAmount)
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation(
            "PLUS",
            this,
            other,
            listOf(TimeValue::class)
        )
    }
}

fun TimeValue.minusValue(other: Value): Value {
    return when (other) {
        is TimeValue -> TimeValue(this.temporalAmount - other.temporalAmount)
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation(
            "MINUS",
            this,
            other,
            listOf(TimeValue::class)
        )
    }
}

fun TimeValue.timesValue(other: Value): Value {
    return when(other) {
        is NumberValue -> TimeValue(
            this.temporalAmount._multipliedBy(
                other.toDouble().roundToInt()
            )
        )
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation(
            "TIMES",
            this,
            other,
            listOf(NumberValue::class)
        )
    }
}

fun TimeValue.divValue(other: Value): Value {
    return when(other) {
        is NumberValue -> TimeValue(this.temporalAmount._dividedBy(other.toDouble().roundToLong()))
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation(
            "DIV",
            this,
            other,
            listOf(NumberValue::class)
        )
    }
}

/**
 *  DATAPOINTS VALUE
 */

fun DatapointsValue.plusValue(other: Value): Value {
    return when(other) {
        is NumberValue -> applyToAllPoints( { it + other.toDouble() } )
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation(
            "PLUS",
            this,
            other,
            listOf(NumberValue::class)
        )
    }
}

fun DatapointsValue.minusValue(other: Value): Value {
    return when (other) {
        is NumberValue -> applyToAllPoints({ it - other.toDouble() })
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation(
            "MINUS",
            this,
            other,
            listOf(NumberValue::class)
        )
    }
}

internal fun TemporalAmount.toSeconds(): Long {
    return when (this) {
        is Duration -> this.seconds
        is Period -> this.toDuration().seconds
        else -> throw Exception("unreachable")
    }
}

fun DatapointsValue.timesValue(other: Value): Value {
    return when (other) {
        is NumberValue -> applyToAllPoints({ it * other.toDouble() })
        is TimeValue -> {
            if (this.dataType != DataType.NUMERICAL) throwUnsupportedArgumentTypeErrorBinaryOperation(
                "TIMES",
                this,
                other,
                listOf(NumberValue::class)
            )
            else applyToAllPoints(
                { it * other.temporalAmount.toSeconds() },
                newDataType = DataType.TIME
            )
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
            else applyToAllPoints(
                { it / other.temporalAmount.toSeconds() },
                newDataType = DataType.NUMERICAL
            )
        }
        else -> throwUnsupportedArgumentTypeErrorBinaryOperation("DIV", this, other, listOf(NumberValue::class))
    }
}