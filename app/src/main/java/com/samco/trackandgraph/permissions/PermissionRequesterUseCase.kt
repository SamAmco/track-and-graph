package com.samco.trackandgraph.permissions

import android.Manifest
import android.annotation.SuppressLint
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.platform.LocalContext
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment


/**
 * Composable function that handles notification permission requests.
 * Returns a function that can be called to request notification permission.
 */
@Composable
fun rememberNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* We don't care about the result */ }

    val permissionRequestFunction = {
        if (Build.VERSION.SDK_INT >= 33) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            val permissionStatus = ContextCompat.checkSelfPermission(context, notificationPermission)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                singlePermissionLauncher.launch(notificationPermission)
            }
        }
    }

    return remember { permissionRequestFunction }
}

/**
 * Composable function that handles alarm and notification permission requests.
 * Returns a function that can be called to request both alarm and notification permissions.
 */
@Composable
fun rememberAlarmAndNotificationPermissionRequester(): () -> Unit {
    val context = LocalContext.current

    val singlePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestPermission()
    ) { /* We don't care about the result */ }

    val multiplePermissionLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { /* We don't care about the result */ }

    val permissionRequestFunction = {
        if (Build.VERSION.SDK_INT >= 33) {
            // Request both alarm and notification permissions for Android 33+
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            val alarmPermission = Manifest.permission.SCHEDULE_EXACT_ALARM
            val notificationPermissionStatus = ContextCompat.checkSelfPermission(context, notificationPermission)
            val alarmPermissionStatus = ContextCompat.checkSelfPermission(context, alarmPermission)
            
            if (notificationPermissionStatus != PackageManager.PERMISSION_GRANTED
                || alarmPermissionStatus != PackageManager.PERMISSION_GRANTED
            ) {
                multiplePermissionLauncher.launch(
                    arrayOf(notificationPermission, alarmPermission)
                )
            }
        } else if (Build.VERSION.SDK_INT >= 31) {
            // Request only alarm permission for Android 31-32
            val alarmPermission = Manifest.permission.SCHEDULE_EXACT_ALARM
            val alarmPermissionStatus = ContextCompat.checkSelfPermission(context, alarmPermission)
            if (alarmPermissionStatus != PackageManager.PERMISSION_GRANTED) {
                singlePermissionLauncher.launch(alarmPermission)
            }
        }
    }

    return remember { permissionRequestFunction }
}