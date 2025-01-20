package com.app.docthongbaochuyenkhoan.controller

import android.content.Context
import android.content.SharedPreferences
import android.net.Uri
import android.os.Vibrator
import android.speech.tts.TextToSpeech
import android.util.Log
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.model.Transaction
import com.app.docthongbaochuyenkhoan.utils.AppUtils
import com.app.docthongbaochuyenkhoan.utils.MediaPlayerUtils
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import java.util.Locale

class NotificationReader(private var context: Context) : TextToSpeech.OnInitListener {
    private var textToSpeech: TextToSpeech = TextToSpeech(context, this)
    private var vibrator: Vibrator = context.getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
    private val scope: CoroutineScope = CoroutineScope(Dispatchers.IO)
    private var notificationSoundUri: Uri? = null
    private val notificationQueue = mutableListOf<String>()
    private var isReading = false
    private var isSuccessFullyInit = false

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            textToSpeech.language = Locale("vi")
            textToSpeech.setOnUtteranceCompletedListener {
                isReading = false
            }
            isSuccessFullyInit = true
        }

        notificationSoundUri = Uri.parse(SharedPreferencesManager.getNotificationSound())

        SharedPreferencesManager.registerOnSharedPreferenceChangeListener(preferenceChangeListener)
    }

    private val preferenceChangeListener =
        SharedPreferences.OnSharedPreferenceChangeListener { _, key ->
            if (key == context.getString(R.string.notification_sound))
                notificationSoundUri = Uri.parse(SharedPreferencesManager.getNotificationSound())
        }

    fun addNotification(transaction: Transaction) {
        val notification = StringBuilder(transaction.bank.displayName)

        if (transaction.amount > 0) {
            val notificationContent = SharedPreferencesManager.getNotificationContentReceived()
            notification.append(" $notificationContent")
            notification.append(" ${AppUtils.formatCurrency(transaction.amount)}")
        } else {
            val notificationContent = SharedPreferencesManager.getNotificationContentSent()
            notification.append(" $notificationContent")
            notification.append(" ${AppUtils.formatCurrency(-transaction.amount)}")
        }

        Log.d("NotificationReader", "addNotification: $notification")

        notificationQueue.add(notification.toString())
        processQueue()
    }

    private fun processQueue() {
        if (isReading || notificationQueue.isEmpty()) return // Processed or no notification
        isReading = true

        // Get the first message out of the queue
        val currentNotification = notificationQueue.removeAt(0)

        // Check when TTS is done to process the next message
        scope.launch(Dispatchers.IO) {
            // Notification
            while (!isSuccessFullyInit)
                delay(100)

            readNotification(currentNotification)

            while (textToSpeech.isSpeaking) {
                delay(100) // Chờ 100ms
            }
            isReading = false // The current message is done
            processQueue() // Continue processing the next message
        }
    }

    private fun readNotification(notification: String?) {
        makeVibration()
        playNotificationSound()
        textToSpeech.speak(notification, TextToSpeech.QUEUE_FLUSH, null, null)
    }

    private fun playNotificationSound() {
        MediaPlayerUtils.playMedia(
            context,
            notificationSoundUri
        )
    }

    private fun makeVibration() {
        if (vibrator.hasVibrator()) {
            val mVibratePattern: LongArray = longArrayOf(0, 200, 100, 200)

            // -1: If you don't want it to repeat
            // 0 if you want it to repeat from the first element position
            vibrator.vibrate(mVibratePattern, -1)
        }
    }

    fun onDestroy() {
        textToSpeech.stop()
        textToSpeech.shutdown()
        SharedPreferencesManager.unRegisterOnSharedPreferenceChangeListener(preferenceChangeListener)
    }
}