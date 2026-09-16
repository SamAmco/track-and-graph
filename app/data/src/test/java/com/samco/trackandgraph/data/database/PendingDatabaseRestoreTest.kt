package com.samco.trackandgraph.data.database

import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.nio.file.Files
import java.nio.file.StandardCopyOption

class PendingDatabaseRestoreTest {
    private val rename: (File, File) -> Unit = { source, target ->
        Files.move(source.toPath(), target.toPath(), StandardCopyOption.REPLACE_EXISTING)
    }

    @Test
    fun `staged restore replaces database and removes old sidecars`() {
        val directory = Files.createTempDirectory("pending-restore-test").toFile()
        try {
            val database = File(directory, "database").apply { writeText("old") }
            val source = File(directory, "selected-backup").apply { writeText("restored") }
            val wal = File(database.path + "-wal").apply { writeText("old wal") }
            val shm = File(database.path + "-shm").apply { writeText("old shm") }

            PendingDatabaseRestore.stage(source, database, rename)

            assertEquals("old", database.readText())
            assertTrue(File(database.path + ".restore_staged").exists())
            assertTrue(File(database.path + ".restore_pending").exists())

            assertTrue(PendingDatabaseRestore.installIfPending(database, rename))

            assertEquals("restored", database.readText())
            assertFalse(wal.exists())
            assertFalse(shm.exists())
            assertFalse(File(database.path + ".restore_staged").exists())
            assertFalse(File(database.path + ".restore_pending").exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `database is unchanged when no restore is pending`() {
        val directory = Files.createTempDirectory("pending-restore-test").toFile()
        try {
            val database = File(directory, "database").apply { writeText("old") }
            val wal = File(database.path + "-wal").apply { writeText("old wal") }

            assertFalse(PendingDatabaseRestore.installIfPending(database, rename))

            assertEquals("old", database.readText())
            assertTrue(wal.exists())
        } finally {
            directory.deleteRecursively()
        }
    }

    @Test
    fun `pending marker finishes cleanup when database was already renamed`() {
        val directory = Files.createTempDirectory("pending-restore-test").toFile()
        try {
            val database = File(directory, "database").apply { writeText("old") }
            val source = File(directory, "selected-backup").apply { writeText("restored") }

            PendingDatabaseRestore.stage(source, database, rename)
            rename(File(database.path + ".restore_staged"), database)
            val wal = File(database.path + "-wal").apply { writeText("old wal") }

            assertTrue(PendingDatabaseRestore.installIfPending(database, rename))

            assertEquals("restored", database.readText())
            assertFalse(wal.exists())
            assertFalse(File(database.path + ".restore_pending").exists())
        } finally {
            directory.deleteRecursively()
        }
    }
}
