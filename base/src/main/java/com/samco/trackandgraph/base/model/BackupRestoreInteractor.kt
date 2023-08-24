/*
 * This file is part of Track & Graph
 *
 * Track & Graph is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Track & Graph is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Track & Graph.  If not, see <https://www.gnu.org/licenses/>.
 */

package com.samco.trackandgraph.base.model

import android.content.Context
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import com.samco.trackandgraph.base.database.TNG_DATABASE_VERSION
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject

enum class BackupResult {
    SUCCESS,
    FAIL_COULD_NOT_WRITE_TO_FILE,
    FAIL_COULD_NOT_FIND_DATABASE,
    FAIL_COULD_NOT_COPY
}

enum class RestoreResult {
    SUCCESS,
    FAIL_INVALID_DATABASE,
    FAIL_COULD_NOT_FIND_OR_READ_DATABASE_FILE,
    FAIL_COULD_NOT_COPY
}

interface BackupRestoreInteractor {

    suspend fun performManualBackup(uri: Uri): BackupResult

    suspend fun performManualRestore(uri: Uri): RestoreResult

    suspend fun performAutoBackup(): BackupResult

    fun setAutoBackupLocation(uri: Uri)

}

internal class BackupRestoreInteractorImpl @Inject constructor(
    private val database: TrackAndGraphDatabase,
    private val alarmInteractor: AlarmInteractor,
    @ApplicationContext private val context: Context,
) : BackupRestoreInteractor {
    override suspend fun performManualBackup(uri: Uri): BackupResult {
        val writableDatabase = database.openHelper.writableDatabase
        val databaseFile = writableDatabase.path
            ?.let { path -> File(path).takeIf { it.exists() } }
            ?: return BackupResult.FAIL_COULD_NOT_FIND_DATABASE

        context.contentResolver.openOutputStream(uri)?.use { outputStream ->
            try {
                writableDatabase.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(full)")).apply {
                    moveToFirst()
                    if (getInt(0) != 0) return BackupResult.FAIL_COULD_NOT_COPY
                    close()
                }

                databaseFile.inputStream().use { it.copyTo(outputStream) }

                return BackupResult.SUCCESS
            } catch (t: Throwable) {
                Timber.e(t, "Error backing up database")
                return BackupResult.FAIL_COULD_NOT_COPY
            }
        } ?: return BackupResult.FAIL_COULD_NOT_WRITE_TO_FILE
    }

    override suspend fun performManualRestore(uri: Uri): RestoreResult {
        context.contentResolver.openInputStream(uri)?.use { inputStream ->
            try {
                val tempFile = File.createTempFile("dbrestore", "db")

                tempFile.outputStream().use { tempOutStream ->
                    inputStream.use { it.copyTo(tempOutStream) }
                }

                if (!isValidDatabase(tempFile)) {
                    return RestoreResult.FAIL_INVALID_DATABASE
                }

                alarmInteractor.clearAlarms()
                database.openHelper.close()

                tempFile.inputStream().use {
                    val databaseFileOutputStream = database.openHelper.writableDatabase.path
                        ?.let { path -> File(path).takeIf { it.exists() } }
                        ?.outputStream()
                        ?: return RestoreResult.FAIL_COULD_NOT_COPY

                    it.copyTo(databaseFileOutputStream)
                }

                tempFile.deleteOnExit()

                return RestoreResult.SUCCESS
            } catch (t: Throwable) {
                Timber.e(t, "Error clearing alarms before restore")
                return RestoreResult.FAIL_COULD_NOT_COPY
            }
        } ?: return RestoreResult.FAIL_COULD_NOT_FIND_OR_READ_DATABASE_FILE
    }

    private fun isValidDatabase(dbFile: File): Boolean {
        var db: SQLiteDatabase? = null
        try {
            db = SQLiteDatabase.openDatabase(
                dbFile.path,
                null,
                SQLiteDatabase.OPEN_READONLY
            )

            if (db.version > TNG_DATABASE_VERSION) return false
        } catch (t: Throwable) {
            return false
        } finally {
            db?.close()
        }
        return true
    }

    override suspend fun performAutoBackup(): BackupResult {
        TODO("Not yet implemented")
    }

    override fun setAutoBackupLocation(uri: Uri) {
        TODO("Not yet implemented")
    }
}