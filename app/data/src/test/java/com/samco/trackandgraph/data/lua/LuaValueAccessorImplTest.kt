/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.data.lua

import kotlinx.coroutines.sync.Mutex
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test
import org.luaj.vm2.Globals
import org.luaj.vm2.LuaTable
import org.luaj.vm2.LuaValue

class LuaValueAccessorImplTest {
    private val affinity = LuaVMRuntime(Globals(), Mutex(), "test")

    @Test
    fun `reads only string values from string keyed table`() {
        val table = LuaTable().apply {
            rawset("en", "Calendar")
            rawset("fr", "Calendrier")
            rawset("ignored", LuaValue.valueOf(4))
            rawset(1, "also ignored")
        }

        assertEquals(
            mapOf("en" to "Calendar", "fr" to "Calendrier"),
            LuaValueAccessorImpl(table, affinity).asStringMap(),
        )
    }

    @Test
    fun `non table has no string map`() {
        assertNull(LuaValueAccessorImpl(LuaValue.valueOf("Calendar"), affinity).asStringMap())
    }
}
