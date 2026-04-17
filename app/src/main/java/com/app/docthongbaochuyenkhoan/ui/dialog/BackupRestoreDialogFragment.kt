package com.app.docthongbaochuyenkhoan.ui.dialog

import android.annotation.SuppressLint
import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.view.Gravity
import android.view.View
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.lifecycleScope
import androidx.lifecycle.repeatOnLifecycle
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.databinding.DialogBackupRestoreBinding
import com.app.docthongbaochuyenkhoan.model.UiEvent
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.DateUtils
import com.app.docthongbaochuyenkhoan.viewModel.ExportDialogViewModel
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

class BackupRestoreDialogFragment() : DialogFragment() {
    lateinit var bindingBackupRestoreDialog: DialogBackupRestoreBinding

    companion object {
        fun newInstance(
        ): BackupRestoreDialogFragment {
            val fragment = BackupRestoreDialogFragment()
            return fragment
        }
    }

    lateinit var viewModel: ExportDialogViewModel

    override fun onAttach(context: Context) {
        super.onAttach(context)
        viewModel = ViewModelProvider(
            requireActivity()
        )[ExportDialogViewModel::class.java]
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        bindingBackupRestoreDialog = DialogBackupRestoreBinding.inflate(layoutInflater)
        builder.setView(bindingBackupRestoreDialog.root)

        val dialog = builder.create()
        dialog.let { dialog ->
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

            bindingBackupRestoreDialog.exportLayout.setOnClickListener {
                onExportClick()
            }

            bindingBackupRestoreDialog.importLayout.setOnClickListener {
                onImportClick()
            }

            bindingBackupRestoreDialog.exportLayout.addClickAnimation()
            bindingBackupRestoreDialog.importLayout.addClickAnimation()
            bindingBackupRestoreDialog.iBtnClose.addClickAnimation()
            bindingBackupRestoreDialog.iBtnClose.setOnClickListener { dialog.dismiss() }
            bindingBackupRestoreDialog.btnClose.addClickAnimation()
            bindingBackupRestoreDialog.btnClose.setOnClickListener { dialog.dismiss() }
        }
        return dialog
    }

    @SuppressLint("SetTextI18n")
    override fun onViewCreated(view: View, savedInstanceState: Bundle?) {
        super.onViewCreated(view, savedInstanceState)

        lifecycleScope.launch {
            repeatOnLifecycle(Lifecycle.State.STARTED) {
                viewModel.uiEvent.collect { event ->
                    bindingBackupRestoreDialog.tvResultStatus.visibility = View.VISIBLE
                    when (event) {
                        UiEvent.Exporting -> {
                            delay(200)
                            showToast("Đang thực hiện…")
                            bindingBackupRestoreDialog.tvResultStatus.text = "Đang thực hiện…"
                        }

                        UiEvent.ExportSuccess -> {
                            delay(200)
                            showToast("Xuất file thành công")
                            bindingBackupRestoreDialog.tvResultStatus.text =
                                "Xuất file thành công"
                        }

                        is UiEvent.ImportSuccess -> {
                            delay(200)
                            showToast("Import thành công ${event.count} giao dịch")
                            bindingBackupRestoreDialog.tvResultStatus.text =
                                "Import thành công ${event.count} giao dịch"
                        }

                        is UiEvent.Error -> {
                            delay(200)
                            showToast(
                                ("Xuất file thất bại" + event.message) ?: "Xuất file thất bại"
                            )
                            bindingBackupRestoreDialog.tvResultStatus.text =
                                ("Xuất file thất bại" + event.message)
                        }
                    }
                }
            }
        }
    }

    private fun onExportClick() {
        val fileName =
            "transactions_${DateUtils.formatRawDate(System.currentTimeMillis())}.json"

        createFileLauncher.launch(fileName)
    }

    private fun onImportClick() {
        openFileLauncher.launch(arrayOf("application/json"))
    }

    private val createFileLauncher =
        registerForActivityResult(
            ActivityResultContracts.CreateDocument("application/json")
        ) { uri: Uri? ->
            uri?.let {
                viewModel.exportToZip(requireContext(), it)
            }
        }

    private val openFileLauncher =
        registerForActivityResult(
            ActivityResultContracts.OpenDocument()
        ) { uri: Uri? ->
            uri?.let {
                requireContext().contentResolver.takePersistableUriPermission(
                    it,
                    Intent.FLAG_GRANT_READ_URI_PERMISSION
                )

                viewModel.importFromZip(requireContext(), it)
            }
        }

    private fun showToast(message: String) {
        if (!isAdded) return   // an toàn cho DialogFragment
        Toast.makeText(requireContext(), message, Toast.LENGTH_SHORT).show()
    }
}