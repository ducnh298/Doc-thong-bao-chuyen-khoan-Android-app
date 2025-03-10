package com.app.docthongbaochuyenkhoan.dialog

import android.app.Dialog
import android.content.Context
import android.content.Intent
import android.media.AudioManager
import android.media.RingtoneManager
import android.net.Uri
import android.os.Bundle
import android.util.Log
import android.view.Gravity
import android.view.View
import android.view.WindowManager
import android.widget.SeekBar
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatDelegate
import androidx.fragment.app.DialogFragment
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.activity.MainActivity
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import com.app.docthongbaochuyenkhoan.databinding.DialogChangeNotificationContentBinding
import com.app.docthongbaochuyenkhoan.databinding.DialogSettingBinding
import com.app.docthongbaochuyenkhoan.utils.AppUtils.Companion.addClickAnimation
import com.app.docthongbaochuyenkhoan.utils.MediaPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch


class SettingDialogFragment : DialogFragment() {

    interface SettingDialogListener {
        fun onTvNotificationSoundClicked(): View.OnClickListener
    }

    private lateinit var binding: DialogSettingBinding
    private lateinit var listener: SettingDialogListener
    private var notificationSoundUri: Uri? = null
    private var dialogChangeNotificationContent: AlertDialog? = null

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
        updateNotificationSoundName()
    }

    private fun setupUI(binding: DialogSettingBinding) {
        binding.switchNightMode.isChecked =
            AppCompatDelegate.getDefaultNightMode() == AppCompatDelegate.MODE_NIGHT_YES

        binding.switchNightMode.setOnCheckedChangeListener { _, isChecked ->
            CoroutineScope(Dispatchers.Main).launch {
                delay(230)  // Delay 230ms for animation
                AppCompatDelegate.setDefaultNightMode(
                    if (isChecked) AppCompatDelegate.MODE_NIGHT_YES else AppCompatDelegate.MODE_NIGHT_NO
                )
                SharedPreferencesManager.saveNightModeEnabled(isChecked)
            }
        }

        binding.btnCheckPermission.setOnClickListener { openDialogRequestPermissions(false) }

        binding.btnOpenTTSSetting.setOnClickListener { openTextToSpeechSetting() }

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

        val audioManager = requireContext().getSystemService(Context.AUDIO_SERVICE) as AudioManager
        binding.seekBarVolume.max = audioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC)
        binding.seekBarVolume.progress = audioManager.getStreamVolume(AudioManager.STREAM_MUSIC)
        binding.seekBarVolume.setOnSeekBarChangeListener(
            object : SeekBar.OnSeekBarChangeListener {
                override fun onProgressChanged(p0: SeekBar?, p1: Int, p2: Boolean) {
                }

                override fun onStartTrackingTouch(p0: SeekBar?) {
                }

                override fun onStopTrackingTouch(p0: SeekBar?) {
                    if (p0 != null) {
                        audioManager.setStreamVolume(
                            AudioManager.STREAM_MUSIC,
                            p0.progress,
                            0
                        )
                        MediaPlayerUtils.playMedia(
                            requireContext(),
                            notificationSoundUri
                        )
                    }
                }
            }
        )

        binding.btnRestoreSetting.setOnClickListener { restoreSetting() }
        binding.btnClose.setOnClickListener { this.dismiss() }

        binding.btnCheckPermission.addClickAnimation()
        binding.btnOpenTTSSetting.addClickAnimation()
        binding.btnRestoreSetting.addClickAnimation()
        binding.btnClose.addClickAnimation()
    }

    private fun openDialogRequestPermissions(autoClose: Boolean) {
        val dialog = RequestPermissionsDialogFragment.newInstance(autoClose)
        dialog.show(requireActivity().supportFragmentManager, "RequestPermissionsDialogFragment")
    }

    private fun openTextToSpeechSetting() {
        val intent = Intent()
        intent.setAction("com.android.settings.TTS_SETTINGS")
        startActivity(intent)
    }

    private fun updateNotificationSoundName() {
        getNotificationSound()

        if (notificationSoundUri != null) {
            val ringtone = RingtoneManager.getRingtone(
                requireContext(),
                notificationSoundUri
            )
            if (ringtone != null) {
                val ringtoneTitle = ringtone.getTitle(requireContext())
                binding.tvNotificationSound.text = ringtoneTitle
            }
        } else
            binding.tvNotificationSound.text =
                requireContext().getString(R.string.notification_sound_default_name)
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
            Uri.parse(notificationSound)
        else null
    }

    private fun restoreSetting() {
        SharedPreferencesManager.restoreSetting()
        dialog?.dismiss()

        val newDialogFragment = newInstance(listener)
        newDialogFragment.show(requireFragmentManager(), "SettingDialogFragment")
    }

    private fun makeToast(msg: String, isLongToast: Boolean) {
        Toast.makeText(
            requireContext(),
            msg,
            if (isLongToast) Toast.LENGTH_LONG else Toast.LENGTH_SHORT
        )
            .show()
    }

    override fun onDestroy() {
        super.onDestroy()
    }
}