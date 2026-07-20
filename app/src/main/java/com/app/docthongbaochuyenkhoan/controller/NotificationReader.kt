package com.app.docthongbaochuyenkhoan.controller

import android.content.Context
import android.content.SharedPreferences
import android.media.AudioManager
import android.net.Uri
import android.os.Bundle
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.utils.AppUtils
import com.app.docthongbaochuyenkhoan.utils.MediaPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale
import androidx.core.net.toUri
import kotlin.time.Duration.Companion.milliseconds

class NotificationReader(private var context: Context) : TextToSpeech.OnInitListener {

    companion object {
        private const val TAG = "NotifReader"
        private const val GOOGLE_TTS_ENGINE = "com.google.android.tts"
    }

    // Thử Google TTS trước — nhiều hãng (Xiaomi, Huawei) đặt engine riêng làm default,
    // engine đó không hỗ trợ tiếng Việt dù Google TTS đã cài.
    private var textToSpeech: TextToSpeech = TextToSpeech(context, this, GOOGLE_TTS_ENGINE)
    private var usingGoogleEngine = true
    private var preferenceListenerRegistered = false

    private var vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val job = Job()
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO + job)
    private var notificationSoundUri: Uri? = null
    private val notificationQueue = mutableListOf<String>()
    @Volatile private var isReading = false
    @Volatile private var isSuccessFullyInit = false

    override fun onInit(status: Int) {
        val engineLabel = if (usingGoogleEngine) "Google TTS" else "system default"
        Log.i(TAG, "TTS onInit: engine=$engineLabel status=${if (status == TextToSpeech.SUCCESS) "SUCCESS" else "FAILED($status)"}")

        if (status == TextToSpeech.SUCCESS) {
            val langResult = textToSpeech.setLanguage(Locale("vi"))
            Log.i(TAG, "TTS setLanguage(vi): result=$langResult engine=$engineLabel")

            if (langResult >= TextToSpeech.LANG_AVAILABLE) {
                textToSpeech.setOnUtteranceCompletedListener { isReading = false }
                isSuccessFullyInit = true
                Log.i(TAG, "TTS ready with $engineLabel (Vietnamese supported)")
            } else {
                // Engine khởi động được nhưng không hỗ trợ tiếng Việt
                Log.w(TAG, "Vietnamese not available on $engineLabel (langResult=$langResult)")
                if (usingGoogleEngine) {
                    fallbackToSystemDefault()
                } else {
                    Log.e(TAG, "No TTS engine supports Vietnamese. User should install/configure Google TTS with Vietnamese data.")
                    textToSpeech.setOnUtteranceCompletedListener { isReading = false }
                    isSuccessFullyInit = true // tránh block queue mãi mãi
                }
            }
        } else {
            // Engine không khởi động được (chưa cài / lỗi)
            if (usingGoogleEngine) {
                Log.w(TAG, "Google TTS engine not available, falling back to system default")
                fallbackToSystemDefault()
            } else {
                Log.e(TAG, "All TTS engines failed to initialize")
                isSuccessFullyInit = true // tránh block queue mãi mãi
            }
        }

        // Chỉ đăng ký 1 lần dù onInit có thể được gọi nhiều lần khi retry
        if (!preferenceListenerRegistered) {
            notificationSoundUri = savedSoundUri()
            SharedPreferencesManager.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
            preferenceListenerRegistered = true
        }
    }

    private fun fallbackToSystemDefault() {
        usingGoogleEngine = false
        textToSpeech.shutdown()
        textToSpeech = TextToSpeech(context, this) // onInit sẽ được gọi lại
    }

    private fun savedSoundUri(): Uri? {
        val path = SharedPreferencesManager.getNotificationSound()
        return if (path.isBlank()) null else path.toUri()
    }

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == context.getString(R.string.notification_sound))
                notificationSoundUri = savedSoundUri()
        }

    fun addNotification(transaction: Transaction) {
        Log.d(TAG, "addNotification: bank=${transaction.bank} amount=${transaction.amount} queueSize=${notificationQueue.size}")
        val notification = StringBuilder(transaction.bank.speakName)

        if (transaction.amount > 0) {
            val notificationContent = SharedPreferencesManager.getNotificationContentReceived()
            notification.append(" $notificationContent")
            notification.append(" ${AppUtils.formatCurrency(transaction.amount)}")
        } else {
            val notificationContent = SharedPreferencesManager.getNotificationContentSent()
            notification.append(" $notificationContent")
            notification.append(" ${AppUtils.formatCurrency(-transaction.amount)}")
        }

        notificationQueue.add(notification.toString())
        processQueue()
    }

    private fun processQueue() {
        if (isReading || notificationQueue.isEmpty()) return
        isReading = true

        val currentNotification = notificationQueue.removeAt(0)

        scope.launch(Dispatchers.IO) {
            while (!isSuccessFullyInit)
                delay(100.milliseconds)

            readNotification(currentNotification)

            while (textToSpeech.isSpeaking) {
                delay(100.milliseconds)
            }
            isReading = false
            processQueue()
        }
    }

    private fun readNotification(notification: String?) {
        Log.d(TAG, "readNotification: \"$notification\"")
        makeVibration()
        val params = Bundle().apply {
            putInt(TextToSpeech.Engine.KEY_PARAM_STREAM, AudioManager.STREAM_NOTIFICATION)
            putFloat(TextToSpeech.Engine.KEY_PARAM_VOLUME, 1.0f)
        }
        MediaPlayerUtils.playMedia(context, notificationSoundUri) {
            textToSpeech.speak(notification, TextToSpeech.QUEUE_FLUSH, params, null)
        }
    }

    private fun makeVibration() {
        if (vibrator.hasVibrator()) {
            vibrator.vibrate(longArrayOf(0, 200, 100, 200), -1)
        }
    }

    fun onDestroy() {
        job.cancel()
        textToSpeech.stop()
        textToSpeech.shutdown()
        SharedPreferencesManager.unRegisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}
