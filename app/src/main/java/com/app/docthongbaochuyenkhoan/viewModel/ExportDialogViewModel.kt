package com.app.docthongbaochuyenkhoan.viewModel

import android.content.Context
import android.net.Uri
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

class ExportDialogViewModel() : ViewModel() {
    private val _uiEvent = MutableSharedFlow<UiEvent>()
    val uiEvent = _uiEvent.asSharedFlow()


    fun exportToZip(context: Context, uri: Uri) {
        val repository: TransactionDao = AppDatabase.getDatabase(context).transactionDao()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiEvent.emit(UiEvent.Exporting)

                val transactions = repository.getAllTransactions()

                val exportData = TransactionExport(
                    version = 1,
                    transactions = transactions
                )

                val json = Json {
                    prettyPrint = false // 👈 để giảm size
                }.encodeToString(exportData)

                context.contentResolver.openOutputStream(uri)?.use { output ->
                    ZipOutputStream(output).use { zip ->
                        zip.putNextEntry(ZipEntry("transactions.json"))
                        zip.write(json.toByteArray(Charsets.UTF_8))
                        zip.closeEntry()
                    }
                }

                _uiEvent.emit(UiEvent.ExportSuccess)

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.Error("Xuất file thất bại"))
            }
        }
    }

    fun importFromZip(context: Context, uri: Uri) {
        val repository: TransactionDao = AppDatabase.getDatabase(context).transactionDao()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _uiEvent.emit(UiEvent.Exporting)

                val input = context.contentResolver.openInputStream(uri)
                    ?: throw IllegalStateException("Không mở được file")

                val json = ZipInputStream(input).use { zip ->
                    var entry = zip.nextEntry

                    while (entry != null) {
                        if (entry.name == "transactions.json") {
                            return@use zip.bufferedReader().readText()
                        }
                        entry = zip.nextEntry
                    }
                    throw IllegalStateException("Không tìm thấy file JSON")
                }

                val exportData =
                    Json.decodeFromString<TransactionExport>(json)

                // 🔥 MERGE + chống trùng
                val existing = repository.getAllTransactions()
                    .map { it.uniqueKey() }
                    .toSet()

                val newTransactions = exportData.transactions
                    .filter { it.uniqueKey() !in existing }
                    .map { it.copy(id = 0) }

                repository.insertAll(newTransactions)

                _uiEvent.emit(
                    UiEvent.ImportSuccess(newTransactions.size)
                )

            } catch (e: Exception) {
                _uiEvent.emit(UiEvent.Error("Nhập file thất bại (${e.message})"))
            }
        }
    }
}