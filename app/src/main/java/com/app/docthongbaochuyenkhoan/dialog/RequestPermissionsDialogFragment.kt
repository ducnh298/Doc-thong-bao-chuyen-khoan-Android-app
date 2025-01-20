package com.app.docthongbaochuyenkhoan.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.net.Uri
import android.os.Bundle
import android.os.PowerManager
import android.provider.Settings
import android.view.Gravity
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.fragment.app.DialogFragment
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.databinding.DialogRequestPermissionsBinding
import com.app.docthongbaochuyenkhoan.utils.AppUtils
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation

class RequestPermissionsDialogFragment : DialogFragment() {
    private lateinit var binding: DialogRequestPermissionsBinding
    private var autoClose = false

    companion object {
        fun newInstance(
            autoClose: Boolean
        ): RequestPermissionsDialogFragment {
            val fragment = RequestPermissionsDialogFragment()
            fragment.autoClose = autoClose
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {

        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        binding = DialogRequestPermissionsBinding.inflate(layoutInflater)
        builder.setView(binding.root)

        val dialogRequestPermissions = builder.create()

        dialogRequestPermissions.let { dialog ->
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)

            binding.let {
                it.llNotificationAccessSetting.setOnClickListener { requestNotificationAccess() }
                it.llIgnoreBatteryOptimizationSetting.setOnClickListener { requestIgnoreBatteryOptimizationSetting() }
                it.btnClose.setOnClickListener { dialogRequestPermissions.dismiss() }

                it.llNotificationAccessSetting.addClickAnimation()
                it.llIgnoreBatteryOptimizationSetting.addClickAnimation()
                it.btnClose.addClickAnimation()
            }

            dialog.show()
            dialog.window?.attributes =
                AppUtils.getDialogDimension(requireActivity(), dialog)

            return dialog
        }
    }

    override fun onResume() {
        super.onResume()
        checkEnabledSettings()
    }

    private fun requestNotificationAccess() {
        Toast.makeText(
            requireContext(),
            "Tìm và cho phép ứng dụng \"Đọc thông báo chuyển khoản\"",
            Toast.LENGTH_LONG
        ).show()

        startActivity(Intent("android.settings.ACTION_NOTIFICATION_LISTENER_SETTINGS"))
    }

    private fun requestIgnoreBatteryOptimizationSetting() {
        Toast.makeText(
            requireContext(),
            "Tiết kiệm pin -> Không giới hạn/không hạn chế",
            Toast.LENGTH_LONG
        )
            .show()
        val intent = Intent()

        intent.setAction(Settings.ACTION_APPLICATION_DETAILS_SETTINGS)
        intent.setData(Uri.fromParts("package", requireContext().packageName, null))
        startActivity(intent)
    }

    private fun checkNotificationAccessEnabled(): Boolean {
        val enabledListeners = Settings.Secure.getString(
            requireContext().contentResolver,
            "enabled_notification_listeners"
        )
        return enabledListeners != null && enabledListeners.contains(requireContext().packageName)
    }

    private fun checkIgnoreBatteryOptimizationEnabled(): Boolean {
        return (requireContext().getSystemService(Context.POWER_SERVICE) as PowerManager).isIgnoringBatteryOptimizations(
            requireContext().packageName
        )
    }

    private fun checkEnabledSettings() {
        val notificationAccessEnabled = checkNotificationAccessEnabled()
        val ignoreBatteryOptimizationEnabled = checkIgnoreBatteryOptimizationEnabled()

        binding.tvNotificationAccessSetting.setTextColor(
            if (notificationAccessEnabled) requireContext().getColor(R.color.green) else requireContext().getColor(
                R.color.black
            )
        )

        binding.tvIgnoreBatteryOptimizationSetting.setTextColor(
            if (ignoreBatteryOptimizationEnabled) requireContext().getColor(R.color.green) else requireContext().getColor(
                R.color.black
            )
        )

//        if (autoClose && notificationAccessEnabled && ignoreBatteryOptimizationEnabled)
//            dialogRequestPermissions.dismiss()
    }
}