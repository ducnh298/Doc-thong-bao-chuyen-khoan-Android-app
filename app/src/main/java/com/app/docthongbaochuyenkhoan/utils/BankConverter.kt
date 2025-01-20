package com.app.docthongbaochuyenkhoan.utils

import androidx.room.TypeConverter
import com.app.docthongbaochuyenkhoan.model.Bank

class BankConverter {
    @TypeConverter
    fun fromBank(bank: Bank?): String? {
        return bank?.displayName // Lưu tên Enum (name) vào DB
    }

    @TypeConverter
    fun toBank(name: String?): Bank? {
        return Bank.fromName(name) // Chuyển tên thành enum
    }
}