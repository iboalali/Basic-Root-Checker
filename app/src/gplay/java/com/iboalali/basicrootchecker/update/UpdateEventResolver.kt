package com.iboalali.basicrootchecker.update

import com.google.android.play.core.install.model.InstallStatus

/**
 * The card state for a fresh `AppUpdateInfo`. [offerable] means Play has a flexible update that
 * is stale enough to offer.
 *
 * Only an install status that shows a download in progress keeps [current] on `Downloading`. A
 * download canceled or lost while the install listener was unregistered falls back to the offer,
 * so the card can never stay on an endless progress bar.
 *
 * A `Failed` card lasts until the next check: it turns back into the offer if Play still has the
 * update, so the user can try again, or clears if Play no longer offers it.
 */
internal fun resolveUpdateEvent(
    current: AppUpdateEvent,
    installStatus: Int,
    offerable: Boolean,
    bytesDownloaded: Long,
    totalBytes: Long,
): AppUpdateEvent = when (installStatus) {
    InstallStatus.DOWNLOADED -> AppUpdateEvent.Downloaded
    InstallStatus.DOWNLOADING, InstallStatus.PENDING ->
        current as? AppUpdateEvent.Downloading
            ?: AppUpdateEvent.Downloading(bytesDownloaded, totalBytes)
    else -> when {
        offerable -> AppUpdateEvent.Available
        current is AppUpdateEvent.Available ||
            current is AppUpdateEvent.Downloading ||
            current is AppUpdateEvent.Failed -> AppUpdateEvent.None
        else -> current
    }
}
