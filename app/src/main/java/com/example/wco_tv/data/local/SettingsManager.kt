package com.example.wco_tv.data.local

import android.content.Context
import android.content.SharedPreferences

class SettingsManager(context: Context) {
    private val prefs: SharedPreferences = context.getSharedPreferences("wco_settings", Context.MODE_PRIVATE)

    companion object {
        private const val KEY_BASE_URL = "base_url"
        private const val DEFAULT_BASE_URL = "https://www.wcoflix.tv"
    }

    fun getBaseUrl(): String {
        return prefs.getString(KEY_BASE_URL, DEFAULT_BASE_URL) ?: DEFAULT_BASE_URL
    }

    fun saveBaseUrl(url: String) {
        // Ensure no trailing slash for consistency, unless it's just root which is rare for this usage
        val cleanUrl = if (url.endsWith("/")) url.dropLast(1) else url
        prefs.edit().putString(KEY_BASE_URL, cleanUrl).apply()
    }
}
