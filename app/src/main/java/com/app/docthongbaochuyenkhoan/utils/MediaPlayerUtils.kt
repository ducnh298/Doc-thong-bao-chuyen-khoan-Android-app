package com.app.docthongbaochuyenkhoan.utils

import android.content.Context
import android.media.AudioAttributes
import android.media.AudioManager
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager

class MediaPlayerUtils {
    companion object {

        private var currentMediaPlayer: MediaPlayer? = null

        fun playMedia(
            context: Context,
            soundUri: Uri?,
            onComplete: (() -> Unit)? = null
        ) {
            try {
                safeStopAndRelease(currentMediaPlayer)
                currentMediaPlayer = null

                val audioAttributes = AudioAttributes.Builder()
                    .setUsage(AudioAttributes.USAGE_NOTIFICATION)
                    .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
                    .build()

                // Try custom sound first, fall back to bundled default if it fails or is null
                val mediaPlayer: MediaPlayer? = if (soundUri != null) {
                    val mp = try {
                        MediaPlayer.create(context, soundUri, null, audioAttributes, AudioManager.AUDIO_SESSION_ID_GENERATE)
                    } catch (e: Exception) {
                        Log.e("playMedia", "Custom sound failed: ${e.message}")
                        null
                    }
                    if (mp == null) {
                        SharedPreferencesManager.removeNotificationSound()
                        Handler(Looper.getMainLooper()).post {
                            Toast.makeText(context, "Âm thanh thông báo không hợp lệ, đã đặt lại về mặc định.", Toast.LENGTH_SHORT).show()
                        }
                        MediaPlayer.create(context, R.raw.ting)
                    } else mp
                } else {
                    MediaPlayer.create(context, R.raw.ting)
                }

                if (mediaPlayer == null) {
                    Log.e("playMedia", "MediaPlayer creation failed, skipping sound")
                    onComplete?.invoke()
                    return
                }

                currentMediaPlayer = mediaPlayer

                var completed = false
                fun fireComplete() {
                    if (!completed) {
                        completed = true
                        onComplete?.invoke()
                    }
                }

                mediaPlayer.setOnCompletionListener {
                    safeStopAndRelease(it)
                    currentMediaPlayer = null
                    fireComplete()
                }
                mediaPlayer.setOnErrorListener { mp, what, extra ->
                    Log.e("MediaPlayer Error", "Error: what=$what, extra=$extra")
                    safeStopAndRelease(mp)
                    currentMediaPlayer = null
                    fireComplete()
                    true
                }

                mediaPlayer.start()

                val duration = try { mediaPlayer.duration } catch (e: Exception) { -1 }
                // Trigger TTS ~300ms before sound ends so reading starts as bell fades out.
                // Safety net fires at duration + 200ms in case onCompletion doesn't fire.
                val earlyTriggerMs = if (duration > 300) (duration - 300L) else 0L
                val safetyNetMs = if (duration > 0) (duration + 200L) else 1000L

                if (earlyTriggerMs > 0) {
                    Handler(Looper.getMainLooper()).postDelayed({
                        if (!completed) {
                            completed = true
                            onComplete?.invoke()
                        }
                    }, earlyTriggerMs)
                }

                Handler(Looper.getMainLooper()).postDelayed({
                    if (!completed) {
                        safeStopAndRelease(mediaPlayer)
                        currentMediaPlayer = null
                        fireComplete()
                    }
                }, safetyNetMs)

            } catch (e: Exception) {
                Log.e("playMedia", "Unexpected error: ${e.message}")
                onComplete?.invoke()
            }
        }

        private fun safeStopAndRelease(mediaPlayer: MediaPlayer?) {
            try {
                mediaPlayer?.let {
                    if (it.isPlaying) { // Check status
                        it.stop()
                    }
                    it.release()
                }
            } catch (e: IllegalStateException) {
                Log.e("MediaPlayer", "MediaPlayer is in an invalid state: ${e.message}")
            } catch (e: Exception) {
                Log.e("MediaPlayer", "Unexpected error: ${e.message}")
            }
        }
    }
}