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

package com.samco.trackandgraph.backupandrestore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.CoroutineWorker
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.ForegroundInfo
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkInfo
import androidx.work.WorkManager
import androidx.work.WorkerParameters
import com.samco.trackandgraph.base.R
import com.samco.trackandgraph.base.database.TNG_DATABASE_VERSION
import com.samco.trackandgraph.base.database.TrackAndGraphDatabase
import com.samco.trackandgraph.base.helpers.PrefHelper
import com.samco.trackandgraph.base.model.AlarmInteractor
import com.samco.trackandgraph.base.model.di.IODispatcher
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant.ofEpochMilli
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.io.File
import java.lang.Long.max
import java.util.concurrent.TimeUnit
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

data class BackupConfig(
    val uri: Uri,
    val firstDate: OffsetDateTime,
    val interval: Int,
    val units: ChronoUnit
) {
    companion object {
        private fun chronoUnitToPrefUnit(chronoUnit: ChronoUnit): PrefHelper.BackupConfigUnit =
            when (chronoUnit) {
                ChronoUnit.HOURS -> PrefHelper.BackupConfigUnit.HOURS
                ChronoUnit.DAYS -> PrefHelper.BackupConfigUnit.DAYS
                ChronoUnit.WEEKS -> PrefHelper.BackupConfigUnit.WEEKS
                else -> throw IllegalArgumentException("Invalid unit")
            }

        private fun prefUnitToChronoUnit(prefUnit: PrefHelper.BackupConfigUnit): ChronoUnit =
            when (prefUnit) {
                PrefHelper.BackupConfigUnit.HOURS -> ChronoUnit.HOURS
                PrefHelper.BackupConfigUnit.DAYS -> ChronoUnit.DAYS
                PrefHelper.BackupConfigUnit.WEEKS -> ChronoUnit.WEEKS
            }
    }

    constructor(prefHelperData: PrefHelper.BackupConfigData) : this(
        uri = prefHelperData.uri,
        firstDate = prefHelperData.firstDate,
        interval = prefHelperData.interval,
        units = prefUnitToChronoUnit(prefHelperData.units)
    )

    fun asPrefHelperData() = PrefHelper.BackupConfigData(
        uri = uri,
        firstDate = firstDate,
        interval = interval,
        units = chronoUnitToPrefUnit(units)
    )

}

data class AutoBackupInfo(
    val uri: Uri,
    val nextScheduled: OffsetDateTime,
    val interval: Int,
    val units: ChronoUnit,
    val lastSuccessful: OffsetDateTime?
)

interface BackupRestoreInteractor {

    val autoBackupConfig: Flow<AutoBackupInfo?>

    suspend fun performManualBackup(uri: Uri): BackupResult

    suspend fun performManualRestore(uri: Uri): RestoreResult

    suspend fun performAutoBackup(): BackupResult

    suspend fun backupNowAndSetAutoBackupConfig(backupConfig: BackupConfig)

    fun validAutoBackupConfiguration(backupConfig: BackupConfig): Boolean

    suspend fun getAutoBackupInfo(): AutoBackupInfo?

    fun disableAutoBackup()
}

