package com.app.docthongbaochuyenkhoan.utils

import androidx.room.TypeConverter
import com.app.docthongbaochuyenkhoan.model.Bank

class BankConverter {
    @TypeConverter
    fun fromBank(bank: Bank?): String? {
        return bank?.displayName // Save the Enum name (name) to the DB
    }

    @TypeConverter
    fun toBank(name: String?): Bank? {
        return Bank.fromName(name) // Convert name to enum
    }
}