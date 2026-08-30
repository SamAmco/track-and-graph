/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 */

package com.samco.trackandgraph.support

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class SupportBillingGatewayTest {
    @Test
    fun `latest product query replaces cached offers`() {
        val cache = LatestQueryCache<String, String>()
        val query = cache.beginQuery()

        assertTrue(cache.replaceIfLatest(query, mapOf("new" to "offer")))

        assertEquals("offer", cache["new"])
    }

    @Test
    fun `late product query cannot replace offers from a newer query`() {
        val cache = LatestQueryCache<String, String>()
        val oldQuery = cache.beginQuery()
        val newQuery = cache.beginQuery()
        assertTrue(cache.replaceIfLatest(newQuery, mapOf("current" to "offer")))

        assertFalse(cache.replaceIfLatest(oldQuery, mapOf("stale" to "offer")))

        assertEquals("offer", cache["current"])
        assertEquals(null, cache["stale"])
    }
}
