package com.iboalali.basicrootchecker.util

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `displayName returns the autonym for each supported language`() {
        assertEquals("English", AppLanguage.displayName("en"))
        assertEquals("Deutsch", AppLanguage.displayName("de"))
        assertEquals("العربية", AppLanguage.displayName("ar"))
        assertEquals("Español", AppLanguage.displayName("es"))
        assertEquals("Русский", AppLanguage.displayName("ru"))
        assertEquals("Nederlands", AppLanguage.displayName("nl"))
        assertEquals("中文", AppLanguage.displayName("zh-Hans"))
        assertEquals("Melayu", AppLanguage.displayName("ms"))
        assertEquals("தமிழ்", AppLanguage.displayName("ta"))
    }

    @Test
    fun `bestSupportedTag returns a supported tag unchanged`() {
        AppLanguage.SUPPORTED_TAGS.forEach { tag ->
            assertEquals(tag, AppLanguage.bestSupportedTag(tag))
        }
    }

    @Test
    fun `bestSupportedTag matches regional tags by prefix`() {
        assertEquals("de", AppLanguage.bestSupportedTag("de-AT"))
        assertEquals("en", AppLanguage.bestSupportedTag("en-US"))
        assertEquals("ar", AppLanguage.bestSupportedTag("ar-EG"))
        assertEquals("zh-Hans", AppLanguage.bestSupportedTag("zh-Hans-CN"))
        assertEquals("zh-Hans", AppLanguage.bestSupportedTag("zh-hans-sg"))
        assertEquals("ta", AppLanguage.bestSupportedTag("ta-IN-u-nu-tamldec"))
    }

    @Test
    fun `bestSupportedTag returns null when nothing matches`() {
        assertNull(AppLanguage.bestSupportedTag(null))
        assertNull(AppLanguage.bestSupportedTag(""))
        assertNull(AppLanguage.bestSupportedTag("fr-FR"))
        assertNull(AppLanguage.bestSupportedTag("zh-Hant-TW"))
        assertNull(AppLanguage.bestSupportedTag("ms2"))
    }

    @Test
    fun `displayName capitalizes the first letter`() {
        AppLanguage.SUPPORTED_TAGS.forEach { tag ->
            val name = AppLanguage.displayName(tag)
            assertEquals(
                "display name for '$tag' should start with an uppercase letter",
                name.substring(0, 1).uppercase(),
                name.substring(0, 1),
            )
        }
    }
}
