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

import com.samco.trackandgraph.data.algorithms.Xoroshiro128Plus
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue
import javax.inject.Inject
import kotlin.random.Random

class RandomApiImpl @Inject constructor() {
    fun installIn(table: LuaTable) = table.apply {
        overrideOrThrow("new_seeded_random", randomSeedLuaFunction())
    }

    private fun randomSeedLuaFunction() = twoArgFunction { a, b ->
        val aDouble = if (a.isnumber()) a.checkdouble() else Random.nextDouble()
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

