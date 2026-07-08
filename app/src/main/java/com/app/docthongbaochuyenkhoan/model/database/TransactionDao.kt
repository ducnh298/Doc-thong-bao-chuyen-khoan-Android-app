package com.app.docthongbaochuyenkhoan.model.database

import androidx.room.Dao
import androidx.room.Delete
import androidx.room.Insert
import androidx.room.Query
import androidx.room.Update
import com.app.docthongbaochuyenkhoan.model.DailyAmount
import com.app.docthongbaochuyenkhoan.model.MonthlyAmount
import com.app.docthongbaochuyenkhoan.model.Transaction

@Dao
interface TransactionDao {
    @Insert
    fun insert(transaction: Transaction)

    @Insert
    fun insertAll(transactions: List<Transaction>)

    @Update
    fun update(transaction: Transaction)

    @Delete
    fun delete(transaction: Transaction)

    @Query("SELECT * FROM transactions ORDER BY timestamp DESC")
    fun getAllTransactions(): List<Transaction>

    @Query("SELECT * FROM transactions WHERE timestamp >= :startOfDay AND timestamp < :endOfDay ORDER BY timestamp DESC")
    suspend fun getTransactionsForToday(startOfDay: Long, endOfDay: Long): List<Transaction>

    @Query("""
        SELECT 
            strftime('%d/%m/%Y', timestamp / 1000, 'unixepoch', 'localtime') as day,
            SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) as received,
            SUM(CASE WHEN amount < 0 THEN amount ELSE 0 END) as sent
        FROM transactions
        WHERE timestamp >= :startDate AND timestamp <= :endDate
        GROUP BY day
        ORDER BY day ASC
    """)
    suspend fun getTotalReceivedAndSentByDays(startDate: Long, endDate: Long): List<DailyAmount>

    @Query("""
    SELECT 
        strftime('%m/%Y', timestamp / 1000, 'unixepoch', 'localtime') AS month,
        SUM(CASE WHEN amount > 0 THEN amount ELSE 0 END) AS received,
        SUM(CASE WHEN amount < 0 THEN amount ELSE 0 END) as sent
    FROM transactions
    WHERE timestamp >= :startDate AND timestamp <= :endDate
    GROUP BY month
    ORDER BY month ASC
""")
    suspend fun getTotalReceivedAndSentByMonth(startDate: Long, endDate: Long): List<MonthlyAmount>
}