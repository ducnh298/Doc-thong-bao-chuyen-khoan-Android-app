package com.app.docthongbaochuyenkhoan

import android.app.Application
import android.content.Intent
import android.os.Build
import android.os.Handler
import android.os.Looper
import android.os.Process
import android.util.Log
import android.widget.Toast
import com.app.docthongbaochuyenkhoan.ui.activity.MainActivity
import kotlin.system.exitProcess

class MyCustomApplication : Application() {

    override fun onCreate() {
        super.onCreate()

        //Set UncaughtExceptionHandler globally
        Thread.setDefaultUncaughtExceptionHandler { thread, exception ->
            handleUncaughtException(thread, exception)
        }
    }

    private fun handleUncaughtException(thread: Thread, exception: Throwable) {
        exception.printStackTrace()

        Toast.makeText(
            applicationContext,
            "Lỗi không xác định. Vui lòng khởi động lại ứng dụng.",
            Toast.LENGTH_LONG
        ).show()
        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            addFlags(Intent.FLAG_ACTIVITY_NEW_TASK)
            addFlags(Intent.FLAG_ACTIVITY_CLEAR_TASK)
        }
        applicationContext.startActivity(intent)

        Handler(Looper.getMainLooper()).postDelayed({
            Process.killProcess(Process.myPid())
            exitProcess(1) // Đảm bảo ứng dụng thoát với mã lỗi
        }, 100)
    }

    companion object {
        fun isSamsungDevice(): Boolean {
            val manufacturer = Build.MANUFACTURER
            val brand = Build.BRAND
            Log.d("TAG", "Manufacturer: $manufacturer, Brand: $brand")
//        return manufacturer.equals("samsung", ignoreCase = true) ||
//                brand.equals("samsung", ignoreCase = true)
            return true
        }
    }
}