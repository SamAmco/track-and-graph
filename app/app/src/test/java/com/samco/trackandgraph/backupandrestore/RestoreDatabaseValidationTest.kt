package com.samco.trackandgraph.backupandrestore

import com.samco.trackandgraph.data.database.TNG_DATABASE_VERSION
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class RestoreDatabaseValidationTest {
    @Test
    fun `accepts intact positive versions up to current for Room validation`() {
        assertTrue(
            isValidRestoreDatabaseMetadata(
                1,
                listOf("ok"),
            )
        )
        assertTrue(
            isValidRestoreDatabaseMetadata(
                TNG_DATABASE_VERSION,
                listOf("ok"),
            )
        )
    }

    @Test
    fun `rejects zero and future database versions`() {
        assertFalse(
            isValidRestoreDatabaseMetadata(
                0,
                listOf("ok"),
            )
        )
        assertFalse(
            isValidRestoreDatabaseMetadata(
                TNG_DATABASE_VERSION + 1,
                listOf("ok"),
            )
        )
    }

    @Test
    fun `rejects database that fails integrity check`() {
        assertFalse(
            isValidRestoreDatabaseMetadata(
                TNG_DATABASE_VERSION,
                listOf("database disk image is malformed"),
            )
        )
    }
}
