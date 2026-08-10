package com.app.docthongbaochuyenkhoan.ui.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Build
import android.os.Bundle
import android.provider.DocumentsContract
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.databinding.DialogBackupRestoreBinding
import com.app.docthongbaochuyenkhoan.model.UiEvent
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.applyCustomStyle
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import com.app.docthongbaochuyenkhoan.viewModel.ExportDialogViewModel
import com.app.docthongbaochuyenkhoan.viewModel.MainViewModel
import com.app.docthongbaochuyenkhoan.viewModel.MainViewModelFactory
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import kotlinx.coroutines.launch

class BackupRestoreDialogFragment : DialogFragment() {

    private lateinit var binding: DialogBackupRestoreBinding
    private lateinit var viewModel: ExportDialogViewModel
    private lateinit var mainViewModel: MainViewModel

    companion object {
        fun newInstance() = BackupRestoreDialogFragment()
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(requireActivity())[ExportDialogViewModel::class.java]
        val dao = AppDatabase.getDatabase(context).transactionDao()
        mainViewModel = ViewModelProvider(
            requireActivity(),
            MainViewModelFactory(dao)
        )[MainViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogBackupRestoreBinding.inflate(layoutInflater)

        val dialog = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setView(binding.root)
            .create()

        dialog.applyCustomStyle()

        binding.exportLayout.setOnClickListener { onExportClick() }
        binding.importLayout.setOnClickListener { onImportClick() }
        binding.exportLayout.addClickAnimation()
        binding.importLayout.addClickAnimation()
        binding.iBtnClose.addClickAnimation()
        binding.iBtnClose.setOnClickListener { dialog.dismiss() }
        binding.btnClose.addClickAnimation()
        binding.btnClose.setOnClickListener { dialog.dismiss() }

        return dialog
    }

    // onViewCreated không được gọi khi dùng onCreateDialog.
    // Dùng onStart để đảm bảo observe luôn được thiết lập,
    // kể cả sau khi file picker trả về (lifecycle restart).
    override fun onStart() {
        super.onStart()
        observeViewModel()
    }

    private fun observeViewModel() {
        // Status text persists across dialog close/reopen (StateFlow)
        lifecycleScope.launch {
            viewModel.statusText.collect { text ->
                if (!isAdded || text == null) return@collect
                binding.tvResultStatus.visibility = View.VISIBLE
                binding.tvResultStatus.text = text
            }
        }
        // One-shot side effects: toast + reload (SharedFlow, no replay)
        lifecycleScope.launch {
            viewModel.sideEffect.collect { event ->
                if (!isAdded) return@collect
                when (event) {
                    UiEvent.ExportSuccess -> showToast("Xuất file thành công")
                    is UiEvent.ImportSuccess -> {
                        showToast("Nhập thành công ${event.count} giao dịch")
                        mainViewModel.loadTransactions()
                    }
                    is UiEvent.Error -> showToast(event.message ?: "Thao tác thất bại")
                    else -> {}
                }
            }
        }
    }

    private fun onExportClick() {
        val fileName = "sao_luu_${DateUtils.formatFileDate(System.currentTimeMillis())}.json"
        createFileLauncher.launch(fileName)
    }

    private fun onImportClick() {
        openFileLauncher.launch(arrayOf("application/json", "*/*"))
    }

    private val createFileLauncher =
        registerForActivityResult(CreateDocumentInDocuments()) { uri ->
            uri?.let { viewModel.exportToZip(requireContext(), it) }
        }

    private val openFileLauncher =
        registerForActivityResult(OpenDocumentInDocuments()) { uri ->
            uri?.let {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )
                viewModel.importFromZip(requireContext(), it)
            }
        }

    private fun showToast(message: String) {
        if (!isAdded) return
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}

private val documentsInitialUri: Uri
    get() = DocumentsContract.buildDocumentUri(
        "com.android.externalstorage.documents",
        "primary:Documents"
    )

private fun Intent.applyDocumentsInitialUri(): Intent = also { intent ->
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
        intent.putExtra(DocumentsContract.EXTRA_INITIAL_URI, documentsInitialUri)
    }
}

private class CreateDocumentInDocuments : ActivityResultContracts.CreateDocument("application/json") {
    override fun createIntent(context: Context, input: String): Intent =
        super.createIntent(context, input).applyDocumentsInitialUri()
}

private class OpenDocumentInDocuments : ActivityResultContracts.OpenDocument() {
    override fun createIntent(context: Context, input: Array<String>): Intent =
        super.createIntent(context, input).applyDocumentsInitialUri()
}
