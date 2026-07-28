package com.iboalali.basicrootchecker.navigation

import androidx.compose.runtime.compositionLocalOf

/**
 * Which leading navigation icon a secondary screen (Settings / About / License) should draw,
 * depending on how it's currently presented:
 *
 * - [BACK] — pushed full-screen (phones / medium widths): the up-arrow returns to the main screen.
 * - [CLOSE] — shown as a dialog over the main screen (expanded widths): an X dismisses the dialog.
 *
 * Both invoke the same `onNavigateBack`; only the glyph and its content description differ.
 */
enum class DetailNavIcon { BACK, CLOSE }

/** Defaults to [DetailNavIcon.BACK]; `AppNavigation` provides [DetailNavIcon.CLOSE] at expanded width. */
val LocalDetailNavIcon = compositionLocalOf { DetailNavIcon.BACK }

// There used to be a `Modifier.detailDialogShape()` here that rounded a secondary screen's corners
// when it was shown as a dialog. It existed because the built-in `DialogSceneStrategy` rendered the
// screen's `Scaffold` directly, with square corners. The custom overlay replaced that: the screen is
// now hosted inside `DetailCard`'s `Surface(shape = RoundedCornerShape(DetailCardDefaults.CornerRadius))`,
// which already clips it — so the modifier was a third redundant clip at the same radius, and a
// second place the 32.dp had to be kept in sync. `DetailCardDefaults.CornerRadius` is the only
// source of that radius now.
