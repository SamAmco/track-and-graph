/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

internal class LatestQueryCache<K, V> {
    private val lock = Any()
    private var generation = 0L
    private var values = emptyMap<K, V>()

    fun beginQuery(): Long = synchronized(lock) { ++generation }

    fun replaceIfLatest(queryGeneration: Long, newValues: Map<K, V>): Boolean =
        synchronized(lock) {
            if (queryGeneration != generation) return@synchronized false
            values = newValues
            true
        }

    operator fun get(key: K): V? = synchronized(lock) { values[key] }
}
