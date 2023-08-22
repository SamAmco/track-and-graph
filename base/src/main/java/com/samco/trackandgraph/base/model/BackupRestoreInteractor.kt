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
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
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
        val databaseFileOutputStream = database.openHelper.writableDatabase.path
            ?.let { path -> File(path).takeIf { it.exists() } }
            ?.outputStream()
            ?: return RestoreResult.FAIL_COULD_NOT_COPY

        val inputStream = context.contentResolver.openInputStream(uri)
            ?: return RestoreResult.FAIL_COULD_NOT_FIND_OR_READ_DATABASE_FILE

        databaseFileOutputStream.use { outputStream ->
            try {
                alarmInteractor.clearAlarms()
                database.openHelper.close()
                inputStream.use { it.copyTo(outputStream) }

                return RestoreResult.SUCCESS
            } catch (t: Throwable) {
                Timber.e(t, "Error clearing alarms before restore")
                return RestoreResult.FAIL_COULD_NOT_COPY
            }
        }
    }

    override suspend fun performAutoBackup(): BackupResult {
        TODO("Not yet implemented")
    }

    override fun setAutoBackupLocation(uri: Uri) {
        TODO("Not yet implemented")
    }
}