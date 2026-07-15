package com.app.docthongbaochuyenkhoan.ui.activity

import android.app.Activity
import android.view.View
import android.widget.ImageButton
import androidx.fragment.app.FragmentActivity
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.ui.dialog.SettingDialogFragment
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation

class TaskbarManager(private val activity: Activity) {

    private var taskbarView: View = activity.findViewById(R.id.task_bar)

    init {
        val btnSetting = taskbarView.findViewById<ImageButton>(R.id.btnSetting)
        btnSetting.setOnClickListener { openSettingDialog() }
        btnSetting.addClickAnimation()
    }

    fun openSettingDialogFromOutside() = openSettingDialog()

    private fun openSettingDialog() {
        val supportFragmentManager =
            (activity as? FragmentActivity)?.supportFragmentManager ?: return

        val fragment = supportFragmentManager.findFragmentByTag("SettingDialogFragment")
        if (fragment != null && fragment is SettingDialogFragment) {
            if (fragment.isVisible) {
                fragment.dismiss()
                fragment.show(supportFragmentManager, "SettingDialogFragment")
            }
        } else {
            SettingDialogFragment
                .newInstance(activity as SettingDialogFragment.SettingDialogListener)
                .show(supportFragmentManager, "SettingDialogFragment")
        }
    }

    fun release() {
        taskbarView.findViewById<ImageButton>(R.id.btnSetting).setOnClickListener(null)
    }
}