package com.iboalali.basicrootchecker.ui

import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.WindowInsets
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.safeDrawing
import androidx.compose.foundation.layout.windowInsetsPadding
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.lifecycle.Lifecycle
import androidx.lifecycle.compose.LocalLifecycleOwner
import androidx.lifecycle.repeatOnLifecycle
import com.iboalali.basicrootchecker.R
import com.iboalali.basicrootchecker.billing.TipTier
import com.iboalali.basicrootchecker.navigation.AppNavigation
import kotlinx.coroutines.flow.Flow

/**
 * App root: hosts [AppNavigation] and overlays a single app-wide [SnackbarHost] so a
 * late-cleared tip can be announced over whatever screen is currently showing. Each screen
 * still owns its own Scaffold/snackbars; this is a thin overlay (a [Box], not a nested
 * Scaffold) reserved for signals that aren't tied to any one screen.
 */
@Composable
fun AppRoot(tipCleared: Flow<TipTier>) {
    val snackbarHostState = remember { SnackbarHostState() }
    val tipClearedMessage = stringResource(R.string.tip_jar_cleared)
    val lifecycleOwner = LocalLifecycleOwner.current
    LaunchedEffect(tipCleared, lifecycleOwner) {
        lifecycleOwner.repeatOnLifecycle(Lifecycle.State.STARTED) {
            tipCleared.collect { snackbarHostState.showSnackbar(tipClearedMessage) }
        }
    }

    Box(modifier = Modifier.fillMaxSize()) {
        AppNavigation()
        SnackbarHost(
            hostState = snackbarHostState,
            modifier = Modifier
                .align(Alignment.BottomCenter)
                .windowInsetsPadding(WindowInsets.safeDrawing),
        )
    }
}
