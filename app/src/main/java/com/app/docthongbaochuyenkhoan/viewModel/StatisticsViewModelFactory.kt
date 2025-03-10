package com.app.docthongbaochuyenkhoan.viewModel

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import com.app.docthongbaochuyenkhoan.model.database.TransactionDao

class StatisticsViewModelFactory(private val transactionDao: TransactionDao) :
    ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(StatisticsViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return StatisticsViewModel(transactionDao) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}