package com.iboalali.basicrootchecker.billing

import androidx.activity.ComponentActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.StateFlow

/**
 * Tip-jar billing facade. The real Google Play implementation lives in the `gplay`
 * flavor; the `foss` flavor supplies a no-op that reports [isAvailable] = false so
 * the tip jar disappears entirely from FOSS builds.
 */
interface BillingController {
    /** Whether tipping is supported in this build flavor (gplay = true, foss = false). */
    val isAvailable: Boolean

    /** Available tip offers, one per tier, sorted cheapest first. Empty until Play details load. */
    val products: StateFlow<ImmutableList<TipProduct>>

    /**
     * One-shot tip outcomes (thanks / pending / error) to surface as transient UI. A
     * deferred purchase that completes later — and any purchase restored on connect — is
     * reconciled silently and does NOT emit here, so the user never sees an out-of-context
     * thank-you.
     */
    val events: Flow<TipEvent>

    /**
     * Tiers whose durable record product is currently owned — a permanent, on-device
     * record of which tips the user has ever given. Future features can gate on this
     * (`isSupporter = supporterTiers.value.isNotEmpty()`).
     */
    val supporterTiers: StateFlow<ImmutableSet<TipTier>>

    fun attach(activity: ComponentActivity)

    fun launchPurchase(tier: TipTier)
}
