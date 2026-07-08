package com.app.docthongbaochuyenkhoan.flow

import com.app.docthongbaochuyenkhoan.model.Transaction
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow

object TransactionFlowManager {
    // extraBufferCapacity = 1: cho phép emit không suspend khi không có collector
    private val _transactionFlow = MutableSharedFlow<Transaction>(
        replay = 0,
        extraBufferCapacity = 1
    )
    val transactionFlow = _transactionFlow.asSharedFlow()

    fun emitTransaction(transaction: Transaction) {
        _transactionFlow.tryEmit(transaction)
    }
}