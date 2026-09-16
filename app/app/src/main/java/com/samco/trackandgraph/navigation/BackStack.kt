/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.samco.trackandgraph.navigation

/**
 * Removes the top destination only when doing so leaves the root destination in place.
 *
 * Navigation 3 requires a non-empty back stack. Keeping this check next to the mutation makes
 * repeated callbacks safe, including callbacks delivered before navigation has recomposed.
 */
internal fun <T> MutableList<T>.popLastIfNotRoot(): Boolean {
    if (size <= 1) return false
    removeAt(lastIndex)
    return true
}
