package com.iboalali.basicrootchecker.data

/** User-selectable app theme. Persisted by [UserPreferences] and resolved in MainActivity. */
enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK;

    companion object {
        /** Parses a persisted value, falling back to [SYSTEM] for null/unknown input. */
        fun fromStorage(value: String?): ThemeMode = entries.firstOrNull { it.name == value } ?: SYSTEM
    }
}
