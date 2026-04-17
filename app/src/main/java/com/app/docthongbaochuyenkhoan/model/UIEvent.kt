package com.app.docthongbaochuyenkhoan.model

sealed class UiEvent {
    object Exporting : UiEvent()
    object ExportSuccess : UiEvent()
    data class ImportSuccess(val count: Int) : UiEvent()
    data class Error(val message: String?) : UiEvent()
}