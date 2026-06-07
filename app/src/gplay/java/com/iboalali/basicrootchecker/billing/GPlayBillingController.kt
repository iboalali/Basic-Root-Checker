package com.iboalali.basicrootchecker.billing

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
import com.android.billingclient.api.AcknowledgePurchaseParams
import com.android.billingclient.api.BillingClient
import com.android.billingclient.api.BillingClient.ProductType
import com.android.billingclient.api.BillingClientStateListener
import com.android.billingclient.api.BillingFlowParams
import com.android.billingclient.api.BillingResult
import com.android.billingclient.api.ConsumeParams
import com.android.billingclient.api.PendingPurchasesParams
import com.android.billingclient.api.ProductDetails
import com.android.billingclient.api.Purchase
import com.android.billingclient.api.PurchasesUpdatedListener
import com.android.billingclient.api.QueryProductDetailsParams
import com.android.billingclient.api.QueryPurchasesParams
import com.iboalali.basicrootchecker.analytics.Analytics
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "GPlayBilling"

/**
 * Master switch for the tip jar. Flip to `false` to hide the Settings "Support
 * development" row and keep the billing client from ever connecting; all code below is
 * retained either way.
 */
private const val TIPPING_ENABLED = true

/**
 * Tip jar over Google Play Billing. Each tier has two products (see [TipTier]): the
 * record variant is acknowledged and never consumed, so it stays owned forever and acts
 * as a durable client-side record of having tipped; the repeat variant is always
 * consumed so the tier can be tipped again. [supporterTiers] is recomputed from owned
 * record products on every connect, so it survives reinstalls.
 */
class GPlayBillingController(context: Context) : BillingController {

    override val isAvailable: Boolean = TIPPING_ENABLED

    private val _products = MutableStateFlow<ImmutableList<TipProduct>>(persistentListOf())
    override val products: StateFlow<ImmutableList<TipProduct>> = _products.asStateFlow()

    private val _purchaseState = MutableStateFlow(TipPurchaseState.Idle)
    override val purchaseState: StateFlow<TipPurchaseState> = _purchaseState.asStateFlow()

    private val _supporterTiers = MutableStateFlow<PersistentSet<TipTier>>(persistentSetOf())
    override val supporterTiers: StateFlow<ImmutableSet<TipTier>> = _supporterTiers.asStateFlow()

    private var activity: ComponentActivity? = null

    /** The tier of the most recently launched flow, for attributing a later cancel. */
    private var lastLaunchedTier: TipTier? = null

