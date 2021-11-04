package com.samco.trackandgraph.antlr.evaluation


import com.samco.trackandgraph.antlr.ast.*
import com.samco.trackandgraph.antlr.parsing.DatatransformationFunctionParserFacade
import com.samco.trackandgraph.database.entity.DataPoint

//TODO this function needs to be made obsolete by incorporating the information in DataSample!
fun inferDatatype(data: List<DataPoint>) : DataType {
    if (data.all { it.label != "" }) return DataType.CATEGORICAL
    return DataType.NUMERICAL
}

class EvaluationModel {

    fun run(code: String, inputFeatures: Map<String, List<DataPoint>> = emptyMap()) : Map<String, Value> {
        val parseResult = DatatransformationFunctionParserFacade.parse(code, inputFeatures.keys)
        when (parseResult.errors.size) {
            0 -> {}
            1 -> throw parseResult.errors[0]
            else -> throw ListOfErrors(parseResult.errors)
        }

        return this.evaluate(parseResult.root!!, inputFeatures)
    }

    private fun evaluate(
        parsedTree: DatatransformationFunction,
        inputFeatures: Map<String, List<DataPoint>> = emptyMap<String, List<DataPoint>>()
    ) : Map<String, Value> {
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

fun Expression.evaluate(context: Map<String, Value>) : Value {
    try {
        return when (this) {
            is IntLit -> NumberValue(this.value.toInt())
            is DecLit -> NumberValue(this.value.toDouble())
            is StringLit -> StringValue(this.value)
            is TimeperiodLit -> TimeValue(this.context)
            is AggregationEnumLit -> AggregationEnumValue(this.context)

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
    } catch (e: DatatransformationFunctionError) {
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