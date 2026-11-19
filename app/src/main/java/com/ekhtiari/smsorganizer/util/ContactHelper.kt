package com.ekhtiari.smsorganizer.util

import android.content.Context
import android.net.Uri
import android.provider.ContactsContract

object ContactHelper {
    private val cache = object : LinkedHashMap<String, String?>(64, 0.75f, true) {
        override fun removeEldestEntry(eldest: MutableMap.MutableEntry<String, String?>?): Boolean =
            size > 256
    }

    @Synchronized
    fun resolveDisplayName(context: Context, address: String): String? {
        if (address.isBlank()) return null
        if (cache.containsKey(address)) return cache[address]

        val uri = Uri.withAppendedPath(
            ContactsContract.PhoneLookup.CONTENT_FILTER_URI,
            Uri.encode(address),
        )
        return try {
            context.contentResolver.query(
                uri,
                arrayOf(ContactsContract.PhoneLookup.DISPLAY_NAME),
                null,
                null,
                null,
            )?.use { cursor ->
                val name = if (cursor.moveToFirst()) {
                    cursor.getString(0)
                } else null
                cache[address] = name
                name
            }
        } catch (_: SecurityException) {
            null
        }
    }

    @Synchronized
    fun clearCache() = cache.clear()
}
