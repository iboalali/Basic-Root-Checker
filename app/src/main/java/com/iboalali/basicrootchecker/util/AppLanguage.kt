package com.iboalali.basicrootchecker.util

import android.app.LocaleManager
import android.content.Context
import android.os.Build
import android.os.LocaleList
import java.util.Locale

/**
 * Helpers for the in-app language picker. Per-app language is a platform feature since
 * Android 13 (API 33); the framework both applies the chosen locale (recreating the activity)
 * and persists it across launches via [LocaleManager], so there is no local cache to keep in sync.
 */
object AppLanguage {

    /**
     * Languages the app ships translations for. Keep in sync with
     * `res/xml/app_locales_config.xml` and `androidResources.localeFilters` in `app/build.gradle.kts`.
     */
    val SUPPORTED_TAGS: List<String> =
        listOf("en", "de", "ar", "es", "ru", "nl", "zh-Hans", "ms", "ta")

    val isSupported: Boolean
        get() = Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU

    /**
     * The language's own name (autonym), e.g. "Deutsch", "العربية", "Español", with the first
     * letter capitalized in that language (some locales return a lowercase display name).
     */
    fun displayName(tag: String): String {
        val locale = Locale.forLanguageTag(tag)
        return locale.getDisplayLanguage(locale).replaceFirstChar {
            if (it.isLowerCase()) it.titlecase(locale) else it.toString()
        }
    }

    /** The current app language tag, or `null` when following the system default. */
    fun currentTag(context: Context): String? {
        if (!isSupported) return null
        val locales = context.getSystemService(LocaleManager::class.java).applicationLocales
        return if (locales.isEmpty) null else locales[0].toLanguageTag()
    }

    /**
     * The entry of [SUPPORTED_TAGS] that [tag] falls under, matched by the longest subtag prefix,
     * or `null` if none does. The system's per-app language page sets regional tags such as
     * `de-AT` or `zh-Hans-CN`, which must still select `de` and `zh-Hans` in the picker.
     */
    fun bestSupportedTag(tag: String?): String? {
        if (tag.isNullOrEmpty()) return null
        var subtags = tag.split('-')
        while (subtags.isNotEmpty()) {
            val prefix = subtags.joinToString("-")
            SUPPORTED_TAGS.firstOrNull { it.equals(prefix, ignoreCase = true) }?.let { return it }
            subtags = subtags.dropLast(1)
        }
        return null
    }

    /** Pass `null` to follow the system default. No-op below Android 13. */
    fun setLanguage(context: Context, tag: String?) {
        if (!isSupported) return
        val localeList = if (tag == null) {
            LocaleList.getEmptyLocaleList()
        } else {
            LocaleList.forLanguageTags(tag)
        }
        context.getSystemService(LocaleManager::class.java).applicationLocales = localeList
    }
}
