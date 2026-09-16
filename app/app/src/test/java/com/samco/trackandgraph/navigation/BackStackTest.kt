/*
 *  This file is part of Track & Graph
 *
 *  Track & Graph is free software: you can redistribute it and/or modify
 *  it under the terms of the GNU General Public License as published by
 *  the Free Software Foundation, either version 3 of the License, or
 *  (at your option) any later version.
 */
package com.samco.trackandgraph.navigation

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class BackStackTest {

    @Test
    fun `pop removes the top destination when more than the root remains`() {
        val backStack = mutableListOf("root", "first", "second")

        assertTrue(backStack.popLastIfNotRoot())

        assertEquals(listOf("root", "first"), backStack)
    }

    @Test
    fun `repeated pops never remove the root destination`() {
        val backStack = mutableListOf("root", "destination")

        assertTrue(backStack.popLastIfNotRoot())
        assertFalse(backStack.popLastIfNotRoot())
        assertFalse(backStack.popLastIfNotRoot())

        assertEquals(listOf("root"), backStack)
    }

    @Test
    fun `pop is safe for an empty inactive stack`() {
        val backStack = mutableListOf<String>()

        assertFalse(backStack.popLastIfNotRoot())

        assertEquals(emptyList<String>(), backStack)
    }
}
