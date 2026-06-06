package com.iboalali.basicrootchecker.billing

import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.StateFlow

/**
 * Tip-jar billing facade. The real Google Play implementation lives in the `gplay`
 * flavor; the `foss` flavor supplies a no-op that reports [isAvailable] = false so
 * the tip jar disappears entirely from FOSS builds.
 */
interface BillingController {
    /** Whether tipping is supported in this build flavor (gplay = true, foss = false). */
    val isAvailable: Boolean

    /** Available tip offers, sorted cheapest first. Empty until Play details load. */
    val products: StateFlow<List<TipProduct>>

    val purchaseState: StateFlow<TipPurchaseState>

    fun attach(activity: ComponentActivity)

    fun launchPurchase(productId: String)

    /** Acknowledge that the thank-you UI has been shown, returning state to idle. */
    fun consumeThanks()
}
