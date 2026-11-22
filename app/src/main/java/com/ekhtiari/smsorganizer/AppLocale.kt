package com.ekhtiari.smsorganizer

import android.content.Context
import android.content.res.Configuration
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * The app chrome is English and LTR on every API this app supports.
 * Message text keeps its own direction; this only fixes the configuration locale.
 */
object AppLocale {
    private val english: Locale = Locale.ENGLISH

    fun wrap(base: Context): Context {
        Locale.setDefault(english)
        val config = Configuration(base.resources.configuration)
        config.setLocales(LocaleList(english))
        config.setLayoutDirection(english)
        return base.createConfigurationContext(config)
    }

    fun lock(context: Context) {
        Locale.setDefault(english)
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.TIRAMISU) return
        val manager = context.getSystemService(android.app.LocaleManager::class.java) ?: return
        val current = manager.applicationLocales
        val alreadyEnglish = current.size() == 1 && current[0]?.language == "en"
        if (!alreadyEnglish) {
            manager.applicationLocales = LocaleList.forLanguageTags("en")
        }
    }
}
