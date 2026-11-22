package com.ekhtiari.smsorganizer.update

import android.content.Context
import android.content.SharedPreferences

internal class SharedPrefsUpdateStore(context: Context) : UpdateStore {
    private val prefs = context.applicationContext.getSharedPreferences(PREFS_NAME, Context.MODE_PRIVATE)

    override var lastCheckAtMillis: Long
        get() = prefs.getLong(KEY_LAST_CHECK, 0L)
        set(value) {
            prefs.edit().putLong(KEY_LAST_CHECK, value).apply()
        }

    override var lastCheckSucceeded: Boolean
        get() = prefs.getBoolean(KEY_LAST_OK, false)
        set(value) {
            prefs.edit().putBoolean(KEY_LAST_OK, value).apply()
        }

    override var cachedVersion: String?
        get() = prefs.optionalString(KEY_CACHED_VERSION)
        set(value) {
            prefs.putOptional(KEY_CACHED_VERSION, value)
        }

    override var cachedDownloadUrl: String?
        get() = prefs.optionalString(KEY_CACHED_URL)
        set(value) {
            prefs.putOptional(KEY_CACHED_URL, value)
        }

    override var dismissedVersion: String?
        get() = prefs.optionalString(KEY_DISMISSED)
        set(value) {
            // commit so "Later" survives the process being killed immediately after the tap.
            prefs.edit().putString(KEY_DISMISSED, value.orEmpty()).commit()
        }

    override var notifiedVersion: String?
        get() = prefs.optionalString(KEY_NOTIFIED)
        set(value) {
            prefs.putOptional(KEY_NOTIFIED, value)
        }

    override fun putCachedOffer(version: String?, downloadUrl: String?) {
        prefs.edit()
            .putString(KEY_CACHED_VERSION, version.orEmpty())
            .putString(KEY_CACHED_URL, downloadUrl.orEmpty())
            .apply()
    }

    private fun SharedPreferences.optionalString(key: String): String? =
        getString(key, null)?.takeIf { it.isNotEmpty() }

    private fun SharedPreferences.putOptional(key: String, value: String?) {
        edit().putString(key, value.orEmpty()).apply()
    }

    companion object {
        const val PREFS_NAME = "app_update"
        private const val KEY_LAST_CHECK = "last_check_at"
        private const val KEY_LAST_OK = "last_check_ok"
        private const val KEY_CACHED_VERSION = "cached_version"
        private const val KEY_CACHED_URL = "cached_url"
        private const val KEY_DISMISSED = "dismissed_version"
        private const val KEY_NOTIFIED = "notified_version"
    }
}
