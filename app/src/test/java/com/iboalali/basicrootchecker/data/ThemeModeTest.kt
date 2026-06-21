package com.iboalali.basicrootchecker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class ThemeModeTest {

    @Test
    fun `fromStorage maps each enum name back to its value`() {
        ThemeMode.entries.forEach { mode ->
            assertEquals(mode, ThemeMode.fromStorage(mode.name))
        }
    }

    @Test
    fun `fromStorage falls back to SYSTEM for null`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(null))
    }

    @Test
    fun `fromStorage falls back to SYSTEM for empty or unknown values`() {
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage(""))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("garbage"))
        assertEquals(ThemeMode.SYSTEM, ThemeMode.fromStorage("light")) // case-sensitive
    }
}
