package com.samco.trackandgraph.data.database

import android.system.Os
import java.io.File
import java.io.FileOutputStream
import java.io.IOException

/**
 * Stages a replacement database and installs it before Room opens in a new process.
 *
 * The marker deliberately survives the database rename. If the process stops between replacing
 * the main file and removing the old WAL files, the next process will finish that cleanup before
 * Room is allowed to open the restored database.
 */
internal object PendingDatabaseRestore {
    private const val STAGED_SUFFIX = ".restore_staged"
    private const val TEMP_SUFFIX = ".restore_tmp"
    private const val MARKER_SUFFIX = ".restore_pending"

    fun stage(
        source: File,
        databaseFile: File,
        rename: (File, File) -> Unit = ::atomicRename,
    ) {
        val parent = databaseFile.parentFile
            ?: throw IOException("Database file has no parent directory")
        if (!parent.exists() && !parent.mkdirs()) {
            throw IOException("Could not create database directory")
        }

        val stagedFile = stagedFile(databaseFile)
        val tempFile = tempFile(databaseFile)
        val markerFile = markerFile(databaseFile)

        // An older pending restore must not become installable while its replacement is copied.
        deleteIfPresent(markerFile)
        deleteIfPresent(tempFile)

        try {
            source.inputStream().use { input ->
                FileOutputStream(tempFile).use { output ->
                    input.copyTo(output)
                    output.fd.sync()
                }
            }

            rename(tempFile, stagedFile)

            FileOutputStream(markerFile).use { marker ->
                marker.write(1)
                marker.fd.sync()
            }
        } catch (t: Throwable) {
            tempFile.delete()
            stagedFile.delete()
            markerFile.delete()
            throw t
        }
    }

    fun installIfPending(
        databaseFile: File,
        rename: (File, File) -> Unit = ::atomicRename,
    ): Boolean {
        val markerFile = markerFile(databaseFile)
        if (!markerFile.exists()) return false

        val stagedFile = stagedFile(databaseFile)
        if (stagedFile.exists()) {
            rename(stagedFile, databaseFile)
        }

        // These files belong to the database that was replaced. The marker remains until all of
        // them are gone, so a process interruption simply causes this cleanup to be retried.
        listOf("-wal", "-shm", "-journal")
            .map { suffix -> File(databaseFile.path + suffix) }
            .forEach(::deleteIfPresent)

        deleteIfPresent(markerFile)
        deleteIfPresent(tempFile(databaseFile))
        return true
    }

    private fun stagedFile(databaseFile: File) = File(databaseFile.path + STAGED_SUFFIX)

    private fun tempFile(databaseFile: File) = File(databaseFile.path + TEMP_SUFFIX)

    private fun markerFile(databaseFile: File) = File(databaseFile.path + MARKER_SUFFIX)

    private fun deleteIfPresent(file: File) {
        if (file.exists() && !file.delete()) {
            throw IOException("Could not delete ${file.name}")
        }
    }

    private fun atomicRename(source: File, target: File) {
        Os.rename(source.absolutePath, target.absolutePath)
    }
}
