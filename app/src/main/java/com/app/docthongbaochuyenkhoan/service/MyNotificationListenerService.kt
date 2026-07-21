package com.app.docthongbaochuyenkhoan.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.app.docthongbaochuyenkhoan.controller.NotificationMessageParser
import com.app.docthongbaochuyenkhoan.controller.NotificationReader
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.flow.TransactionFlowManager
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.model.database.TransactionDao
import android.util.Log
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    private lateinit var notificationReader: NotificationReader
    private lateinit var transactionDao: TransactionDao
    private val job = SupervisorJob()
    // CoroutineExceptionHandler: log lỗi từ coroutine thay vì để crash app
    private val exceptionHandler = CoroutineExceptionHandler { _, throwable ->
        Log.e(TAG, "Unhandled coroutine exception", throwable)
    }
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job + exceptionHandler)

    override fun onCreate() {
        super.onCreate()
        SharedPreferencesManager.init(applicationContext)
        transactionDao = AppDatabase.getDatabase(applicationContext).transactionDao()
        notificationReader = NotificationReader(applicationContext)
        Log.i(TAG, "Service created")
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        isNotificationListenerEnabled = SharedPreferencesManager.isNotificationListenerEnabled()
        isNotificationReceivedEnabled = SharedPreferencesManager.isNotificationReceivedEnabled()
        isNotificationSentEnabled = SharedPreferencesManager.isNotificationSentEnabled()
        // Đảm bảo foreground service chạy để ROM không kill process giữa chừng
        KeepAliveService.start(applicationContext)
        Log.i(TAG, "Listener connected — enabled=$isNotificationListenerEnabled received=$isNotificationReceivedEnabled sent=$isNotificationSentEnabled")
    }

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isNotificationListenerEnabled) return

        // Đọc dữ liệu ngay trước khi launch coroutine — sbn có thể bị recycle sau callback
        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        if (packageName.isBlank() || title.isBlank() || content.isBlank()) return

        Log.d(TAG, "onNotificationPosted pkg=$packageName")
        coroutineScope.launch {
            processNotification(packageName, title, content)
        }
    }

    private fun processNotification(
        packageName: String,
        title: String,
        content: String
    ) {
        try {
            val transaction: Transaction? =
                NotificationMessageParser.extractTransactionFromRawNotificationMessage(
                    packageName,
                    title,
                    content
                )

            if (transaction != null) {
                Log.i(TAG, "Transaction parsed: bank=${transaction.bank} amount=${transaction.amount}")
                TransactionFlowManager.emitTransaction(transaction)
                coroutineScope.launch {
                    transactionDao.insert(transaction)
                    Log.d(TAG, "Transaction saved to DB")
                    val shouldRead = (transaction.amount > 0 && isNotificationReceivedEnabled) ||
                            (transaction.amount < 0 && isNotificationSentEnabled)
                    if (shouldRead) {
                        notificationReader.addNotification(transaction)
                    } else {
                        Log.d(TAG, "TTS skipped — read setting disabled for this direction")
                    }
                }
            } else {
                Log.d(TAG, "No transaction extracted from pkg=$packageName")
            }
        } catch (e: Exception) {
            Log.e(TAG, "Error processing notification from $packageName", e)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        Log.w(TAG, "Listener disconnected — requesting rebind")
        requestRebind(ComponentName(this, MyNotificationListenerService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        Log.i(TAG, "Service destroyed")
        instance = null
        job.cancel()
        if (::notificationReader.isInitialized) notificationReader.onDestroy()
    }

    companion object {
        private const val TAG = "NotifService"
        var instance: MyNotificationListenerService? = null
        var isNotificationListenerEnabled: Boolean = true
        var isNotificationReceivedEnabled: Boolean = true
        var isNotificationSentEnabled: Boolean = true
    }
}
