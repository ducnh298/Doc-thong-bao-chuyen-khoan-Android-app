package com.app.docthongbaochuyenkhoan.service

import android.content.BroadcastReceiver
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.util.Log

class BootReceiver : BroadcastReceiver() {
    override fun onReceive(context: Context, intent: Intent?) {
        val action = intent?.action
        if (action != Intent.ACTION_BOOT_COMPLETED && action != Intent.ACTION_MY_PACKAGE_REPLACED) return

        Log.i(TAG, "Received: $action — toggling NLS component and starting KeepAlive")

        // Toggle component để hệ thống tự rebind NotificationListenerService
        val component = ComponentName(context, MyNotificationListenerService::class.java)
        val pm = context.packageManager
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_DISABLED, PackageManager.DONT_KILL_APP)
        pm.setComponentEnabledSetting(component, PackageManager.COMPONENT_ENABLED_STATE_ENABLED, PackageManager.DONT_KILL_APP)

        // Khởi động foreground service để giữ process sống
        KeepAliveService.start(context)
    }

    companion object {
        private const val TAG = "BootReceiver"
    }
}
