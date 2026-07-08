package com.app.docthongbaochuyenkhoan.viewModel

import android.content.Context
import android.net.Uri
import android.util.Log
import androidx.lifecycle.ViewModel
import androidx.lifecycle.viewModelScope
import com.app.docthongbaochuyenkhoan.model.TransactionExport
import com.app.docthongbaochuyenkhoan.model.UiEvent
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.model.database.TransactionDao
import com.app.docthongbaochuyenkhoan.model.uniqueKey
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.MutableSharedFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExportDialogViewModel : ViewModel() {
    // replay = 1: đảm bảo event không bị mất khi lifecycle restart (file picker trả về)
    private val _uiEvent = MutableSharedFlow<UiEvent>(replay = 1)
    val uiEvent = _uiEvent.asSharedFlow()

    fun exportToZip(context: Context, uri: Uri) {
        val repository: TransactionDao = AppDatabase.getDatabase(context).transactionDao()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiEvent.emit(UiEvent.Exporting)

                val transactions = repository.getAllTransactions()
                val json = Json { prettyPrint = false }
                    .encodeToString(TransactionExport(version = 1, transactions = transactions))

                val outputStream = context.contentResolver.openOutputStream(uri)
                    ?: throw IllegalStateException("Không thể mở file để ghi")

                outputStream.use { output ->
                    ZipOutputStream(output).use { zip ->
                        zip.putNextEntry(ZipEntry("transactions.json"))
                        zip.write(json.toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                }

                _uiEvent.emit(UiEvent.ExportSuccess)

            } catch (e: Exception) {
                Log.e("ExportDialog", "Export failed", e)
                _uiEvent.emit(UiEvent.Error("Xuất file thất bại: ${e.message}"))
            }
        }
    }

    fun importFromZip(context: Context, uri: Uri) {
        val repository: TransactionDao = AppDatabase.getDatabase(context).transactionDao()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiEvent.emit(UiEvent.Exporting)

                val inputStream = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Không thể mở file để đọc")

                val json = inputStream.use { input ->
                    ZipInputStream(input).use { zip ->
                        var entry = zip.nextEntry
                        while (entry != null) {
                            if (entry.name == "transactions.json") {
                                return@use zip.bufferedReader().readText()
                            }
                            entry = zip.nextEntry
                        }
                        throw IllegalStateException("Không tìm thấy file transactions.json trong archive")
                    }
                }

                val exportData = Json.decodeFromString<TransactionExport>(json)

                val existingKeys = repository.getAllTransactions()
                    .map { it.uniqueKey() }
                    .toSet()

                val newTransactions = exportData.transactions
                    .filter { it.uniqueKey() !in existingKeys }
                    .map { it.copy(id = 0) }

                repository.insertAll(newTransactions)

                _uiEvent.emit(UiEvent.ImportSuccess(newTransactions.size))

            } catch (e: Exception) {
                Log.e("ExportDialog", "Import failed", e)
                _uiEvent.emit(UiEvent.Error("Nhập file thất bại: ${e.message}"))
            }
        }
    }
}
