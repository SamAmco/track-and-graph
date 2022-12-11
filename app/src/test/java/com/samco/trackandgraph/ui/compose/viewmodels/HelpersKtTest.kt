package com.samco.trackandgraph.ui.compose.viewmodels

import com.samco.trackandgraph.ui.viewmodels.asValidatedDouble
import com.samco.trackandgraph.ui.viewmodels.asValidatedInt
import junit.framework.Assert.assertEquals
import org.junit.Test


internal class HelpersKtTest {

    @Test
    fun asValidatedInt() {
        assertEquals("-1", "-1-".asValidatedInt())
        assertEquals("-1", "-1.0".asValidatedInt())
        assertEquals("-1", "-1.0".asValidatedInt())
        assertEquals("1", "1.1".asValidatedInt())
        assertEquals("1001234", "1001234".asValidatedInt())
        assertEquals("-1001234", "-1001234".asValidatedInt())
        assertEquals("-", "-".asValidatedInt())
        assertEquals("0", "0".asValidatedInt())
    }

    @Test
    fun asValidatedDouble() {
        assertEquals("-1.0", "-1.0".asValidatedDouble())
        assertEquals("1.0", "1.0".asValidatedDouble())
        assertEquals("812341.123", "812341.123".asValidatedDouble())
        assertEquals("-812341.123", "-812341.123".asValidatedDouble())
        assertEquals("1", "1".asValidatedDouble())
        assertEquals("-1", "-1".asValidatedDouble())
        assertEquals("-1", "-1-".asValidatedDouble())
        assertEquals("-1.", "-1-.".asValidatedDouble())
        assertEquals(".", ".".asValidatedDouble())
        assertEquals("0.", "0.".asValidatedDouble())
        assertEquals("-1.00", "-1.0.0".asValidatedDouble())
    }
}