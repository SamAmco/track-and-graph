package com.samco.trackandgraph.permissions

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.contract.ActivityResultContracts
import androidx.core.content.ContextCompat
import androidx.fragment.app.Fragment

interface NotificationsPermissionRequesterUseCase {
    fun initNotificationsPermissionRequester(fragment: Fragment)
    fun requestNotificationPermission(context: Context)
}

class NotificationsPermissionRequesterUseCaseImpl : NotificationsPermissionRequesterUseCase {
    private lateinit var permissionRequester: ActivityResultLauncher<String>

    override fun initNotificationsPermissionRequester(fragment: Fragment) {
        permissionRequester =
            fragment.registerForActivityResult(ActivityResultContracts.RequestPermission()) {}
    }

    override fun requestNotificationPermission(context: Context) {
        if (Build.VERSION.SDK_INT >= 33) {
            val notificationPermission = Manifest.permission.POST_NOTIFICATIONS
            val permissionStatus = ContextCompat
                .checkSelfPermission(context, notificationPermission)
            if (permissionStatus != PackageManager.PERMISSION_GRANTED) {
                permissionRequester.launch(Manifest.permission.POST_NOTIFICATIONS)
            }
        }
    }
}