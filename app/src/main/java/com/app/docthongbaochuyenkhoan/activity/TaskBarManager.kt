package com.app.docthongbaochuyenkhoan.activity

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import android.widget.Toast
import androidx.fragment.app.FragmentActivity
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.dialog.SettingDialogFragment
import com.app.docthongbaochuyenkhoan.service.MyNotificationListenerService
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.google.android.material.switchmaterial.SwitchMaterial

class TaskbarManager(private val activity: Activity) {
    private var taskbarView: View = activity.findViewById(R.id.task_bar)

    init {
        val switchNotification = taskbarView.findViewById<SwitchMaterial>(R.id.switchNotification)
        val btnSetting = taskbarView.findViewById<ImageButton>(R.id.btnSetting)

        MyNotificationListenerService.isNotificationListenerEnabled =
            SharedPreferencesManager.isNotificationListenerEnabled()
        switchNotification.let {
            it.isChecked = MyNotificationListenerService.isNotificationListenerEnabled
            it.setOnCheckedChangeListener { _, isChecked ->
                MyNotificationListenerService.isNotificationListenerEnabled = isChecked
                SharedPreferencesManager.saveNotificationListenerEnabled(isChecked)

                Toast.makeText(
                    activity,
                    if (isChecked) "Đã bật tính năng đọc thông báo chuyển khoản" else "Đã tắt tính năng đọc thông báo chuyển khoản",
                    Toast.LENGTH_SHORT
                ).show()
            }
        }

        btnSetting.setOnClickListener {
            openSettingDialog()
        }
        btnSetting.addClickAnimation()
    }

    private fun openSettingDialog() {
        val supportFragmentManager =
            (activity as? FragmentActivity)?.supportFragmentManager

        if (supportFragmentManager != null) {
            val fragment =
                supportFragmentManager.findFragmentByTag("SettingDialogFragment")

            if (fragment != null && fragment is SettingDialogFragment) {
                // If fragment has been added
                if (fragment.isVisible) {
                    fragment.dismiss() // Make sure the fragment is deleted before displaying it again
                    fragment.show(supportFragmentManager, "SettingDialogFragment")
                }
            } else {
                // If the fragment does not exist, create a new one and display it
                val dialogSetting =
                    SettingDialogFragment.newInstance(activity as SettingDialogFragment.SettingDialogListener)
                dialogSetting.show(supportFragmentManager, "SettingDialogFragment")
            }
        }
    }

    fun release() {
        taskbarView.findViewById<SwitchMaterial>(R.id.switchNotification)
            .setOnCheckedChangeListener(null)
        taskbarView.findViewById<ImageButton>(R.id.btnSetting).setOnClickListener(null)
    }
}