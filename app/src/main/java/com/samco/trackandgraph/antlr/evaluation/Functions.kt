package com.samco.trackandgraph.antlr.evaluation

import com.samco.trackandgraph.R
import com.samco.trackandgraph.database.entity.DataPointInterface
import com.samco.trackandgraph.functionslib.GlobalAggregationPreferences
import com.samco.trackandgraph.functionslib.TimeHelper
import com.samco.trackandgraph.functionslib.aggregation.FixedBinAggregator
import com.samco.trackandgraph.functionslib.aggregation.MovingAggregator
import org.threeten.bp.OffsetDateTime

suspend fun callFunction(functionName: String, args: List<Value>, context: Map<String, Value>) : Value {
//    FunctionCallableFromCode::class.sealedSubclasses.map { it.objectInstance?.functionName  }
    return when(functionName) {
        "Delta" -> DeltaFunction().main(args)
        "Accumulate" -> AccumulateFunction().main(args)
        "Derivative" -> DerivativeFunction().main(args)
        "TimeBetween" -> TimeBetweenFunction().main(args)
        "TimeBetween2" -> TimeBetween2Function().main(args)

        "Filter" -> FilterFunction().main(args)
        "Exclude" -> ExcludeFunction().main(args)

        "Merge" -> MergeFunction().main(args)

        "Moving" -> MovingAggregationFunction().main(args)
        "Bin" -> FixedBinAggregationFunction().main(args, context)
        else -> throw UnknownFunctionName(functionName)
    }
}

sealed class FunctionCallableFromCode(val functionName: String, val signature: Int) {
    /**
     * Throws errors if the argument has the wrong type or is missing.
     */
    inline fun <reified T : Value> getArgument(
        argumentList: List<Value>,
        index: Int
    ): T {
        val arg =
            argumentList.elementAtOrNull(index) ?: throw ArgMissingError(functionName, index, T::class)
        when (arg) {
            is T -> return arg
            else -> throw WrongArgDatatypeError(functionName, index, arg::class, listOf(T::class))
        }
    }

    fun checkTooManyArgs(argumentList: List<Value>, expectedNumber: Int) {
        if (argumentList.size > expectedNumber) {
            throw TooManyArgsError(functionName, expectedNumber, argumentList.size)
        }
    }

//    abstract suspend fun main(args: List<Value>) : Value

}

class DeltaFunction : FunctionCallableFromCode("Delta", R.string.functionsig_delta) {
    suspend fun main(args: List<Value>) : Value {
        val inputData = getArgument<DatapointsValue>(args, 0)
        inputData.confirmType(listOf(DataType.NUMERICAL, DataType.TIME))
        checkTooManyArgs(args, 1)

        val outputData = inputData.datapoints.zipWithNext { a, b -> b.copyPoint(value = b.value - a.value) }
        return DatapointsValue(outputData, inputData.dataType)
    }
}

class AccumulateFunction : FunctionCallableFromCode("Accumulate", R.string.functionsig_accumulate) {
    suspend fun main(args: List<Value>): Value {
        val inputData = getArgument<DatapointsValue>(args, 0)
        inputData.confirmType(listOf(DataType.NUMERICAL, DataType.TIME))
        checkTooManyArgs(args, 1)

        val outputData = mutableListOf<DataPointInterface>()
        var sum = 0.0
        for (i in inputData.datapoints.indices) {
            val point = inputData.datapoints[i]
            sum += point.value
            outputData.add(point.copyPoint(value = sum))
        }

        return DatapointsValue(outputData, inputData.dataType)
    }
}

class DerivativeFunction : FunctionCallableFromCode("Derivative", R.string.functionsig_derivative) {
    suspend fun main(args: List<Value>): Value {
        val inputData = getArgument<DatapointsValue>(args, 0)
        inputData.confirmType(listOf(DataType.NUMERICAL, DataType.TIME))
        val perTime = getArgument<TimeValue>(args, 1)
        checkTooManyArgs(args, 2)

        fun calcDerivative(a: DataPointInterface, b: DataPointInterface) : DataPointInterface {
            val diff = b.value - a.value
            val derivative = diff / (b.timestamp.toEpochSecond() - a.timestamp.toEpochSecond())
            val scaledDerivative = derivative * perTime.temporalAmount.toSeconds()
            return b.copyPoint(value = scaledDerivative)
        }

        val outputData = inputData.datapoints.zipWithNext { a, b -> calcDerivative(a, b) }
        return DatapointsValue(outputData, inputData.dataType)
    }
}

class TimeBetweenFunction : FunctionCallableFromCode("TimeBetween", R.string.functionsig_timebetween) {
    suspend fun main(args: List<Value>): Value {
        val inputData = getArgument<DatapointsValue>(args, 0)
        checkTooManyArgs(args, 1)

        val outputData = inputData.datapoints.zipWithNext {
                a, b -> b.copyPoint(value = b.timestamp.toEpochSecond().toDouble() - a.timestamp.toEpochSecond()) }
        return DatapointsValue(outputData, DataType.TIME)
    }
}

