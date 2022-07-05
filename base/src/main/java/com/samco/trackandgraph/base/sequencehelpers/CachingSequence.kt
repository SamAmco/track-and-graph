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

package com.samco.trackandgraph.base.sequencehelpers

import java.util.*

fun <T> Sequence<T>.cache() = CachingSequence(this)

/**
 * A sequence that caches items that have already been visited so that they may be iterated again.
 * Supports multiple calls to [iterator]. Each call will return an iterator that iterates the whole
 * sequence from start to finish. The base sequence will only be iterated once for each item.
 */
class CachingSequence<T>(sequence: Sequence<T>) : Sequence<T> {
    private val upstream = sequence.iterator()
    private val cache: MutableList<T> = Collections.synchronizedList(mutableListOf<T>())

    override fun iterator(): Iterator<T> {
        return object : Iterator<T> {
            var index = 0
            override fun hasNext(): Boolean {
                return index in cache.indices || upstream.hasNext()
            }

            override fun next(): T {
                return if (index in cache.indices) cache[index++]
                else {
                    val next = upstream.next()
                    cache.add(next)
                    index++
                    next
                }
            }
        }
    }
}