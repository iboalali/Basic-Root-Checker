package com.iboalali.basicrootchecker.ui.main

/**
 * Pure decision logic for when to fire the in-app review prompt. Kept separate from [MainViewModel]
 * and the Play controller so it can be unit-tested without Android.
 */
object ReviewGate {

    /** Minimum number of root-found results before the user is eligible to be asked. */
    const val MIN_ROOTED_CHECKS = 3

    /**
     * Whether to request the in-app review flow now.
     *
     * @param rootedCount how many checks have found root so far
     * @param lastPromptedVersion the version code the prompt last fired at (0 if never)
     * @param currentVersion the running app's version code
     *
     * Eligible once root has been confirmed [MIN_ROOTED_CHECKS] times and the prompt hasn't already
     * fired on this (or a later) version — capping it to roughly once per release, on top of Play's
     * own quota.
     */
    fun shouldRequest(rootedCount: Int, lastPromptedVersion: Int, currentVersion: Int): Boolean =
        rootedCount >= MIN_ROOTED_CHECKS && lastPromptedVersion < currentVersion
}
