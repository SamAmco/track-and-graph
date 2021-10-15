package com.samco.trackandgraph.antlr.evaluation

import com.samco.trackandgraph.antlr.generated.TnG2Parser
import com.samco.trackandgraph.database.entity.DataPoint
import org.antlr.v4.runtime.tree.ParseTree
import org.threeten.bp.Duration

abstract class Value {
    open fun _plus(other: Value) : Value = throw NotImplementedError()

    open fun _minus(other: Value) : Value = throw NotImplementedError()

    open fun _times(other: Value) : Value = throw NotImplementedError()

    open fun _div(other: Value) : Value = throw NotImplementedError()

    operator fun plus (other: Value) : Value {
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

class NumberValue(val number: Number) : Value() {
    fun toDouble() : Double = number.toDouble()

    override fun toString(): String {
        return "NumberValue(number=$number)"
    }

    override fun hashCode(): Int {
        return number.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is NumberValue) return this.number.toDouble() == other.number.toDouble()
        return super.equals(other)
    }

    override fun _plus(other: Value): Value = this.plusValue(other)
    override fun _minus(other: Value): Value = this.minusValue(other)
    override fun _times (other: Value): Value = this.timesValue(other)
    override fun _div(other: Value): Value = this.divValue(other)
}

class StringValue(val string: String): Value() {
    override fun toString(): String {
        return "StringValue(string=\"$string\""
    }

    override fun equals(other: Any?): Boolean {
        if (other is StringValue) return this.string == other.string
        return super.equals(other)
    }

    override fun hashCode(): Int {
        return string.hashCode()
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


    override fun hashCode(): Int {
        return duration.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is TimeValue) return this.duration == other.duration
        return super.equals(other)
    }

    override fun _plus(other: Value): Value = this.plusValue(other)
    override fun _minus(other: Value): Value = this.minusValue(other)
    override fun _times (other: Value): Value = this.timesValue(other)
    override fun _div(other: Value): Value = this.divValue(other)
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

    override fun hashCode(): Int {
        var result = datapoints.hashCode()
        result = 31 * result + dataType.hashCode()
        result = 31 * result + regularity.hashCode()
        return result
    }

    fun applyToAllPoints(function: (Double) -> Double, newDataType: DataType = this.dataType) : DatapointsValue {
        return DatapointsValue(
            this.datapoints.map { dp -> dp.copy(value = function(dp.value)) },
            dataType = newDataType, regularity = this.regularity
        )
    }


    override fun _plus(other: Value): Value = this.plusValue(other)
    override fun _minus(other: Value): Value = this.minusValue(other)
    override fun _times (other: Value): Value = this.timesValue(other)
    override fun _div(other: Value): Value = this.divValue(other)
}




