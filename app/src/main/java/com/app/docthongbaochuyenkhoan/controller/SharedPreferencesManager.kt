package com.app.docthongbaochuyenkhoan.controller

import android.content.Context
import android.content.SharedPreferences
import com.app.docthongbaochuyenkhoan.R

object SharedPreferencesManager {
    private const val PREFS_NAME = "app_prefs"
    private lateinit var appContext: Context
    private lateinit var sharedPreferences: SharedPreferences
    private lateinit var editor: SharedPreferences.Editor

    fun init(context: Context) {
        appContext = context.applicationContext
        sharedPreferences = context.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)
        editor = sharedPreferences.edit()
    }

    fun isInitialized(): Boolean {
        return this::sharedPreferences.isInitialized
    }

    private fun getDataString(key: String, defaultValue: String): String {
        return sharedPreferences.getString(key, defaultValue) ?: defaultValue
    }

    private fun saveDataString(key: String, value: String) {
        editor.putString(key, value)
        editor.apply()
    }

    fun registerOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.registerOnSharedPreferenceChangeListener(listener)
    }

    fun unRegisterOnSharedPreferenceChangeListener(listener: SharedPreferences.OnSharedPreferenceChangeListener) {
        sharedPreferences.unregisterOnSharedPreferenceChangeListener(listener)
    }

    fun isNightModeEnabled(): Boolean {
        return getDataString(appContext.getString(R.string.night_mode_enabled), "false") == "true"
    }

    fun saveNightModeEnabled(isEnabled: Boolean) {
        saveDataString(appContext.getString(R.string.night_mode_enabled), isEnabled.toString())
    }

    fun isNotificationListenerEnabled(): Boolean {
        return getDataString(appContext.getString(R.string.notification_enabled), "false") == "true"
    }

    fun saveNotificationListenerEnabled(isEnabled: Boolean) {
        saveDataString(appContext.getString(R.string.notification_enabled), isEnabled.toString())
    }

    fun getNotificationContentReceived(): String {
        return getDataString(
            appContext.getString(R.string.notification_content_received),
            appContext.getString(R.string.notification_content_received_default)
        )
    }

    fun saveNotificationContentReceived(content: String) {
        saveDataString(appContext.getString(R.string.notification_content_received), content)
    }

    fun getNotificationContentSent(): String {
        return getDataString(
            appContext.getString(R.string.notification_content_sent),
            appContext.getString(R.string.notification_content_sent_default)
        )
    }

    fun saveNotificationContentSent(content: String) {
        saveDataString(appContext.getString(R.string.notification_content_sent), content)
    }

    fun getNotificationSound(): String {
        return getDataString(
            appContext.getString(R.string.notification_sound),
            ""
        )
    }

    fun saveNotificationSound(sound: String) {
        saveDataString(appContext.getString(R.string.notification_sound), sound)
    }

    fun removeNotificationSound() {
        editor.remove(appContext.getString(R.string.notification_sound))
        editor.commit()
    }

    fun restoreSetting() {
        editor.remove(appContext.getString(R.string.night_mode_enabled))
        editor.remove(appContext.getString(R.string.notification_content_received))
        editor.remove(appContext.getString(R.string.notification_content_sent))
        editor.remove(appContext.getString(R.string.notification_sound))

        editor.commit()
    }
}