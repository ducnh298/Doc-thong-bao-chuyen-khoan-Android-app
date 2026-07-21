package com.app.docthongbaochuyenkhoan.service

import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.Service
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import android.util.Log
import androidx.core.app.NotificationCompat
import com.app.docthongbaochuyenkhoan.R

/**
 * Foreground service để giữ process sống trên các ROM aggressive (Xiaomi, Huawei, Oppo…).
 * Không có foreground service → ROM có thể kill process → NotificationListenerService mất kết nối.
 */
class KeepAliveService : Service() {

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground(NOTIFICATION_ID, buildNotification())
        Log.i(TAG, "KeepAliveService started")
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        // START_STICKY: nếu bị kill, Android tự khởi động lại service
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Đọc thông báo nền",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Giữ app hoạt động để không bỏ lỡ thông báo chuyển khoản"
                setShowBadge(false)
            }
            (getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager)
                .createNotificationChannel(channel)
        }
    }

    private fun buildNotification() = NotificationCompat.Builder(this, CHANNEL_ID)
        .setContentTitle(getString(R.string.app_name))
        .setContentText("Đang chạy nền — sẵn sàng đọc thông báo")
        .setSmallIcon(R.mipmap.ic_launcher)
        .setPriority(NotificationCompat.PRIORITY_LOW)
        .setOngoing(true)
        .setSilent(true)
        .build()

    companion object {
        private const val TAG = "KeepAliveService"
        const val CHANNEL_ID = "keep_alive_channel"
        private const val NOTIFICATION_ID = 1001

        fun start(context: Context) {
            val intent = Intent(context, KeepAliveService::class.java)
            try {
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                    context.startForegroundService(intent)
                } else {
                    context.startService(intent)
                }
            } catch (e: Exception) {
                // ForegroundServiceStartNotAllowedException (API 31+) hoặc các lỗi background start.
                // Không crash app — service không critical, NLS vẫn hoạt động được.
                Log.e(TAG, "Cannot start KeepAliveService: ${e.message}")
            }
        }
    }
}
