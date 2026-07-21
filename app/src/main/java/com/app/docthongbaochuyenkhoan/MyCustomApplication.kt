package com.app.docthongbaochuyenkhoan

import android.app.Application
import android.content.Intent
import android.content.pm.PackageManager
import android.os.Build
import android.os.Process
import android.util.Log
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.ui.activity.MainActivity
import java.io.File
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.system.exitProcess

class MyCustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()
        SharedPreferencesManager.init(this)

        if (SharedPreferencesManager.getNotificationSound().isBlank()) {
            val defaultUri = "android.resource://$packageName/${R.raw.ting}"
            SharedPreferencesManager.saveNotificationSound(defaultUri)
        }

        Thread.setDefaultUncaughtExceptionHandler { _, exception ->
            handleUncaughtException(exception)
        }
    }

    private fun handleUncaughtException(exception: Throwable) {
        exception.printStackTrace()
        val recentCrash = wasRecentCrash()
        saveCrashLog(exception)

        if (!recentCrash) {
            try {
                val intent = Intent(applicationContext, MainActivity::class.java).apply {
                    addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TASK)
                }
                applicationContext.startActivity(intent)
            } catch (e: Exception) {
                Log.e(TAG, "Failed to restart after crash", e)
            }
        }

        // Dùng daemon thread thay vì Handler — an toàn hơn khi crash trên main thread
        Thread {
            Thread.sleep(500)
            Process.killProcess(Process.myPid())
            exitProcess(1)
        }.also { it.isDaemon = true }.start()
    }

    private fun wasRecentCrash(): Boolean {
        return try {
            val tsFile = File(filesDir, CRASH_TIMESTAMP_FILE)
            if (!tsFile.exists()) false
            else {
                val lastTs = tsFile.readText().toLongOrNull() ?: return false
                System.currentTimeMillis() - lastTs < 10_000L
            }
        } catch (e: Exception) {
            false
        }
    }

    private fun saveCrashLog(exception: Throwable) {
        try {
            val timestamp = SimpleDateFormat("yyyy-MM-dd HH:mm:ss", Locale.getDefault()).format(Date())
            val pInfo = try {
                packageManager.getPackageInfo(packageName, 0)
            } catch (e: PackageManager.NameNotFoundException) { null }
            val versionName = pInfo?.versionName ?: "?"
            val versionCode = pInfo?.longVersionCode ?: 0
            val deviceInfo = "Device: ${Build.MANUFACTURER} ${Build.MODEL}\n" +
                    "Android: ${Build.VERSION.RELEASE} (API ${Build.VERSION.SDK_INT})\n" +
                    "App: v$versionName ($versionCode)"
            val log = "$timestamp\n$deviceInfo\n\n${exception.stackTraceToString()}"
            File(filesDir, CRASH_LOG_FILE).writeText(log)
            File(filesDir, CRASH_TIMESTAMP_FILE).writeText(System.currentTimeMillis().toString())
            Log.e(TAG, "Crash log saved to $CRASH_LOG_FILE — ${exception.javaClass.simpleName}: ${exception.message}")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to save crash log", e)
        }
    }

    companion object {
        private const val TAG = "MyCustomApplication"
        const val CRASH_LOG_FILE = "crash_log.txt"
        const val CRASH_TIMESTAMP_FILE = "crash_timestamp.txt"

        fun isSamsungDevice(): Boolean {
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            Log.d(TAG, "Manufacturer: $manufacturer, Brand: $brand")
            return true
        }
    }
}
