/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */
package com.samco.trackandgraph.lua.ui

import com.samco.trackandgraph.R
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class LuaIconTest {
    @Test
    fun `semantic tokens resolve to app drawable resources`() {
        assertEquals(R.drawable.add_icon, luaIconDrawableResource("ICON_ADD"))
        assertEquals(R.drawable.ic_add_record, luaIconDrawableResource("ICON_ADD_RECORD"))
        assertEquals(R.drawable.delete_icon, luaIconDrawableResource("ICON_DELETE"))
        assertEquals(R.drawable.list_menu_icon, luaIconDrawableResource("ICON_MORE"))
        assertEquals(R.drawable.chart_data, luaIconDrawableResource("ICON_GRAPH"))
        assertEquals(R.drawable.warning_icon, luaIconDrawableResource("ICON_WARNING"))
    }

    @Test
    fun `unknown tokens do not resolve`() {
        assertNull(luaIconDrawableResource(null))
        assertNull(luaIconDrawableResource("ICON_UNKNOWN"))
    }
}
