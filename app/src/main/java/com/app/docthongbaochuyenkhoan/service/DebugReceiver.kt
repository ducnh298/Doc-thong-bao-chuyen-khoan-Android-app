package com.app.docthongbaochuyenkhoan.service

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.util.Log
import com.app.docthongbaochuyenkhoan.BuildConfig
import com.app.docthongbaochuyenkhoan.controller.NotificationMessageParser
import com.app.docthongbaochuyenkhoan.flow.TransactionFlowManager
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch

// Chỉ dùng để test qua ADB. Xoá hoặc disable trước khi release production.
class DebugReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (!BuildConfig.DEBUG) return

        val pkg   = intent.getStringExtra("pkg")   ?: ""
        val title = intent.getStringExtra("title") ?: ""
        val text  = intent.getStringExtra("text")  ?: ""

        Log.d(TAG, "Received: pkg=$pkg | title=$title | text=$text")

        val transaction = NotificationMessageParser
            .extractTransactionFromRawNotificationMessage(pkg, title, text)

        if (transaction == null) {
            Log.d(TAG, "Parser returned null — kiểm tra lại format title/text")
            return
        }

        Log.d(TAG, "Transaction parsed: bank=${transaction.bank} amount=${transaction.amount}")

        CoroutineScope(Dispatchers.IO).launch {
            TransactionFlowManager.emitTransaction(transaction)
            AppDatabase.getDatabase(context).transactionDao().insert(transaction)
        }
    }

    companion object {
        const val TAG = "DebugReceiver"
        const val ACTION = "com.app.docthongbaochuyenkhoan.DEBUG_NOTIFICATION"
    }
}
