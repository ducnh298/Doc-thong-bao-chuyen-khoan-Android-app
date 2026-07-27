package com.app.docthongbaochuyenkhoan.ui.dialog

import android.app.Dialog
import android.content.ActivityNotFoundException
import android.content.ComponentName
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.os.Handler
import android.os.Looper
import android.service.notification.NotificationListenerService
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.core.net.toUri
import androidx.fragment.app.DialogFragment
import androidx.lifecycle.lifecycleScope
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.databinding.DialogChangeNotificationContentBinding
import com.app.docthongbaochuyenkhoan.databinding.DialogContactInfoBinding
import com.app.docthongbaochuyenkhoan.databinding.DialogSettingBinding
import com.app.docthongbaochuyenkhoan.databinding.DialogTtsHelperBinding
import com.app.docthongbaochuyenkhoan.model.database.AppDatabase
import com.app.docthongbaochuyenkhoan.service.MyNotificationListenerService
import com.app.docthongbaochuyenkhoan.ui.activity.MainActivity
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.getAppVersionInfo
import com.app.docthongbaochuyenkhoan.utils.MediaPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext


class SettingDialogFragment : DialogFragment() {

    interface SettingDialogListener {
        fun onTvNotificationSoundClicked(): View.OnClickListener
        fun onAppEnabledChanged(isEnabled: Boolean)
    }

    private lateinit var binding: DialogSettingBinding
    private lateinit var listener: SettingDialogListener
    private var notificationSoundUri: Uri? = null
    private val volumePreviewHandler = Handler(Looper.getMainLooper())
    private val volumePreviewRunnable = Runnable {
        MediaPlayerUtils.playMedia(requireContext(), notificationSoundUri)
    }
    private var dialogChangeNotificationContent: AlertDialog? = null
    private var suppressSwitchEvent = false
    private val myEmail = "ducnhuu0298@gmail.com"
    private val myZaloPhoneNumber = "0972800125"

    companion object {
        fun newInstance(
            listener: SettingDialogListener
        ): SettingDialogFragment {
            val fragment = SettingDialogFragment()
            fragment.listener = listener
            return fragment
        }
    }

    override fun onCreateDialog(savedInstanceState: Bundle?): Dialog {
        binding = DialogSettingBinding.inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        builder.setView(binding.root)

        setupUI(binding)

        val dialog = builder.create()
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        return dialog
    }

    override fun onAttach(context: Context) {
        super.onAttach(context)
        activity?.let {
            if (it is MainActivity)
                listener = it
            else
                Log.e("SettingDialogFragment", "$it must implement SettingDialogListener")
        }
    }

    override fun onResume() {
        super.onResume()
        loadNotificationSoundNameAsync()
    }

