package com.app.docthongbaochuyenkhoan.viewModel

import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.docthongbaochuyenkhoan.model.DailyAmount
import com.app.docthongbaochuyenkhoan.model.MonthlyAmount
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.model.database.TransactionDao
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.Calendar

class StatisticsViewModel(private val transactionDao: TransactionDao) : ViewModel() {

    private val _dailyAmounts = MutableStateFlow<List<DailyAmount>>(emptyList())
    val dailyAmounts: StateFlow<List<DailyAmount>> = _dailyAmounts.asStateFlow()

    private val _monthlyAmounts = MutableStateFlow<List<MonthlyAmount>>(emptyList())
    val monthlyAmounts: StateFlow<List<MonthlyAmount>> = _monthlyAmounts.asStateFlow()

    fun loadTransactionAmountsByDays(endCalendar: Calendar, numberOfDays: Int) {
        viewModelScope.launch {

            val endDate = endCalendar.timeInMillis
            endCalendar.add(Calendar.DAY_OF_YEAR, -numberOfDays + 1)
            val startDate = endCalendar.timeInMillis

            val data = transactionDao.getTotalReceivedAndSentByDays(startDate, endDate)

            if (data.isNotEmpty()) {
                // Chuyển dữ liệu từ database thành Map để dễ xử lý
                val dataMap = data.associateBy { it.day }

                val resultList = mutableListOf<DailyAmount>()
                val calendar = Calendar.getInstance().apply { timeInMillis = startDate }

                for (i in 0 until numberOfDays) {
                    val dateKey = DateUtils.formatDate(calendar.timeInMillis)
                    val dailyData = dataMap[dateKey] ?: DailyAmount(
                        dateKey, 0, 0
                    ) // Nếu không có thì mặc định 0
                    resultList.add(dailyData)
                    calendar.add(Calendar.DAY_OF_YEAR, 1) // Chuyển sang ngày tiếp theo
                }

                _dailyAmounts.emit(resultList)
            }
        }
    }

    fun loadTransactionAmountsByMonths(endCalendar: Calendar, numberOfMonths: Int) {
        viewModelScope.launch {
            val endDate = endCalendar.timeInMillis
            endCalendar.add(Calendar.MONTH, -numberOfMonths + 1)
            val startDate = endCalendar.timeInMillis

            val data = transactionDao.getTotalReceivedAndSentByMonth(startDate, endDate)

            if (data.isNotEmpty()) {
                val dataMap = data.associateBy { it.month }

                val resultList = mutableListOf<MonthlyAmount>()
                val calendar = Calendar.getInstance().apply { timeInMillis = startDate }

                for (i in 0 until numberOfMonths) {
                    val monthKey = DateUtils.formatMonth(calendar.timeInMillis) // ví dụ "06/2025"
                    val monthlyData = dataMap[monthKey] ?: MonthlyAmount(
                        monthKey, 0, 0
                    )
                    resultList.add(monthlyData)
                    calendar.add(Calendar.MONTH, 1)
                }

                _monthlyAmounts.emit(resultList)
            }
        }
    }
}