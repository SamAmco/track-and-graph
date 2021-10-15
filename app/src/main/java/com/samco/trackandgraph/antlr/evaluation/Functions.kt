package com.samco.trackandgraph.antlr.evaluation

fun callFunction(functionName: String, args: List<Value>) : Value {
    return when(functionName) {
//        "FromTime"  -> fromTime(args)
//        "ToTime"    -> toTime(args)
        else -> throw UnkownFunctionName(functionName)
    }
    return NumberValue(0)
}

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