    private fun setupUI(binding: DialogSettingBinding) {
        // Bật / Tắt toàn bộ ứng dụng
        MyNotificationListenerService.isNotificationListenerEnabled =
            SharedPreferencesManager.isNotificationListenerEnabled()
        binding.switchAppEnabled.isChecked =
            MyNotificationListenerService.isNotificationListenerEnabled

        binding.switchAppEnabled.setOnCheckedChangeListener { _, isChecked ->
            if (suppressSwitchEvent) return@setOnCheckedChangeListener
            if (!isChecked) {
                suppressSwitchEvent = true
                binding.switchAppEnabled.isChecked = true
                suppressSwitchEvent = false
                showTurnOffConfirmationDialog(binding)
            } else {
                applyAppEnabledChange(true)
            }
        }

        binding.switchNightMode.isChecked =
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        binding.switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.Main).launch {
                delay(250)  // Delay 220ms for animation
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
                SharedPreferencesManager.saveNightModeEnabled(isChecked)
            }
        }

        binding.btnCheckPermission.setOnClickListener { openDialogRequestPermissions(false) }

        binding.btnOpenTTSSetting.setOnClickListener { openTextToSpeechSetting() }

        val notificationReceivedEnabled = SharedPreferencesManager.isNotificationReceivedEnabled()
        binding.checkBoxReceivedNotification.isChecked = notificationReceivedEnabled
        binding.checkBoxReceivedNotification.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesManager.saveNotificationReceivedEnabled(isChecked)
            MyNotificationListenerService.isNotificationReceivedEnabled = isChecked
            makeToast(
                "Đã ${if (isChecked) "bật" else "tắt"} đọc khi nhận tiền vào.",
                false
            )
        }

        val notificationSentEnabled = SharedPreferencesManager.isNotificationSentEnabled()
        binding.checkBoxSentNotification.isChecked = notificationSentEnabled
        binding.checkBoxSentNotification.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesManager.saveNotificationSentEnabled(isChecked)
            MyNotificationListenerService.isNotificationSentEnabled = isChecked
            makeToast(
                "Đã ${if (isChecked) "bật" else "tắt"} đọc khi chuyển tiền đi.",
                false
            )
        }
        val notificationContentReceived = SharedPreferencesManager.getNotificationContentReceived()
        binding.tvNotificationContentReceived.text = notificationContentReceived
        binding.tvNotificationContentReceived.setOnClickListener {
            openChangeNotificationContentDialog(true)
        }

        val notificationContentSent = SharedPreferencesManager.getNotificationContentSent()
        binding.tvNotificationContentSent.text = notificationContentSent
        binding.tvNotificationContentSent.setOnClickListener {
            openChangeNotificationContentDialog(false)
        }

        binding.tvNotificationSound.setOnClickListener(listener.onTvNotificationSoundClicked())
        binding.btnResetNotificationSound.setOnClickListener {
            val defaultUri = "android.resource://${requireContext().packageName}/${R.raw.ting}"
            SharedPreferencesManager.saveNotificationSound(defaultUri)
            getNotificationSound()
            binding.tvNotificationSound.text = requireContext().getString(R.string.notification_sound_default_name)
        }

        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        binding.seekBarVolume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_NOTIFICATION)
        binding.seekBarVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_NOTIFICATION)
        binding.seekBarVolume.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    if (p0 != null) {
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_NOTIFICATION,
                            p0.progress,
                            0
                        )
                        volumePreviewHandler.removeCallbacks(volumePreviewRunnable)
                        volumePreviewHandler.postDelayed(volumePreviewRunnable, 600)
                    }
                }
            }
        )

        binding.btnBankFilter.setOnClickListener { openBankFilterDialog() }
        binding.btnBackupRestore.setOnClickListener { openDialogBackupRestore() }
        binding.btnRestoreSetting.setOnClickListener { restoreSetting() }
        binding.btnDeleteData.setOnClickListener { confirmDeleteData() }
        binding.btnContactInfo.setOnClickListener { openContactInfoDialog() }
        binding.btnShowTTSHelper.setOnClickListener { openDialogTTSHelper() }
        binding.btnRateApp.setOnClickListener { openPlayStoreForRating() }
        binding.btnClose.setOnClickListener { this.dismiss() }
        val (versionName, versionCode) = requireContext().getAppVersionInfo()
        binding.tvVersion.text = "Có gì mới - V$versionName ($versionCode)"
        binding.tvVersion.setOnClickListener { openVersionHistoryDialog() }
        binding.tvVersion.addClickAnimation()

        binding.btnCheckPermission.addClickAnimation()
        binding.btnBankFilter.addClickAnimation()
        binding.btnBackupRestore.addClickAnimation()
        binding.btnOpenTTSSetting.addClickAnimation()
        binding.btnRestoreSetting.addClickAnimation()
        binding.btnDeleteData.addClickAnimation()
        binding.btnRateApp.addClickAnimation()
        binding.btnContactInfo.addClickAnimation()
        binding.btnShowTTSHelper.addClickAnimation()
        binding.btnClose.addClickAnimation()
    }

    private fun applyAppEnabledChange(isEnabled: Boolean) {
        MyNotificationListenerService.isNotificationListenerEnabled = isEnabled
        SharedPreferencesManager.saveNotificationListenerEnabled(isEnabled)
        listener.onAppEnabledChanged(isEnabled)

        val component = ComponentName(requireContext(), MyNotificationListenerService::class.java)
        if (isEnabled) {
            NotificationListenerService.requestRebind(component)
        } else {
            MyNotificationListenerService.instance?.requestUnbind()
        }

        makeToast(if (isEnabled) "Ứng dụng đang hoạt động" else "Ứng dụng đã tắt", false)
    }

    private fun showTurnOffConfirmationDialog(binding: DialogSettingBinding) {
        val dialogBinding = com.app.docthongbaochuyenkhoan.databinding.DialogConfirmTurnOffBinding
            .inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        builder.setView(dialogBinding.root)

        val dialog = builder.create()
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        dialogBinding.iBtnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnCancel.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnConfirmTurnOff.setOnClickListener {
            suppressSwitchEvent = true
            binding.switchAppEnabled.isChecked = false
            suppressSwitchEvent = false
            applyAppEnabledChange(false)
            dialog.dismiss()
        }

        dialogBinding.iBtnClose.addClickAnimation()
        dialogBinding.btnCancel.addClickAnimation()
        dialogBinding.btnConfirmTurnOff.addClickAnimation()

        dialog.show()
    }

    private fun openDialogRequestPermissions(autoClose: Boolean) {
        val dialog = RequestPermissionsDialogFragment.newInstance(autoClose)
        dialog.show(requireActivity().supportFragmentManager, "RequestPermissionsDialogFragment")
    }

    private fun openBankFilterDialog() {
        BankFilterDialogFragment.newInstance()
            .show(parentFragmentManager, "BankFilterDialogFragment")
    }

    private fun openDialogBackupRestore() {
        val dialog = BackupRestoreDialogFragment.newInstance()
        dialog.show(parentFragmentManager, "BackupRestoreDialogFragment")
    }

    private fun openTextToSpeechSetting() {
        val intent = Intent()
        intent.setAction("com.android.settings.TTS_SETTINGS")
        startActivity(intent)
    }

    private fun loadNotificationSoundNameAsync() {
        getNotificationSound()
        val uri = notificationSoundUri
        val defaultName = requireContext().getString(R.string.notification_sound_default_name)
        val defaultRawUri = "android.resource://${requireContext().packageName}/${R.raw.ting}"
        if (uri == null || uri.toString() == defaultRawUri) {
            binding.tvNotificationSound.text = defaultName
            return
        }
        val ctx = requireContext().applicationContext
        lifecycleScope.launch {
            val title = withContext(Dispatchers.IO) {
                runCatching {
                    RingtoneManager.getRingtone(ctx, uri)?.getTitle(ctx)
                }.getOrNull() ?: defaultName
            }
            if (isAdded) binding.tvNotificationSound.text = title
        }
    }

    private fun openChangeNotificationContentDialog(
        isReceivedNotification: Boolean
    ) {
        if (dialogChangeNotificationContent == null || !dialogChangeNotificationContent!!.isShowing) {
            val builder =
                AlertDialog.Builder(
                    requireContext(),
                    R.style.CustomDialogTheme
                )
            val binding =
                DialogChangeNotificationContentBinding.inflate(layoutInflater)
            builder.setView(binding.root)

            dialogChangeNotificationContent = builder.create()
            dialogChangeNotificationContent.let { dialog ->
                if (dialog != null) {
                    dialog.window?.setGravity(Gravity.CENTER)
                    dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
                    dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
                    dialog.setOnDismissListener {
                        dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
                    }

                    val content =
                        if (isReceivedNotification) SharedPreferencesManager.getNotificationContentReceived() else SharedPreferencesManager.getNotificationContentSent()

                    binding.let { b ->
                        b.etContent.let {
                            it.setText(content)
                            it.requestFocus()
                            dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_VISIBLE)
                        }

                        b.btnConfirm.setOnClickListener {
                            if (content != b.etContent.text.toString()) {
                                if (isReceivedNotification)
                                    SharedPreferencesManager.saveNotificationContentReceived(
                                        b.etContent.text.toString()
                                    )
                                else
                                    SharedPreferencesManager.saveNotificationContentSent(
                                        b.etContent.text.toString()
                                    )

                                makeToast("Nội dung thông báo đã thay đổi", false)
                                notifyNotificationContentChanged()
                                dialog.dismiss()
                            } else
                                makeToast("Nội dung không thay đổi", false)
                        }

                        b.btnClose.setOnClickListener {
                            dialog.dismiss()
                        }
                        b.btnClose.addClickAnimation()
                    }

                    dialog.show()
                }
            }
        }
    }

    private fun openContactInfoDialog() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        val bindingDialogContactInfo = DialogContactInfoBinding.inflate(layoutInflater)
        builder.setView(bindingDialogContactInfo.root)

        val dialog = builder.create()
        dialog.let {
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
            dialog.setOnDismissListener {
                dialog.window?.setSoftInputMode(WindowManager.LayoutParams.SOFT_INPUT_STATE_ALWAYS_HIDDEN)
            }
            bindingDialogContactInfo.tvEmail.text = myEmail
            bindingDialogContactInfo.emailLayout.setOnClickListener {
                val intent = Intent(Intent.ACTION_SENDTO).apply {
                    data = "mailto:".toUri()
                    putExtra(Intent.EXTRA_EMAIL, arrayOf(myEmail))
                    putExtra(Intent.EXTRA_SUBJECT, "Liên hệ từ ứng dụng Android của bạn")
                }
                startActivity(intent)

                dialog.dismiss() // Đóng dialog sau khi click
            }

            bindingDialogContactInfo.btnZalo.setOnClickListener {
                try {
                    val uri = "https://zalo.me/${myZaloPhoneNumber}".toUri()
                    val intent = Intent(Intent.ACTION_VIEW, uri)
                    startActivity(intent)
                } catch (e: Exception) {
                    makeToast("Không thể mở Zalo. Vui lòng đảm bảo Zalo đã được cài đặt.", false)
                    try {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "market://details?id=com.zing.zalo".toUri()
                            )
                        )
                    } catch (err: android.content.ActivityNotFoundException) {
                        startActivity(
                            Intent(
                                Intent.ACTION_VIEW,
                                "https://play.google.com/store/apps/details?id=com.zing.zalo".toUri()
                            )
                        )
                    }
                }
                dialog.dismiss() // Đóng dialog sau khi click
            }

            bindingDialogContactInfo.iBtnClose.setOnClickListener{
                dialog.dismiss()
            }

            bindingDialogContactInfo.btnClose.setOnClickListener {
                dialog.dismiss()
            }

            bindingDialogContactInfo.btnZalo.addClickAnimation()
            bindingDialogContactInfo.iBtnClose.addClickAnimation()
            bindingDialogContactInfo.btnClose.addClickAnimation()
        }
        dialog.show()
    }

    private fun openDialogTTSHelper() {
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        val dialogBinding = DialogTtsHelperBinding.inflate(layoutInflater)
        builder.setView(dialogBinding.root)

        val dialog = builder.create()
        dialog.let {
            dialog.window?.setGravity(Gravity.CENTER)
            dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
            dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation
        }

        dialogBinding.btnNotShowAgain.isChecked =
            SharedPreferencesManager.getNotShowAgainDialogSettingHelper()
        dialogBinding.btnNotShowAgain.setOnCheckedChangeListener { _, isChecked ->
            SharedPreferencesManager.saveNotShowAgainDialogSettingHelper(isChecked)
        }

        dialogBinding.iBtnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.btnClose.setOnClickListener {
            dialog.dismiss()
        }

        dialogBinding.iBtnClose.addClickAnimation()
        dialogBinding.btnClose.addClickAnimation()
        dialog.show()
    }

    private fun notifyNotificationContentChanged() {
        if (this::binding.isInitialized) {
            binding.tvNotificationContentReceived.text =
                SharedPreferencesManager.getNotificationContentReceived()
            binding.tvNotificationContentSent.text =
                SharedPreferencesManager.getNotificationContentSent()
        }
    }

    private fun getNotificationSound() {
        val notificationSound = SharedPreferencesManager.getNotificationSound()
        notificationSoundUri = if (notificationSound.isNotBlank())
            notificationSound.toUri()
        else null
    }

    private fun confirmDeleteData() {
        AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
            .setTitle("Xoá toàn bộ dữ liệu")
            .setMessage("Tất cả giao dịch sẽ bị xoá vĩnh viễn và không thể khôi phục.\n\nBạn có chắc chắn muốn tiếp tục?")
            .setPositiveButton("Xoá") { _, _ ->
                CoroutineScope(Dispatchers.IO).launch {
                    AppDatabase.getDatabase(requireContext()).transactionDao().deleteAll()
                }
                makeToast("Đã xoá toàn bộ dữ liệu giao dịch", false)
            }
            .setNegativeButton("Huỷ", null)
            .show()
    }

    private fun restoreSetting() {
        SharedPreferencesManager.restoreSetting()
        dialog?.dismiss()

        val newDialogFragment = newInstance(listener)
        newDialogFragment.show(requireFragmentManager(), "SettingDialogFragment")
    }

    private fun openPlayStoreForRating() {
        val appPackageName = "com.app.docthongbaochuyenkhoan" // Lấy package name của ứng dụng hiện tại
        try {
            // Cố gắng mở Play Store trực tiếp
            startActivity(Intent(Intent.ACTION_VIEW, "market://details?id=$appPackageName".toUri()))
        } catch (e: ActivityNotFoundException) {
            // Nếu Play Store không có, mở bằng trình duyệt
            startActivity(Intent(Intent.ACTION_VIEW,
                "https://play.google.com/store/apps/details?id=$appPackageName".toUri()))
        }

        // Thông báo cho người dùng, nếu cần
        makeToast( "Đánh giá ứng dụng 5 sao bạn nhé!", false)
    }

    private fun makeToast(msg: String, isLongToast: Boolean) {
        Toast.makeText(
            requireContext(),
            msg,
            if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        )
            .show()
    }

    private fun openVersionHistoryDialog() {
        val dialogBinding = com.app.docthongbaochuyenkhoan.databinding.DialogVersionHistoryBinding
            .inflate(layoutInflater)
        val builder = AlertDialog.Builder(requireContext(), R.style.CustomDialogTheme)
        builder.setView(dialogBinding.root)

        val dialog = builder.create()
        dialog.window?.setGravity(Gravity.CENTER)
        dialog.window?.setBackgroundDrawableResource(android.R.color.transparent)
        dialog.window?.attributes?.windowAnimations = R.style.CustomDialogAnimation

        val history = runCatching {
            requireContext().assets.open("version_history.txt")
                .bufferedReader()
                .readText()
        }.getOrDefault("Không thể tải lịch sử phiên bản.")
        dialogBinding.tvVersionHistory.text = history

        dialogBinding.iBtnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.btnClose.setOnClickListener { dialog.dismiss() }
        dialogBinding.iBtnClose.addClickAnimation()
        dialogBinding.btnClose.addClickAnimation()

        dialog.show()
    }

    override fun onDestroy() {
        super.onDestroy()

        binding.btnRestoreSetting.setOnClickListener { null }
        binding.btnContactInfo.setOnClickListener { null }
        binding.btnShowTTSHelper.setOnClickListener { null }
        binding.btnRateApp.setOnClickListener { null }
        binding.btnClose.setOnClickListener { null }
    }
}