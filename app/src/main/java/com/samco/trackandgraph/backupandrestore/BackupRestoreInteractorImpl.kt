package com.samco.trackandgraph.backupandrestore

import android.content.Context
import android.content.Intent
import android.database.sqlite.SQLiteDatabase
import android.net.Uri
import androidx.sqlite.db.SimpleSQLiteQuery
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.samco.trackandgraph.backupandrestore.dto.AutoBackupInfo
import com.samco.trackandgraph.backupandrestore.dto.BackupConfig
import com.samco.trackandgraph.backupandrestore.dto.BackupResult
import com.samco.trackandgraph.backupandrestore.dto.RestoreResult
import com.samco.trackandgraph.data.database.TNG_DATABASE_VERSION
import com.samco.trackandgraph.data.database.TrackAndGraphDatabase
import com.samco.trackandgraph.data.di.IODispatcher
import com.samco.trackandgraph.helpers.PrefHelper
import com.samco.trackandgraph.reminders.AlarmInteractor
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.CoroutineDispatcher
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.onStart
import kotlinx.coroutines.withContext
import org.threeten.bp.Instant
import org.threeten.bp.OffsetDateTime
import org.threeten.bp.ZoneId
import org.threeten.bp.temporal.ChronoUnit
import timber.log.Timber
import java.io.File
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import kotlin.math.max

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

    private fun validUnit(unit: ChronoUnit) =
        setOf(
            ChronoUnit.HOURS,
            ChronoUnit.DAYS,
            ChronoUnit.WEEKS
        ).contains(unit)

    override fun validAutoBackupConfiguration(backupConfig: BackupConfig): Boolean {
        return backupConfig.firstDate.isAfter(OffsetDateTime.now()) &&
                backupConfig.interval > 0 &&
                validUnit(backupConfig.units)
    }

    companion object {
        private const val AUTO_BACKUP_WORK_NAME = "auto_backup"
    }

    override suspend fun backupNowAndSetAutoBackupConfig(backupConfig: BackupConfig): BackupResult {
        try {
            context.contentResolver.takePersistableUriPermission(
                backupConfig.uri,
                Intent.FLAG_GRANT_READ_URI_PERMISSION or Intent.FLAG_GRANT_WRITE_URI_PERMISSION
            )

            prefHelper.setAutoBackupConfig(backupConfig.asPrefHelperData())

            val backupResult = performAutoBackup()

            if (backupResult != BackupResult.SUCCESS) {
                prefHelper.setAutoBackupConfig(null)
                return backupResult
            }

            setupWorkManager(backupConfig)

            backupConfigChanged.tryEmit(Unit)
            return backupResult
        } catch (t: Throwable) {
            Timber.e(t, "Error setting auto backup config")
            return BackupResult.FAIL_COULD_NOT_WRITE_TO_FILE
        }
    }

    private fun setupWorkManager(backupConfig: BackupConfig) {
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
                        ?.takeIf { !it.state.isFinished }
                        ?.nextScheduleTimeMillis
                }
        } ?: return null

        val nextScheduledOdt = OffsetDateTime.ofInstant(
            Instant.ofEpochMilli(nextScheduledMillis),
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