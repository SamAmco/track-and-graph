package com.samco.trackandgraph.data.algorithms

/**
 * MurmurHash3 to convert from Long -> Int with lower collision rates
 * than Long.toInt() or Long.hashCode()
 */
fun Long.murmurHash3(): Int {
    var x = this
    x = (x xor (x ushr 33)) * -0xae502812aa7333L
    x = (x xor (x ushr 33)) * -0x3b314601e57a13adL
    x = x xor (x ushr 33)
    return x.toInt()
}