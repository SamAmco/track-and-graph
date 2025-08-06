/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 *
 *  Track & Graph is distributed in the hope that it will be useful,
 *  but WITHOUT ANY WARRANTY; without even the implied warranty of
 *  MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 *  GNU General Public License for more details.
 *
 *  You should have received a copy of the GNU General Public License
 *  along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.base.database.migrations

import com.squareup.moshi.JsonAdapter
import com.squareup.moshi.JsonClass
import com.squareup.moshi.Moshi
import com.squareup.moshi.Types
import java.lang.Exception

internal data class NTuple2<T1, T2>(val t1: T1, val t2: T2) {
    fun toList() = listOf(t1, t2)
}

internal data class NTuple3<T1, T2, T3>(val t1: T1, val t2: T2, val t3: T3) {
    fun toList() = listOf(t1, t2, t3)
}

internal data class NTuple4<T1, T2, T3, T4>(val t1: T1, val t2: T2, val t3: T3, val t4: T4) {
    fun toList() = listOf(t1, t2, t3, t4)
}

internal data class NTuple5<T1, T2, T3, T4, T5>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5
) {
    fun toList() = listOf(t1, t2, t3, t4, t5)
}

internal data class NTuple6<T1, T2, T3, T4, T5, T6>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5,
    val t6: T6
) {
    fun toList() = listOf(t1, t2, t3, t4, t5, t6)
}

internal data class NTuple7<T1, T2, T3, T4, T5, T6, T7>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5,
    val t6: T6,
    val t7: T7
) {
    fun toList() = listOf(t1, t2, t3, t4, t5, t6, t7)
}

internal data class NTuple8<T1, T2, T3, T4, T5, T6, T7, T8>(
    val t1: T1,
    val t2: T2,
    val t3: T3,
    val t4: T4,
    val t5: T5,
    val t6: T6,
    val t7: T7,
    val t8: T8
) {
    fun toList() = listOf(t1, t2, t3, t4, t5, t6, t7, t8)
}

internal class MigrationMoshiHelper private constructor() {
    val moshi: Moshi = Moshi.Builder().build()

    fun <T> toJson(adapter: JsonAdapter<T>, value: T): String {
        return try {
            adapter.toJson(value) ?: ""
        } catch (e: Exception) {
            ""
        }
    }

    fun <T> fromJson(adapter: JsonAdapter<T>, value: String, onError: () -> T): T {
        return try {
            adapter.fromJson(value) ?: onError()
        } catch (e: Exception) {
            onError()
        }
    }

    fun stringToListOfDiscreteValues(value: String): List<DiscreteValue> {
        if (value.isBlank()) return emptyList()
        val listType = Types.newParameterizedType(List::class.java, DiscreteValue::class.java)
        return fromJson(moshi.adapter(listType), value) { emptyList() }
    }

    fun listOfDiscreteValuesToString(values: List<DiscreteValue>): String {
        val listType = Types.newParameterizedType(List::class.java, DiscreteValue::class.java)
        return toJson(moshi.adapter(listType), values)
    }

    @JsonClass(generateAdapter = true)
    data class DiscreteValue(
        val index: Int,
        val label: String
    ) {

        //Ideally we wouldn't need fromString and toString here but they are still used by CSVReadWriter.
        override fun toString() = "$index:$label"

        companion object {
            fun fromString(value: String): DiscreteValue {
                if (!value.contains(':')) throw Exception("value did not contain a colon")
                val label = value.substring(value.indexOf(':') + 1).trim()
                val index = value.substring(0, value.indexOf(':')).trim().toDouble().toInt()
                return DiscreteValue(index, label)
            }
        }
    }

    companion object {
        internal fun getMigrationMoshiHelper() = MigrationMoshiHelper()
    }
}
