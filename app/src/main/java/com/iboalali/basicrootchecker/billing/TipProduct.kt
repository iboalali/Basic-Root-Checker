package com.iboalali.basicrootchecker.billing

import androidx.annotation.StringRes
import com.iboalali.basicrootchecker.R

/**
 * A tip tier, backed by two Google Play one-time products priced identically:
 * a [recordProductId] we acknowledge and never consume (owned forever, so it stays in
 * `queryPurchasesAsync` as a durable client-side record of having tipped) and a
 * [repeatProductId] we always consume (so the tier can be tipped again). The record
 * variant is shown first; once owned, the tier transparently switches to the repeat
 * variant. Both look identical to the user — same [titleRes] label and same price.
 */
enum class TipTier(
    val recordProductId: String,
    val repeatProductId: String,
    @field:StringRes val titleRes: Int,
) {
    SMALL("tip_small", "tip_small_repeat", R.string.tip_tier_small),
    MEDIUM("tip_medium", "tip_medium_repeat", R.string.tip_tier_medium),
    LARGE("tip_large", "tip_large_repeat", R.string.tip_tier_large),
}

/** Which product to launch for a tier, given whether its record variant is already owned. */
fun TipTier.launchProductId(isRecordOwned: Boolean): String =
    if (isRecordOwned) repeatProductId else recordProductId

/** The tier a Play product id belongs to, or `null` if it isn't a tip product. */
fun tipTierForProductId(productId: String): TipTier? =
    TipTier.entries.firstOrNull {
        productId == it.recordProductId || productId == it.repeatProductId
    }

/** Whether [productId] is the durable (record) variant — the one we acknowledge and keep. */
fun isRecordProductId(productId: String): Boolean =
    TipTier.entries.any { productId == it.recordProductId }

/** Every product id across all tiers, for a single product-details query. */
val ALL_TIP_PRODUCT_IDS: List<String> =
    TipTier.entries.flatMap { listOf(it.recordProductId, it.repeatProductId) }

/** A tip offer projected for the UI: the tier plus its localized Play price. */
data class TipProduct(
    val tier: TipTier,
    /** Localized, currency-formatted price string supplied by Play (e.g. "$1.99"). */
    val formattedPrice: String,
)

/**
 * A one-shot outcome of the tip flow, surfaced to the user as transient UI (a snackbar).
 * Emitted as an event rather than held as state, so it fires exactly once at the moment it
 * happens and never re-triggers on recomposition or when the screen is re-observed.
 */
enum class TipEvent {
    /** A purchase the user just made completed — celebrate. */
    Thanks,

    /**
     * A purchase is awaiting approval and isn't complete yet (slow payment method, family
     * "Ask to Buy", etc.). It may clear later; when it does we grant it silently rather
     * than firing [Thanks] out of context.
     */
    Pending,

    /** The purchase flow failed. */
    Error,
}
