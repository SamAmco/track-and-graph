package com.samco.trackandgraph.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

interface PermissionRequesterUseCase {
    fun initNotificationsPermissionRequester(fragment: Fragment)
    fun requestNotificationPermission(context: Context)
    fun requestAlarmAndNotificationPermission(context: Context)
}

class PermissionRequesterUseCaseImpl : PermissionRequesterUseCase {
    private lateinit var singlePermissionRequester: ActivityResultLauncher<String>
    private lateinit var multiplePermissionRequester: ActivityResultLauncher<Array<String>>

    override fun initNotificationsPermissionRequester(fragment: Fragment) {
        singlePermissionRequester =
            fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
        multiplePermissionRequester =
            fragment.registerForActivityResult(ActivityResultContracts.RequestMultiplePermissions()) { }
    }

    override fun requestNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            val permissionStatus = ContextCompat
                .checkSelfPermission(context, notificationPermission)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                singlePermissionRequester.launch(notificationPermission)
            }
        }
    }

    override fun requestAlarmAndNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) requestBothAlarmAndNotificationPermission(context)
        else if (Build.VERSION.SDK_INT >= 31) requestAlarmPermission(context)
    }

    @SuppressLint("InlinedApi")
    private fun requestAlarmPermission(context: Context) {
        val alarmPermission = Manifest.permission.SCHEDULE_EXACT_ALARM
        val alarmPermissionStatus = ContextCompat
            .checkSelfPermission(context, alarmPermission)
        if (alarmPermissionStatus != PackageManager.PERMISSION_GRANTED) {
            singlePermissionRequester.launch(alarmPermission)
        }
    }

    @SuppressLint("InlinedApi")
    private fun requestBothAlarmAndNotificationPermission(context: Context) {
        val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
        val alarmPermission = Manifest.permission.SCHEDULE_EXACT_ALARM
        val notificationPermissionStatus = ContextCompat
            .checkSelfPermission(context, notificationPermission)
        val alarmPermissionStatus = ContextCompat
            .checkSelfPermission(context, alarmPermission)
        if (notificationPermissionStatus != PackageManager.PERMISSION_GRANTED
            || alarmPermissionStatus != PackageManager.PERMISSION_GRANTED
        ) {
            multiplePermissionRequester.launch(
                listOf(
                    notificationPermission,
                    alarmPermission
                ).toTypedArray(),
            )
        }
    }
}