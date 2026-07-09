package com.app.docthongbaochuyenkhoan.service

import android.app.Notification
import android.content.ComponentName
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import com.app.docthongbaochuyenkhoan.controller.NotificationMessageParser
import com.app.docthongbaochuyenkhoan.controller.NotificationReader
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.flow.TransactionFlowManager
import com.app.docthongbaochuyenkhoan.model.Bank
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.model.database.TransactionDao
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    companion object {
        var instance: MyNotificationListenerService? = null
        var isNotificationListenerEnabled: Boolean = true
        var isNotificationReceivedEnabled: Boolean = true
        var isNotificationSentEnabled: Boolean = true
    }

    override fun onListenerConnected() {
        super.onListenerConnected()
        instance = this
        isNotificationListenerEnabled = SharedPreferencesManager.isNotificationListenerEnabled()
        isNotificationReceivedEnabled = SharedPreferencesManager.isNotificationReceivedEnabled()
        isNotificationSentEnabled = SharedPreferencesManager.isNotificationSentEnabled()
    }

    private var notificationReader: NotificationReader? = null
    private var transactionDao: TransactionDao? = null
    private val job = SupervisorJob()
    private val coroutineScope = CoroutineScope(Dispatchers.IO + job)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        if (!isNotificationListenerEnabled) return

        // Đọc dữ liệu ngay trước khi launch coroutine — sbn có thể bị recycle sau callback
        val packageName = sbn.packageName ?: return
        val extras = sbn.notification?.extras ?: return
        val title = extras.getString(Notification.EXTRA_TITLE) ?: return
        val content = extras.getCharSequence(Notification.EXTRA_TEXT)?.toString() ?: return

        if (packageName.isBlank() || title.isBlank() || content.isBlank()) return

        coroutineScope.launch {
            if (transactionDao == null)
                transactionDao = AppDatabase.getDatabase(applicationContext).transactionDao()
            if (notificationReader == null)
                notificationReader = NotificationReader(applicationContext)

            processNotification(packageName, title, content)
        }
    }

    private fun processNotification(
        packageName: String,
        title: String,
        content: String
    ) {
        val transaction: Transaction? =
            NotificationMessageParser.extractTransactionFromRawNotificationMessage(
                packageName,
                title,
                content
            )

        if (transaction != null) {
            TransactionFlowManager.emitTransaction(transaction)
            coroutineScope.launch {
                transactionDao?.insert(transaction)
                if ((transaction.amount > 0 && isNotificationReceivedEnabled) || (transaction.amount < 0 && isNotificationSentEnabled))
                    notificationReader?.addNotification(transaction)
            }
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        requestRebind(ComponentName(this, MyNotificationListenerService::class.java))
    }

    override fun onDestroy() {
        super.onDestroy()
        instance = null
        job.cancel()
        notificationReader?.onDestroy()
    }
}