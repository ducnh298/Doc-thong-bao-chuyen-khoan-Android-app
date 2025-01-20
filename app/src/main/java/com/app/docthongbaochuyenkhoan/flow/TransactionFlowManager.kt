package com.app.docthongbaochuyenkhoan.flow

import com.app.docthongbaochuyenkhoan.model.Transaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TransactionFlowManager {
    private val _transactionFlow = MutableSharedFlow<Transaction>(replay = 0)
    val transactionFlow = _transactionFlow.asSharedFlow()

    suspend fun emitTransaction(transaction: Transaction) {
        _transactionFlow.emit(transaction)
    }
}