    /** Raw Play product details, keyed by product id, for building the purchase flow. */
    private val detailsById = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach { handleFreshPurchase(it) }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    _purchaseState.value = TipPurchaseState.Idle
                    Analytics.trackTipCanceled(lastLaunchedTier?.name ?: "unknown")
                }
                else -> {
                    Log.w(TAG, "Purchase update failed: ${result.responseCode} ${result.debugMessage}")
                    _purchaseState.value = TipPurchaseState.Error
                    Analytics.trackTipFailed(formatBillingResult(result))
                }
            }
        }

    private val billingClient: BillingClient = BillingClient.newBuilder(context)
        .setListener(purchasesUpdatedListener)
        .enablePendingPurchases(
            PendingPurchasesParams.newBuilder().enableOneTimeProducts().build()
        )
        .enableAutoServiceReconnection()
        .build()

    private val lifecycleObserver = object : DefaultLifecycleObserver {
        override fun onStart(owner: LifecycleOwner) {
            connect()
        }

        override fun onDestroy(owner: LifecycleOwner) {
            detach()
        }
    }

    override fun attach(activity: ComponentActivity) {
        if (!isAvailable) return
        if (this.activity === activity) return
        if (this.activity != null) detach()

        this.activity = activity
        activity.lifecycle.addObserver(lifecycleObserver)
    }

    private fun detach() {
        activity?.lifecycle?.removeObserver(lifecycleObserver)
        activity = null
    }

    private fun connect() {
        if (billingClient.isReady) {
            onConnected()
            return
        }
        _purchaseState.compareAndSet(TipPurchaseState.Idle, TipPurchaseState.Connecting)
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                    _purchaseState.compareAndSet(TipPurchaseState.Connecting, TipPurchaseState.Idle)
                    Analytics.trackBillingUnavailable(result.responseCode.toString())
                }
            }

            override fun onBillingServiceDisconnected() {
                // No-op: auto-reconnection is enabled.
            }
        })
    }

    private fun onConnected() {
        _purchaseState.compareAndSet(TipPurchaseState.Connecting, TipPurchaseState.Idle)
        queryProductDetails()
        reconcilePurchases()
    }

    private fun queryProductDetails() {
        val productList = ALL_TIP_PRODUCT_IDS.map { id ->
            QueryProductDetailsParams.Product.newBuilder()
                .setProductId(id)
                .setProductType(ProductType.INAPP)
                .build()
        }
        val params = QueryProductDetailsParams.newBuilder()
            .setProductList(productList)
            .build()

        billingClient.queryProductDetailsAsync(params) { result, queryResult ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "queryProductDetails failed: ${result.responseCode} ${result.debugMessage}")
                Analytics.trackTipProductsUnavailable(result.responseCode.toString())
                return@queryProductDetailsAsync
            }
            detailsById.clear()
            queryResult.productDetailsList.forEach { detailsById[it.productId] = it }
            if (detailsById.isEmpty()) Analytics.trackTipProductsUnavailable("empty")
            rebuildProducts()
        }
    }

    /**
     * Emits one [TipProduct] per tier, using whichever variant we'd launch next (record
     * if not yet owned, otherwise repeat). Tiers whose chosen variant has no Play details
     * yet are skipped. Sorted cheapest first.
     */
    private fun rebuildProducts() {
        val owned = _supporterTiers.value
        _products.value = TipTier.entries
            .mapNotNull { tier ->
                val details = detailsById[tier.launchProductId(tier in owned)] ?: return@mapNotNull null
                val offer = details.oneTimePurchaseOfferDetails ?: return@mapNotNull null
                Triple(tier, offer.formattedPrice, offer.priceAmountMicros)
            }
            .sortedBy { it.third }
            .map { TipProduct(it.first, it.second) }
            .toImmutableList()
    }

    override fun launchPurchase(tier: TipTier) {
        val activity = this.activity ?: return
        val productId = tier.launchProductId(tier in _supporterTiers.value)
        val details = detailsById[productId] ?: return
        lastLaunchedTier = tier

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        when (result.responseCode) {
            BillingClient.BillingResponseCode.OK -> {
                _purchaseState.value = TipPurchaseState.Pending
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Our owned set was stale (record already bought). Recover and let the
                // user retry against the repeat variant.
                reconcilePurchases()
            }
            else -> {
                Log.w(TAG, "launchBillingFlow failed: ${result.responseCode} ${result.debugMessage}")
                _purchaseState.value = TipPurchaseState.Error
                Analytics.trackTipFailed(formatBillingResult(result))
            }
        }
    }

    override fun consumeThanks() {
        _purchaseState.compareAndSet(TipPurchaseState.Thanks, TipPurchaseState.Idle)
        _purchaseState.compareAndSet(TipPurchaseState.Error, TipPurchaseState.Idle)
    }

    /** Handles a purchase the user just completed: grant it and show the thank-you. */
    private fun handleFreshPurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: return
        val tier = tipTierForProductId(productId) ?: return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> {
                _purchaseState.value = TipPurchaseState.Pending
                Analytics.trackTipPending(productId, tier.name)
            }
            Purchase.PurchaseState.PURCHASED -> {
                grant(purchase, tier, productId)
                val variant = if (isRecordProductId(productId)) "record" else "repeat"
                Analytics.trackTipPurchased(productId, tier.name, variant)
                _purchaseState.value = TipPurchaseState.Thanks
            }
            else -> Unit
        }
    }

    /**
     * Recomputes [supporterTiers] from currently-owned purchases. Record products are
     * acknowledged and kept; leftover repeat products (e.g. the app died mid-flow) are
     * consumed. Runs on every connect, which is what restores supporter status after a
     * reinstall.
     */
    private fun reconcilePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            val owned = mutableSetOf<TipTier>()
            purchases.forEach { purchase ->
                if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return@forEach
                val productId = purchase.products.firstOrNull() ?: return@forEach
                val tier = tipTierForProductId(productId) ?: return@forEach
                if (isRecordProductId(productId)) owned += tier
                grant(purchase, tier, productId)
            }
            _supporterTiers.value = owned.toPersistentSet()
            rebuildProducts()
        }
    }

    /**
     * Applies the right post-purchase action for the product's role: record products are
     * acknowledged (kept owned forever); repeat products are consumed (repurchasable).
     */
    private fun grant(purchase: Purchase, tier: TipTier, productId: String) {
        if (isRecordProductId(productId)) {
            _supporterTiers.value = _supporterTiers.value.add(tier)
            if (!purchase.isAcknowledged) {
                val params = AcknowledgePurchaseParams.newBuilder()
                    .setPurchaseToken(purchase.purchaseToken)
                    .build()
                billingClient.acknowledgePurchase(params) { result ->
                    if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                        Log.w(TAG, "acknowledge failed: ${result.responseCode} ${result.debugMessage}")
                    }
                }
            }
            rebuildProducts()
        } else {
            val params = ConsumeParams.newBuilder()
                .setPurchaseToken(purchase.purchaseToken)
                .build()
            billingClient.consumeAsync(params) { result, _ ->
                if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                    Log.w(TAG, "consume failed: ${result.responseCode} ${result.debugMessage}")
                }
            }
        }
    }

    private fun formatBillingResult(result: BillingResult): String =
        "${result.responseCode}"
}
