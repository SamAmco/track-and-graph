package com.samco.trackandgraph.data.database

import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreDatabaseValidationTest {
    @Test
    fun `accepts intact positive versions up to current for Room validation`() {
        assertTrue(isValidRestoreDatabaseMetadata(1, "ok"))
        assertTrue(isValidRestoreDatabaseMetadata(TNG_DATABASE_VERSION, "ok"))
    }

    @Test
    fun `rejects zero and future database versions`() {
        assertFalse(isValidRestoreDatabaseMetadata(0, "ok"))
        assertFalse(isValidRestoreDatabaseMetadata(TNG_DATABASE_VERSION + 1, "ok"))
    }

    @Test
    fun `rejects database that fails integrity check`() {
        assertFalse(
            isValidRestoreDatabaseMetadata(
                TNG_DATABASE_VERSION,
                "database disk image is malformed",
            )
        )
    }

    @Test
    fun `rejects database when integrity check returns no result`() {
        assertFalse(isValidRestoreDatabaseMetadata(TNG_DATABASE_VERSION, null))
    }
}
