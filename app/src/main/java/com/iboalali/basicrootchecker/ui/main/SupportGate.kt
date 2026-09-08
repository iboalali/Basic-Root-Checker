package com.iboalali.basicrootchecker.ui.main

/**
 * Pure decision logic for when to offer the tip jar on the main screen after a root check. Sibling
 * of [ReviewGate]: kept out of [MainViewModel] and the billing layer so it can be unit-tested
 * without Android.
 *
 * The two post-check asks are deliberately **serialized, review first**. Play's review card is a
 * system-modal overlay that fires roughly once per release; this card recurs, so it yields:
 * - [MIN_ROOTED_CHECKS] sits above [ReviewGate.MIN_ROOTED_CHECKS], so a fresh install always
 *   reaches the review ask first.
 * - `reviewRequestedThisSession` keeps them out of the *same session*. A frame-level check wouldn't
 *   be enough — Play's card covers the screen, so a support card rendered behind it would greet the
 *   user the moment they dismissed the review card, reading as a double-ask.
 */
object SupportGate {

    /** Minimum number of root-found results before the user is offered the tip jar here. */
    const val MIN_ROOTED_CHECKS = 5

    /** After this many dismissals the card never returns — a dismissal is an answer. */
    const val MAX_DISMISSALS = 3

    /** How long the card steps aside after being dismissed or opened. */
    const val SNOOZE_MILLIS = 30L * 24 * 60 * 60 * 1000

    /**
     * Whether to show the support card now.
     *
     * @param billingAvailable tipping is supported in this build (Google Play only)
     * @param productsLoaded Play has returned tip prices — without them the card would lead to a
     *   dead loading spinner
     * @param alreadySupporter the user has tipped before; never ask again
     * @param rootedCount how many checks have found root so far
     * @param dismissCount how many times the card has been dismissed
     * @param snoozedUntilEpochMs when the card becomes eligible again (0 if never snoozed)
     * @param nowEpochMs current wall-clock time
     * @param reviewRequestedThisSession the Play review flow was requested in this process
     * @param updatePending an app update is offered/downloading — that card is functional and
     *   time-sensitive, so it owns the slot and the ask waits for the next check
     */
    fun shouldShow(
        billingAvailable: Boolean,
        productsLoaded: Boolean,
        alreadySupporter: Boolean,
        rootedCount: Int,
        dismissCount: Int,
        snoozedUntilEpochMs: Long,
        nowEpochMs: Long,
        reviewRequestedThisSession: Boolean,
        updatePending: Boolean,
    ): Boolean =
        billingAvailable &&
            productsLoaded &&
            !alreadySupporter &&
            !reviewRequestedThisSession &&
            !updatePending &&
            rootedCount >= MIN_ROOTED_CHECKS &&
            dismissCount < MAX_DISMISSALS &&
            nowEpochMs >= snoozedUntilEpochMs

    /** The instant the card becomes eligible again, once dismissed or opened. */
    fun snoozeUntil(nowEpochMs: Long): Long = nowEpochMs + SNOOZE_MILLIS
}
