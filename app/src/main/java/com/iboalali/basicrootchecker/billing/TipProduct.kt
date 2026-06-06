package com.iboalali.basicrootchecker.billing

/**
 * A single tip-jar offer, projected from a Google Play one-time product into the
 * flavor-agnostic shape the UI consumes.
 */
data class TipProduct(
    val productId: String,
    val title: String,
    /** Localized, currency-formatted price string supplied by Play (e.g. "$1.99"). */
    val formattedPrice: String,
)

/** Where the tip flow currently is, for driving Settings UI. */
enum class TipPurchaseState {
    Idle,
    Connecting,
    Pending,
    Thanks,
    Error,
}

/** Product IDs of the consumable tip-jar offers configured in Google Play Console. */
val TIP_PRODUCT_IDS = listOf("tip_small", "tip_medium", "tip_large")
