package com.samco.trackandgraph.backupandrestore

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.content.pm.ServiceInfo.FOREGROUND_SERVICE_TYPE_DATA_SYNC
import android.os.Build
import androidx.annotation.RequiresApi
import androidx.core.app.NotificationCompat
import androidx.hilt.work.HiltWorker
import androidx.work.CoroutineWorker
import androidx.work.ForegroundInfo
import androidx.work.WorkerParameters
import com.samco.trackandgraph.backupandrestore.dto.BackupResult
import com.samco.trackandgraph.R
import dagger.assisted.Assisted
import dagger.assisted.AssistedInject
import timber.log.Timber

@HiltWorker
class PeriodicBackupWorker @AssistedInject constructor(
    @Assisted context: Context,
    @Assisted workerParameters: WorkerParameters,
    private val interactor: BackupRestoreInteractor
) : CoroutineWorker(context, workerParameters) {

    companion object {
        private const val AUTO_BACKUP_NOTIFICATION_CHANNEL_ID = "auto_backup_channel"
        //Hardcoded ids because we only have one notification (there is no significance to this number)
        private const val AUTO_BACKUP_FAIL_NOTIFICATION_ID = 51235
        private const val AUTO_BACKUP_NOTIFICATION_ID = 51234
    }

    override suspend fun doWork(): Result {
        Timber.d("Performing auto backup")
        try {
            //TODO I don't actually recall seeing this work in a long time. Might not work on later
            // versions of Android?
            setForeground(createForegroundInfo())
        } catch (t: Throwable) {
            //This can happen sometimes when the app is in the background
            Timber.e(t, "setForeground failed")
        }

        return when (val result = interactor.performAutoBackup()) {
            BackupResult.SUCCESS -> {
                Timber.d("Auto backup successful")
                Result.success()
            }
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

        return if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            ForegroundInfo(AUTO_BACKUP_NOTIFICATION_ID, notification, FOREGROUND_SERVICE_TYPE_DATA_SYNC)
        } else {
            ForegroundInfo(AUTO_BACKUP_NOTIFICATION_ID, notification)
        }
    }
}