class TimeBetween2Function : FunctionCallableFromCode("TimeBetween2", R.string.functionsig_timebetween2) {
    suspend fun main(args: List<Value>): Value {
        val mainData = getArgument<DatapointsValue>(args, 0)
        val referenceData = getArgument<DatapointsValue>(args, 1)
        checkTooManyArgs(args, 2)

        // since we want an exclusive 1 to 1 mapping (meaning not referring to the same ref-point more than once
        // we have to keep track of the points already referenced
        // then we loop through the points in reversed order to prioritise the closer matches
        // (the call-chain contains two reversed()-calls to keep the established order)
        val alreadyReferenced = mutableSetOf<DataPointInterface>()

        val outputData = mainData.datapoints.reversed().map {
            it.let {
                val ref = referenceData.datapoints.firstOrNull { p -> p.timestamp >= it.timestamp }
                if (ref == null || ref in alreadyReferenced) null
                else {
                    alreadyReferenced.add(ref)
                    it.copyPoint(value = ref.timestamp.toEpochSecond().toDouble() - it.timestamp.toEpochSecond())
                }
            }
        }.filterNotNull().reversed()
        return DatapointsValue(outputData, DataType.TIME)
    }
}

class FilterFunction : FunctionCallableFromCode("Filter", R.string.functionsig_filter) {
    suspend fun main(args: List<Value>): Value {
        val inputData = getArgument<DatapointsValue>(args, 0)
        inputData.confirmType(listOf(DataType.CATEGORICAL))
        getArgument<StringValue>(args, 1) // to make sure we have at least one string
        val allowedValues = args.subList(1, args.size).mapIndexed { index, value ->
            when (value) {
                is StringValue -> value.string
                else -> throw WrongArgDatatypeError("Filter", index+1, value::class, listOf(StringValue::class))
            } }
        return DatapointsValue(
            inputData.datapoints.filter { it.label in allowedValues },
            inputData.dataType
        )
    }
}

class ExcludeFunction : FunctionCallableFromCode("Exclude", R.string.functionsig_exclude) {
    suspend fun main(args: List<Value>): Value {
        val inputData = getArgument<DatapointsValue>(args, 0)
        inputData.confirmType(listOf(DataType.CATEGORICAL))
        getArgument<StringValue>(args, 1) // to make sure we have at least one string
        val allowedValues = args.subList(1, args.size).mapIndexed { index, value ->
            when (value) {
                is StringValue -> value.string
                else -> throw WrongArgDatatypeError("Filter", index+1, value::class, listOf(StringValue::class))
            } }
        return DatapointsValue(
            inputData.datapoints.filter { it.label !in allowedValues },
            inputData.dataType
        )
    }
}

class MergeFunction : FunctionCallableFromCode("Merge", R.string.functionsig_merge) {
    suspend fun main(args: List<Value>): Value {
        val reference = getArgument<DatapointsValue>(args, 0)
        getArgument<DatapointsValue>(args, 1) // to make sure we have at least one more sample
        val listsToMerge: List<MutableList<DataPointInterface>> = args.subList(0, args.size).mapIndexed { index, value ->
            when (value) {
                is DatapointsValue -> value.apply { this.confirmType(listOf(reference.dataType)) }.datapoints.toMutableList()
                else -> throw WrongArgDatatypeError("Merge", index, value::class, listOf(StringValue::class))
            } }

        val mergedList = mutableListOf<DataPointInterface>()
        while (listsToMerge.any { it.size > 0 }) {
            val minIndex = listsToMerge
                .withIndex()
                .minByOrNull { it.value.firstOrNull()?.timestamp ?: OffsetDateTime.MAX }!!
                .index

            mergedList.add(listsToMerge[minIndex].removeFirst())
        }

        // regularity is NONE, bc even if all inputs were regular, now that there could be
        // more than one point per regular interval
        return DatapointsValue(mergedList, reference.dataType) // when we merge, we don't have a singular featureId, so do -1L
    }
}

class MovingAggregationFunction : FunctionCallableFromCode("Moving", R.string.functionsig_moving) {
    suspend fun main(args: List<Value>): Value {
        val inputData = getArgument<DatapointsValue>(args, 0)
        inputData.confirmType(listOf(DataType.NUMERICAL, DataType.TIME))
        val aggregationFunction = getArgument<AggregationEnumValue>(args, 1)
        val timeWindow = getArgument<TimeValue>(args, 2)
        checkTooManyArgs(args, 3)

        return DatapointsValue(
            MovingAggregator(timeWindow.temporalAmount).aggregate(inputData.toSample())(
                aggregationFunction.aggregationFunction,
            )
        )

    }
}

class FixedBinAggregationFunction : FunctionCallableFromCode("Bin", R.string.functionsig_bin) {
    suspend fun main(args: List<Value>, fullContext: Map<String, Value>): Value {
        val inputData = getArgument<DatapointsValue>(args, 0)
        inputData.confirmType(listOf(DataType.NUMERICAL, DataType.TIME))
        val aggregationFunction = getArgument<AggregationEnumValue>(args, 1)
        val timeWindow = getArgument<TimeValue>(args, 2)

        // we provide the option to supply a fallback value for aggregation functions which otherwise
        // drop empty bins
        val fallback : Double? = if (args.size == 4) getArgument<NumberValue>(args, 3).toDouble() else null
        checkTooManyArgs(args, 4)

        val earliestTimestampInContext = fullContext.values.filterIsInstance<DatapointsValue>()
            .mapNotNull { it.datapoints.firstOrNull() }.minByOrNull { it.timestamp }?.timestamp ?: OffsetDateTime.now()

        return DatapointsValue(
            FixedBinAggregator(
                TimeHelper(GlobalAggregationPreferences),
                samplePointsSince = null,
                endTime = null,
                timeWindow.temporalAmount,
                hardStartTime = earliestTimestampInContext
            ).aggregate(inputData.toSample())(aggregationFunction.aggregationFunction, fallback)
        )


    }
}