class BackupRestoreInteractorImpl @Inject constructor(
    private val database: TrackAndGraphDatabase,
    private val alarmInteractor: AlarmInteractor,
    @ApplicationContext private val context: Context,
    private val prefHelper: PrefHelper,
    @IODispatcher private val io: CoroutineDispatcher
) : BackupRestoreInteractor {

    private val backupConfigChanged = MutableSharedFlow<Unit>(extraBufferCapacity = 1)

    override val autoBackupConfig: Flow<AutoBackupInfo?> = backupConfigChanged
        .onStart { emit(Unit) }
        .map { getAutoBackupInfo() }

    override suspend fun performManualBackup(uri: Uri): BackupResult {
        val writableDatabase = database.openHelper.writableDatabase
        val databaseFile = writableDatabase.path
            ?.let { path -> File(path).takeIf { it.exists() } }
            ?: return BackupResult.FAIL_COULD_NOT_FIND_DATABASE

        val outputStream = try {
            context.contentResolver.openOutputStream(uri, "wt")
        } catch (t: Throwable) {
            return BackupResult.FAIL_COULD_NOT_WRITE_TO_FILE
        }

        outputStream?.use { outStream ->
            try {
                writableDatabase.query(SimpleSQLiteQuery("PRAGMA wal_checkpoint(full)")).apply {
                    moveToFirst()
                    if (getInt(0) != 0) return BackupResult.FAIL_COULD_NOT_COPY
                    close()
                }

                databaseFile.inputStream().use { it.copyTo(outStream) }

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
        val uri = prefHelper.getAutoBackupConfig()?.uri
            ?: return BackupResult.FAIL_COULD_NOT_WRITE_TO_FILE
        return performManualBackup(uri).also {
            if (it == BackupResult.SUCCESS) prefHelper.setLastAutoBackupTime(OffsetDateTime.now())
            backupConfigChanged.emit(Unit)
        }
    }

    private fun validUri(uri: Uri): Boolean {
        try {
            context.contentResolver.openOutputStream(uri, "w")
                ?.use { return true }
                ?: return false
        } catch (t: Throwable) {
            return false
        }
    }

    private fun validUnit(unit: ChronoUnit) =
        setOf(
            ChronoUnit.HOURS,
            ChronoUnit.DAYS,
            ChronoUnit.WEEKS
        ).contains(unit)

    override fun validAutoBackupConfiguration(backupConfig: BackupConfig): Boolean {
        return validUri(backupConfig.uri) &&
                backupConfig.firstDate.isAfter(OffsetDateTime.now()) &&
                backupConfig.interval > 0 &&
                validUnit(backupConfig.units)
    }

    companion object {
        private const val AUTO_BACKUP_WORK_NAME = "auto_backup"
        private const val AUTO_BACKUP_NOTIFICATION_CHANNEL_ID = "auto_backup_channel"

        //Hardcoded ids because we only have one notification (there is no significance to this number)
        private const val AUTO_BACKUP_NOTIFICATION_ID = 51234
        private const val AUTO_BACKUP_FAIL_NOTIFICATION_ID = 51235
    }

    @HiltWorker
    class PeriodicBackupWorker @AssistedInject constructor(
        @Assisted context: Context,
        @Assisted workerParameters: WorkerParameters,
        private val interactor: BackupRestoreInteractor
    ) : CoroutineWorker(context, workerParameters) {

        override suspend fun doWork(): Result {
            try {
                setForeground(createForegroundInfo())
            } catch (e: Exception) {
                //This can happen sometimes when the app is in the background
                Timber.e(e, "setForeground failed")
            }

            return when (val result = interactor.performAutoBackup()) {
                BackupResult.SUCCESS -> Result.success()
                BackupResult.FAIL_COULD_NOT_WRITE_TO_FILE,
                BackupResult.FAIL_COULD_NOT_FIND_DATABASE,
                BackupResult.FAIL_COULD_NOT_COPY -> {
                    showFailNotification(result)
                    Result.failure()
                }
            }
        }

        private fun showFailNotification(result: BackupResult) {
            val channelId = AUTO_BACKUP_NOTIFICATION_CHANNEL_ID
            val title = applicationContext.getString(R.string.backup_failed_notification_title)

            // Create a Notification channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel()
            }

            val contentText = when (result) {
                BackupResult.FAIL_COULD_NOT_COPY,
                BackupResult.FAIL_COULD_NOT_FIND_DATABASE -> applicationContext
                    .getString(R.string.backup_failed_notification_could_not_copy)

                BackupResult.FAIL_COULD_NOT_WRITE_TO_FILE -> applicationContext
                    .getString(R.string.backup_failed_notification_could_not_write_to_file)

                else -> throw IllegalArgumentException("Invalid result")
            }

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setContentTitle(title)
                .setContentText(contentText)
                .setTicker(title)
                .setSmallIcon(R.drawable.notification_icon)
                .build()

            val notificationManager =
                applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

            notificationManager.notify(AUTO_BACKUP_FAIL_NOTIFICATION_ID, notification)
        }

        override suspend fun getForegroundInfo(): ForegroundInfo {
            return createForegroundInfo()
        }

        @RequiresApi(Build.VERSION_CODES.O)
        private fun createChannel() {
            val id = AUTO_BACKUP_NOTIFICATION_CHANNEL_ID
            val name = applicationContext.getString(R.string.backup_notification_channel_name)
            val importance = NotificationManager.IMPORTANCE_LOW
            val channel = NotificationChannel(id, name, importance).apply {
                description = applicationContext.getString(
                    R.string.backup_notification_channel_description
                )
            }
            (applicationContext.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager).apply {
                createNotificationChannel(channel)
            }
        }

        private fun createForegroundInfo(): ForegroundInfo {
            val channelId = AUTO_BACKUP_NOTIFICATION_CHANNEL_ID
            val title = applicationContext.getString(R.string.backup_notification_title)

            // Create a Notification channel if necessary
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                createChannel()
            }

            val notification = NotificationCompat.Builder(applicationContext, channelId)
                .setSilent(true)
                .setOngoing(true)
                .setContentTitle(title)
                .setTicker(title)
                .setSmallIcon(R.drawable.notification_icon)
                .build()

            return ForegroundInfo(AUTO_BACKUP_NOTIFICATION_ID, notification)
        }
    }

    override suspend fun backupNowAndSetAutoBackupConfig(backupConfig: BackupConfig) {
        context.contentResolver.takePersistableUriPermission(
            backupConfig.uri,
            Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
        )

        prefHelper.setAutoBackupConfig(backupConfig.asPrefHelperData())

        performAutoBackup()

        val unit = when (backupConfig.units) {
            ChronoUnit.HOURS -> TimeUnit.HOURS
            ChronoUnit.DAYS -> TimeUnit.DAYS
            ChronoUnit.WEEKS -> TimeUnit.DAYS
            else -> throw IllegalArgumentException("Invalid unit")
        }

        val interval = when (backupConfig.units) {
            ChronoUnit.WEEKS -> backupConfig.interval.toLong() * 7
            ChronoUnit.HOURS,
            ChronoUnit.DAYS -> backupConfig.interval.toLong()

            else -> throw IllegalArgumentException("Invalid unit")
        }

        val secondsDelay = max(
            10L,
            backupConfig.firstDate.toEpochSecond() - OffsetDateTime.now().toEpochSecond()
        )

        val periodicWorkRequest =
            PeriodicWorkRequestBuilder<PeriodicBackupWorker>(interval, unit)
                .setInitialDelay(secondsDelay, TimeUnit.SECONDS)
                .build()

        WorkManager.getInstance(context).enqueueUniquePeriodicWork(
            AUTO_BACKUP_WORK_NAME,
            ExistingPeriodicWorkPolicy.CANCEL_AND_REENQUEUE,
            periodicWorkRequest
        )

        backupConfigChanged.tryEmit(Unit)
    }

    override suspend fun getAutoBackupInfo(): AutoBackupInfo? {
        val config = getAutoBackupConfiguration() ?: return null

        val nextScheduledMillis = withContext(io) {
            WorkManager.getInstance(context)
                .getWorkInfosForUniqueWork(AUTO_BACKUP_WORK_NAME)
                .get()
                ?.let { workInfos ->
                    workInfos
                        .firstOrNull()
                        ?.takeIf { it.state == WorkInfo.State.ENQUEUED }
                        ?.nextScheduleTimeMillis
                }
        } ?: return null

        val nextScheduledOdt = OffsetDateTime.ofInstant(
            ofEpochMilli(nextScheduledMillis),
            ZoneId.systemDefault()
        )

        val lastSuccessful = prefHelper.getLastAutoBackupTime()

        return AutoBackupInfo(
            uri = config.uri,
            nextScheduled = nextScheduledOdt,
            interval = config.interval,
            units = config.units,
            lastSuccessful = lastSuccessful
        )
    }

    private fun getAutoBackupConfiguration(): BackupConfig? {
        return prefHelper.getAutoBackupConfig()?.let { BackupConfig(it) }
    }

    override fun disableAutoBackup() {
        prefHelper.setAutoBackupConfig(null)
        WorkManager.getInstance(context).cancelUniqueWork(AUTO_BACKUP_WORK_NAME)
        backupConfigChanged.tryEmit(Unit)
    }
}