package com.app.docthongbaochuyenkhoan.utils

import android.content.Context
import android.media.MediaPlayer
import android.net.Uri
import android.os.Handler
import android.os.Looper
import android.util.Log
import android.widget.Toast
import com.app.docthongbaochuyenkhoan.R
import com.app.docthongbaochuyenkhoan.controller.SharedPreferencesManager
import java.io.IOException

class MediaPlayerUtils {
    companion object {

        private var currentMediaPlayer: MediaPlayer? = null

        fun playMedia(
            context: Context,
            soundUri: Uri?
        ) {
            try {
                // Safely stop and release the old MediaPlayer
                safeStopAndRelease(currentMediaPlayer)
                currentMediaPlayer = null

                // Create new MediaPlayer
                val mediaPlayer: MediaPlayer = try {
                    if (soundUri != null) {
                        MediaPlayer.create(context, soundUri)
                    } else {
                        MediaPlayer.create(context, R.raw.ting)
                    }
                } catch (e: Exception) {
                    Log.e("playMedia", "Error: ${e.message}")
                    Toast.makeText(
                        context,
                        "Không thể phát âm thanh thông báo mới, sử dụng âm thanh mặc định.",
                        Toast.LENGTH_LONG
                    ).show()
                    SharedPreferencesManager.removeNotificationSound()

                    MediaPlayer.create(context, R.raw.ting)
                }

                currentMediaPlayer = mediaPlayer

                // Attach listeners
                mediaPlayer.setOnPreparedListener {
                    it.start() // Start playing when ready
                }
                // Attach listener and start playing
                mediaPlayer.setOnCompletionListener {
                    safeStopAndRelease(it)
                    currentMediaPlayer = null
                }
                mediaPlayer.setOnErrorListener { mp, what, extra ->
                    Log.e(
                        "MediaPlayer Error",
                        "Error occurred: what=$what, extra=$extra"
                    )
                    safeStopAndRelease(mp)
                    currentMediaPlayer = null
                    true // Indicates we handled the error
                }

                mediaPlayer.start()


                // Stop after time limit
                Handler(Looper.getMainLooper()).postDelayed({
                    safeStopAndRelease(mediaPlayer)
                    currentMediaPlayer = null
                }, 2000)  // 2 seconds

            } catch (e: IOException) {
                Log.e("playMedia", "Error setting data source: ${e.message}")
                e.printStackTrace()
            } catch (e: IllegalStateException) {
                Log.e("playMedia", "MediaPlayer is in an invalid state: ${e.message}")
                e.printStackTrace()
            } catch (e: Exception) {
                Log.e("playMedia", "Unexpected error: ${e.message}")
                e.printStackTrace()
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