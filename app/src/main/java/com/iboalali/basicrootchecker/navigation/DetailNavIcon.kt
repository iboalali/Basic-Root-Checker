package com.iboalali.basicrootchecker.navigation

import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.runtime.Composable
import androidx.compose.runtime.compositionLocalOf
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp

/**
 * Which leading navigation icon a secondary screen (Settings / About / Licence) should draw,
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

/** Corner radius for a secondary screen shown as a dialog — matches the app's cards. */
private val DetailDialogCornerRadius = 32.dp

/**
 * Rounds a secondary screen's corners when it's presented as a dialog over the main screen
 * ([DetailNavIcon.CLOSE]) so it reads as a card like the app's other dialogs, since the built-in
 * `DialogSceneStrategy` renders the screen's `Scaffold` directly (which is otherwise square). No-op
 * when the screen is pushed full-screen.
 */
@Composable
fun Modifier.detailDialogShape(): Modifier =
    if (LocalDetailNavIcon.current == DetailNavIcon.CLOSE) {
        clip(RoundedCornerShape(DetailDialogCornerRadius))
    } else {
        this
    }
