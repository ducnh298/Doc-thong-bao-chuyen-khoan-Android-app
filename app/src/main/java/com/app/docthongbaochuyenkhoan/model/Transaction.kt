package com.app.docthongbaochuyenkhoan.model

import androidx.room.Entity
import androidx.room.PrimaryKey
import java.io.Serializable
import kotlinx.serialization.Serializable as KtxSerializable

@KtxSerializable
@Entity(tableName = "transactions")
data class Transaction(
    @PrimaryKey(autoGenerate = true) val id: Int = 0,
    val bank: Bank = Bank.UNKNOWN,
    val title: String,
    val content: String,
    val amount: Long,
    val timestamp: Long = System.currentTimeMillis(),
) : Serializable

fun Transaction.uniqueKey(): String {
    return "${bank}_${amount}_${timestamp}_${content.hashCode()}"
}

@KtxSerializable
data class TransactionExport(
    val version: Int,
    val transactions: List<Transaction>
)
