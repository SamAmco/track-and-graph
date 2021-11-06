package com.samco.trackandgraph.antlr.evaluation

import com.samco.trackandgraph.R
import com.samco.trackandgraph.antlr.generated.TnG2Parser
import com.samco.trackandgraph.database.entity.DataPointInterface
import com.samco.trackandgraph.database.entity.FeatureType
import com.samco.trackandgraph.functionslib.DataSample
import org.antlr.v4.runtime.tree.ParseTree
import org.threeten.bp.Duration
import org.threeten.bp.Period
import org.threeten.bp.temporal.TemporalAmount
import kotlin.reflect.KClass
import kotlin.reflect.KFunction2

sealed class Value {
    init {
        val debugNameRes = valueClassToStringResId(this::class)
    }

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

class TimeValue(val temporalAmount: TemporalAmount) : Value() {
    constructor(context: ParseTree)  : this(when(context) {
        is TnG2Parser.SecondContext -> Duration.ofSeconds(1) // mostly for converting
        is TnG2Parser.MinuteContext -> Duration.ofMinutes(1) // mostly for converting
        is TnG2Parser.HourlyContext -> Duration.ofHours(1)    // mostly for converting, but mixed
        is TnG2Parser.DailyContext -> Period.ofDays(1)
        is TnG2Parser.WeeklyContext -> Period.ofDays(7)
        is TnG2Parser.MonthlyContext -> Period.ofMonths(1)
        is TnG2Parser.YearlyContext -> Period.ofYears(1)
        else -> throw UnsupportedOperationException("Unexpected Context ${context::class.simpleName}!")})


    override fun hashCode(): Int {
        return temporalAmount.hashCode()
    }

    override fun equals(other: Any?): Boolean {
        if (other is TimeValue) return this.temporalAmount.toDuration() == other.temporalAmount.toDuration()
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

private fun inferDatatype(data: DataSample) : DataType {
    return when (data.featureType) {
        FeatureType.DISCRETE -> DataType.CATEGORICAL
        FeatureType.CONTINUOUS -> DataType.NUMERICAL
        FeatureType.DURATION -> DataType.TIME
    }
}


fun DataType.toLocalizedString(getString: KFunction2<Int, Array<Any>, String>) : String{
    return when(this) {
        DataType.NUMERICAL -> getString(R.string.datatype_numerical, arrayOf())
        DataType.TIME -> getString(R.string.datatype_time, arrayOf())
        DataType.CATEGORICAL -> getString(R.string.datatype_categorical, arrayOf())
    }
}



class DatapointsValue(
    val datapoints: List<DataPointInterface>,
    val dataType: DataType,
    val regularity: Regularity = Regularity.NONE
) : Value() {
    constructor(
        dataSample: DataSample,
        regularity: Regularity = Regularity.NONE
    ) : this(dataSample.dataPoints, inferDatatype(dataSample), regularity)

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

    fun confirmType(allowedTypes: List<DataType>) {
        if (dataType !in allowedTypes) {
            throw DatapointsWrongTypeError(allowedTypes, dataType)
        }
    }

    fun toSample() = DataSample(
        this.datapoints,
        when(dataType) {
            DataType.NUMERICAL -> FeatureType.CONTINUOUS
            DataType.TIME -> FeatureType.DURATION
            DataType.CATEGORICAL -> FeatureType.DISCRETE
        }
    )
}

enum class AggregationEnum {
    AVERAGE,
    MEDIAN,
    MIN,
    MAX,
    EARLIEST,
    LATEST,
    SUM,
    COUNT,
}

class AggregationEnumValue(val aggregationFunction : AggregationEnum) : Value() {
    constructor(context: ParseTree)  : this(when(context) {
        is TnG2Parser.AF_MinContext -> AggregationEnum.MIN
        is TnG2Parser.AF_MaxContext -> AggregationEnum.MAX
        is TnG2Parser.AF_AverageContext -> AggregationEnum.AVERAGE
        is TnG2Parser.AF_MedianContext -> AggregationEnum.MEDIAN
        is TnG2Parser.AF_EarliestContext -> AggregationEnum.EARLIEST
        is TnG2Parser.AF_LatestContext -> AggregationEnum.LATEST
        is TnG2Parser.AF_SumContext ->  AggregationEnum.SUM
        is TnG2Parser.AF_CountContext -> AggregationEnum.COUNT
        else -> throw UnsupportedOperationException("Unexpected Context ${context::class.simpleName}!")})

    override fun equals(other: Any?): Boolean {
        if (other is AggregationEnumValue) return this.aggregationFunction == other.aggregationFunction
        return super.equals(other)
    }
}


fun valueClassToStringResId(valueClass: KClass<*>) : Int{
    return when (valueClass) {
        NumberValue::class -> R.string.trafoDebug_number
        DatapointsValue::class -> R.string.trafoDebug_datapoints
        TimeValue::class -> R.string.trafoDebug_time_duration
        StringValue::class -> R.string.trafoDebug_string
        AggregationEnumValue::class -> R.string.datatype_aggregation_enum
        else -> throw NotImplementedError("${valueClass.simpleName} not implemented!")
    }
}


