/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.data.lua.apiimpl

import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject
import kotlin.random.Random

class RandomApiImpl @Inject constructor() {
    fun installIn(table: LuaTable) = table.apply {
        overrideOrThrow("new_seeded_random", randomSeedLuaFunction())
    }

    private fun randomSeedLuaFunction() = twoArgFunction { a, b ->
        val aDouble = if (a.isnumber()) a.checkdouble() else Random.Default.nextDouble()
        val bDouble = b.optdouble(0.0)
        val random = Xoroshiro128Plus(aDouble, bDouble)
        newRandomLuaTable(random)
    }

    private fun newRandomLuaTable(random: Xoroshiro128Plus): LuaTable {
        val table = LuaTable()
        table["next"] = threeArgFunction { self, min, max ->
            val min = min.checkdouble()
            val max = max.checkdouble()
            val result = random.nextDouble(min, max)
            return@threeArgFunction LuaValue.valueOf(result)
        }
        return table
    }
}

/**
 * xoroshiro128+ (1.0) PRNG seeded from two Doubles.
 * reference implementation: https://prng.di.unimi.it/xoroshiro128plus.c
 *
 * - Not cryptographically secure.
 * - Small state (128 bits), very fast init & next().
 * - Uses SplitMix64-style mixers to turn the two double seeds into a valid state.
 * - KMP safe (should remain stable regardless of platform or Lua engine changes)
 *
 * References:
 *  - xoroshiro128+ reference impl (Blackman & Vigna)
 *  - SplitMix64 mixer for seeding (Vigna)
 *  - 53-bit double generation from 64-bit int
 */
private class Xoroshiro128Plus(seedA: Double, seedB: Double) {

    private var s0: Long
    private var s1: Long

    init {
        val a = seedA.toBits()
        val b = seedB.toBits()

        s0 = mix64(a + GOLDEN_GAMMA)
        s1 = mix64(b + GOLDEN_GAMMA * 3)

        if (s0 == 0L && s1 == 0L) s1 = -1L
    }

    fun nextLong(): Long {
        val r = s0 + s1
        var s1v = s1 xor s0
        val s0v = rotl(s0, 24) xor s1v xor (s1v shl 16) // a=24, b=16
        s1v = rotl(s1v, 37)                              // c=37
        s0 = s0v
        s1 = s1v
        return r
    }

    fun nextDouble(): Double {
        val x = nextLong() ushr 11 // top 53 bits
        return x * INV_2_POW_53
    }

    fun nextDouble(min: Double, max: Double): Double {
        require(max > min) { "max must be > min" }
        return min + (max - min) * nextDouble()
    }

    private fun rotl(x: Long, k: Int): Long = (x shl k) or (x ushr (64 - k))

    private fun mix64(xIn: Long): Long {
        var z = xIn
        z = (z xor (z ushr 30)) * -4658895280553007687L  // 0xbf58476d1ce4e5b9
        z = (z xor (z ushr 27)) * -7723592293110705685L  // 0x94d049bb133111eb
        return z xor (z ushr 31)
    }

    companion object {
        private const val INV_2_POW_53 = 1.0 / (1L shl 53).toDouble()
        private const val GOLDEN_GAMMA = -7046029254386353131L // 0x9e3779b97f4a7c15
    }
}
