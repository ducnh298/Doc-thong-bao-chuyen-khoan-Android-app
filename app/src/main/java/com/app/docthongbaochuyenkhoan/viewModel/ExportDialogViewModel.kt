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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asSharedFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.launch
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.util.zip.ZipEntry
import java.util.zip.ZipInputStream
import java.util.zip.ZipOutputStream

class ExportDialogViewModel : ViewModel() {
    // Persistent status text — survives dialog close/reopen
    private val _statusText = MutableStateFlow<String?>(null)
    val statusText = _statusText.asStateFlow()

    // One-shot side effects (toast, loadTransactions) — no replay
    private val _sideEffect = MutableSharedFlow<UiEvent>()
    val sideEffect = _sideEffect.asSharedFlow()

    fun exportToZip(context: Context, uri: Uri) {
        val repository: TransactionDao = AppDatabase.getDatabase(context).transactionDao()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _statusText.value = "Đang thực hiện…"

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

                _statusText.value = "Xuất file thành công"
                _sideEffect.emit(UiEvent.ExportSuccess)

            } catch (e: Exception) {
                Log.e("ExportDialog", "Export failed", e)
                val msg = "Xuất file thất bại: ${e.message}"
                _statusText.value = msg
                _sideEffect.emit(UiEvent.Error(msg))
            }
        }
    }

    fun importFromZip(context: Context, uri: Uri) {
        val repository: TransactionDao = AppDatabase.getDatabase(context).transactionDao()

        viewModelScope.launch(Dispatchers.IO) {
            try {
                _statusText.value = "Đang thực hiện…"

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

                val importJson = Json { ignoreUnknownKeys = true; coerceInputValues = true }
                val exportData = importJson.decodeFromString<TransactionExport>(json)

                val existingKeys = repository.getAllTransactions()
                    .map { it.uniqueKey() }
                    .toSet()

                val newTransactions = exportData.transactions
                    .filter { it.uniqueKey() !in existingKeys }
                    .map { it.copy(id = 0) }

                repository.insertAll(newTransactions)

                _statusText.value = "Nhập thành công ${newTransactions.size} giao dịch"
                _sideEffect.emit(UiEvent.ImportSuccess(newTransactions.size))

            } catch (e: Exception) {
                Log.e("ExportDialog", "Import failed", e)
                val msg = "Nhập file thất bại: ${e.message}"
                _statusText.value = msg
                _sideEffect.emit(UiEvent.Error(msg))
            }
        }
    }
}
