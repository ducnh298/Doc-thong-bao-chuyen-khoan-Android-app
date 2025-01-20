package com.app.docthongbaochuyenkhoan.service

import android.app.Notification
import android.content.ComponentName
import android.content.pm.PackageManager
import android.service.notification.NotificationListenerService
import android.service.notification.StatusBarNotification
import android.util.Log
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
import kotlinx.coroutines.launch

class MyNotificationListenerService : NotificationListenerService() {

    companion object {
        var isNotificationListenerEnabled: Boolean = true
    }

    override fun onCreate() {
        super.onCreate()

        if (!SharedPreferencesManager.isInitialized())
            SharedPreferencesManager.init(applicationContext)
        isNotificationListenerEnabled = SharedPreferencesManager.isNotificationListenerEnabled()
    }

    private var notificationReader: NotificationReader? = null
    private var transactionDao: TransactionDao? = null
    private val coroutineScope = CoroutineScope(Dispatchers.IO)

    override fun onNotificationPosted(sbn: StatusBarNotification) {
        coroutineScope.launch {
            sbn.notification?.let {
                if (isNotificationListenerEnabled) {
                    val packageName = sbn.packageName
                    val extras = sbn.notification.extras
                    val title = extras.getString(Notification.EXTRA_TITLE).toString()
                    val content = extras.getCharSequence(Notification.EXTRA_TEXT).toString()

                    Log.d(
                        "NotificationListener",
                        "onNotificationPosted: packageName:$packageName\nTitle: $title\nContent: $content"
                    )

                    if (packageName.isNullOrBlank() || title.isBlank() || content.isBlank())
                        return@launch

                    if (isBankNotification(packageName)) {
                        if (transactionDao == null)
                            transactionDao =
                                AppDatabase.getDatabase(applicationContext).transactionDao()

                        if (notificationReader == null)
                            notificationReader = NotificationReader(applicationContext)

                        processNotification(packageName, title, content)
                    }
                }
            }
        }
    }

    // com.mbmobile, vn.com.techcombank.bb.app, com.VCB

    private fun isBankNotification(packageName: String): Boolean {
        return Bank.entries.any { bank ->
            bank.aliases.any { alias -> packageName.contains(alias, ignoreCase = true) }
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
            emitTransactionFlow(transaction)
            saveTransactionToDatabase(transaction)

            notificationReader?.addNotification(transaction)
        }
    }

    private fun emitTransactionFlow(transaction: Transaction) {
        coroutineScope.launch {
            TransactionFlowManager.emitTransaction(transaction)
        }
    }

    private fun saveTransactionToDatabase(transaction: Transaction) {
        coroutineScope.launch {
            transactionDao?.insert(transaction)
        }
    }

    override fun onListenerDisconnected() {
        super.onListenerDisconnected()
        // Listen when Notification Listener is interrupted
        restartNotificationService()
    }

    private fun restartNotificationService() {
        val componentName = ComponentName(this, MyNotificationListenerService::class.java)
        val pm = packageManager

        // Disable NotificationListenerService
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
            PackageManager.DONT_KILL_APP
        )

        // Enable NotificationListenerService
        pm.setComponentEnabledSetting(
            componentName,
            PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
            PackageManager.DONT_KILL_APP
        )
        Log.d("NotificationListener", "NotificationListenerService restarted")
    }

    override fun onDestroy() {
        super.onDestroy()
        notificationReader?.onDestroy()
    }
}