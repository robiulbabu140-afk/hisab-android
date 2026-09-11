package com.hisab.app.data.remote

import android.content.Context

/**
 * User-entered backend connection details (Settings screen), plain SharedPreferences —
 * this is the user's own server address + a token they generated themselves, not a
 * platform credential, so storing it locally like any other app setting is appropriate.
 */
class SyncPrefs(context: Context) {
    private val prefs = context.getSharedPreferences("hisab_sync_prefs", Context.MODE_PRIVATE)

    var baseUrl: String
        get() = prefs.getString(KEY_BASE_URL, "") ?: ""
        set(value) = prefs.edit().putString(KEY_BASE_URL, value.trim().trimEnd('/')).apply()

    var apiKey: String
        get() = prefs.getString(KEY_API_KEY, "") ?: ""
        set(value) = prefs.edit().putString(KEY_API_KEY, value.trim()).apply()

    var lastSyncMillis: Long
        get() = prefs.getLong(KEY_LAST_SYNC, 0L)
        set(value) = prefs.edit().putLong(KEY_LAST_SYNC, value).apply()

    var autoSyncEnabled: Boolean
        get() = prefs.getBoolean(KEY_AUTO_SYNC, false)
        set(value) = prefs.edit().putBoolean(KEY_AUTO_SYNC, value).apply()

    fun isConfigured(): Boolean = baseUrl.isNotBlank() && apiKey.isNotBlank()

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val KEY_API_KEY = "api_key"
        private const val KEY_LAST_SYNC = "last_sync_millis"
        private const val KEY_AUTO_SYNC = "auto_sync_enabled"
    }
}
