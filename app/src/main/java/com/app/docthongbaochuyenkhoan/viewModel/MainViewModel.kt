package com.app.docthongbaochuyenkhoan.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.app.docthongbaochuyenkhoan.flow.TransactionFlowManager
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.model.database.TransactionDao
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import java.util.concurrent.TimeUnit
import java.util.concurrent.atomic.AtomicInteger

class MainViewModel(private val dao: TransactionDao) : ViewModel() {

    private val _selectedDay = MutableStateFlow(DateUtils.getStartTimeOfToday())
    val selectedDay: StateFlow<Long> = _selectedDay.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions: StateFlow<List<Transaction>> = _transactions.asStateFlow()

    private val _isLoading = MutableStateFlow(false)
    val isLoading: StateFlow<Boolean> = _isLoading.asStateFlow()
    private val activeLoadCount = AtomicInteger(0)

    val today: Long get() = DateUtils.getStartTimeOfToday()
    val canGoNext: Boolean get() = _selectedDay.value < today

    private var lastKnownToday: Long = DateUtils.getStartTimeOfToday()

    init {
        viewModelScope.launch {
            TransactionFlowManager.transactionFlow.collect { transaction ->
                if (_selectedDay.value == today) {
                    _transactions.value = listOf(transaction) + _transactions.value
                }
            }
        }
        loadTransactions()
    }

    fun loadTransactions() {
        viewModelScope.launch {
            activeLoadCount.incrementAndGet()
            _isLoading.value = true
            try {
                _transactions.value = dao.getTransactionsForToday(
                    _selectedDay.value,
                    _selectedDay.value + TimeUnit.DAYS.toMillis(1)
                )
            } finally {
                _isLoading.value = activeLoadCount.decrementAndGet() > 0
            }
        }
    }

    fun selectDay(timestamp: Long) {
        _selectedDay.value = timestamp
        loadTransactions()
    }

    fun nextDay() {
        if (canGoNext) {
            _selectedDay.value += TimeUnit.DAYS.toMillis(1)
            loadTransactions()
        }
    }

    fun prevDay() {
        _selectedDay.value -= TimeUnit.DAYS.toMillis(1)
        loadTransactions()
    }

    fun resetToToday() {
        lastKnownToday = today
        _selectedDay.value = lastKnownToday
        loadTransactions()
    }

    // Gọi trong onResume để phát hiện qua nửa đêm khi app đang mở
    fun onAppForeground() {
        val currentToday = today
        if (currentToday != lastKnownToday) {
            resetToToday()
        }
    }
}

class MainViewModelFactory(private val dao: TransactionDao) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(MainViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return MainViewModel(dao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class: ${modelClass.name}")
    }
}
