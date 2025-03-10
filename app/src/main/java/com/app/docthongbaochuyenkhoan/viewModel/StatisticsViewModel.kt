package com.app.docthongbaochuyenkhoan.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.docthongbaochuyenkhoan.model.DailyAmount
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

    fun loadDailyAmounts(endCalendar: Calendar, numberOfDays: Int) {
        viewModelScope.launch {
            val endDate = endCalendar.timeInMillis
            endCalendar.add(Calendar.DAY_OF_YEAR, -numberOfDays)
            val startDate = endCalendar.timeInMillis

            val data = transactionDao.getAmountsForLast7Days(startDate, endDate)

            if (data.isNotEmpty()) {
                // Chuyển dữ liệu từ database thành Map để dễ xử lý
                val dataMap = data.associateBy { it.day }

                // Tạo danh sách đủ 7 ngày
                val resultList = mutableListOf<DailyAmount>()
                val calendar = Calendar.getInstance().apply { timeInMillis = startDate }

                for (i in 0 until numberOfDays + 1) {
                    val dateKey = DateUtils.formatDate(calendar.timeInMillis)
                    val dailyData = dataMap[dateKey] ?: DailyAmount(
                        dateKey, 0, 0
                    ) // Nếu không có thì mặc định 0
                    resultList.add(dailyData)
                    calendar.add(Calendar.DAY_OF_YEAR, 1) // Chuyển sang ngày tiếp theo
                }

                _dailyAmounts.value = resultList
            }
        }
    }
}