package com.iboalali.basicrootchecker.util

import org.junit.Assert.assertEquals
import org.junit.Test

class AppLanguageTest {

    @Test
    fun `displayName returns the autonym for each supported language`() {
        assertEquals("English", AppLanguage.displayName("en"))
        assertEquals("Deutsch", AppLanguage.displayName("de"))
        assertEquals("العربية", AppLanguage.displayName("ar"))
        assertEquals("Español", AppLanguage.displayName("es"))
        assertEquals("Русский", AppLanguage.displayName("ru"))
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
