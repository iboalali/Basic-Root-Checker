package com.iboalali.basicrootchecker.ui.main

/**
 * Pure decision logic for when to offer the tip jar on the main screen after a root check. Sibling
 * of [ReviewGate]: kept out of [MainViewModel] and the billing layer so it can be unit-tested
 * without Android.
 *
 * The two post-check asks are deliberately **serialized, review first**. Play's review card is a
 * system-modal overlay that fires roughly once per release; this card recurs, so it yields:
 * - [MIN_CHECKS] sits above [ReviewGate.MIN_ROOTED_CHECKS]. On a device that keeps reporting root
 *   every check feeds both gates, so the review ask is always reached first. A device that never
 *   reports root can reach this card first, but it never becomes eligible for the review ask at
 *   all, so there is nothing there to yield to.
 * - `reviewRequestedThisSession` keeps them out of the *same session*. A frame-level check wouldn't
 *   be enough — Play's card covers the screen, so a support card rendered behind it would greet the
 *   user the moment they dismissed the review card, reading as a double-ask.
 */
object SupportGate {

    /**
     * Minimum number of root checks, whatever they found, before the user is offered the tip jar
     * here. Unlike [ReviewGate.MIN_ROOTED_CHECKS] this counts every check: someone who runs the app
     * repeatedly is a user worth asking, whether or not their device turns out to be rooted.
     */
    const val MIN_CHECKS = 5

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
     * @param checkCount how many root checks have run so far, whatever they found
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
        checkCount: Int,
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
            checkCount >= MIN_CHECKS &&
            dismissCount < MAX_DISMISSALS &&
            nowEpochMs >= snoozedUntilEpochMs

    /** The instant the card becomes eligible again, once dismissed or opened. */
    fun snoozeUntil(nowEpochMs: Long): Long = nowEpochMs + SNOOZE_MILLIS
}
