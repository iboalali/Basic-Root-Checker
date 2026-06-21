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
import com.iboalali.basicrootchecker.data.UserPreferences
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.PersistentSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.collections.immutable.toImmutableList
import kotlinx.collections.immutable.toPersistentSet
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.channels.Channel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.receiveAsFlow
import kotlinx.coroutines.launch

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

    // A Channel (not a SharedFlow) so an event emitted while the screen is briefly STOPPED
    // — e.g. behind the Play purchase sheet — is queued and delivered when the collector
    // resumes, rather than dropped. Each event is delivered to exactly one collector.
    private val _events = Channel<TipEvent>(Channel.BUFFERED)
    override val events: Flow<TipEvent> = _events.receiveAsFlow()

    // Late clears of previously-pending tips, surfaced app-wide. Buffered for the same
    // reason as _events: the signal can be emitted from reconcile on connect before the
    // app-root collector has resumed.
    private val _tipCleared = Channel<TipTier>(Channel.BUFFERED)
    override val tipCleared: Flow<TipTier> = _tipCleared.receiveAsFlow()

    // App-scoped: this controller is an Application singleton. Used for DataStore reads/
    // writes from the fire-and-forget billing callbacks.
    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.IO)
    private val prefs = UserPreferences(context)

    private val _supporterTiers = MutableStateFlow<PersistentSet<TipTier>>(persistentSetOf())
    override val supporterTiers: StateFlow<ImmutableSet<TipTier>> = _supporterTiers.asStateFlow()

    private var activity: ComponentActivity? = null

    /** The tier of the most recently launched flow, for attributing a later cancel. */
    private var lastLaunchedTier: TipTier? = null

    /**
     * True between launching a flow and its terminal result. Gates the [TipEvent.Thanks]
     * celebration to purchases that complete as part of the active flow: a purchase that
     * lands later (a pending one clearing, or an external/promo grant) is not in flight,
     * so it grants silently rather than firing a thank-you out of context.
     */
    private var purchaseInFlight = false

    /** Raw Play product details, keyed by product id, for building the purchase flow. */
    private val detailsById = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach { handleFreshPurchase(it) }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    purchaseInFlight = false
                    Analytics.trackTipCanceled(lastLaunchedTier?.name ?: "unknown")
                }
                else -> {
                    Log.w(TAG, "Purchase update failed: ${result.responseCode} ${result.debugMessage}")
                    purchaseInFlight = false
                    _events.trySend(TipEvent.Error)
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
        billingClient.startConnection(object : BillingClientStateListener {
            override fun onBillingSetupFinished(result: BillingResult) {
                if (result.responseCode == BillingClient.BillingResponseCode.OK) {
                    onConnected()
                } else {
                    Log.w(TAG, "Billing setup failed: ${result.responseCode} ${result.debugMessage}")
                    Analytics.trackBillingUnavailable(result.responseCode.toString())
                }
            }

            override fun onBillingServiceDisconnected() {
                // No-op: auto-reconnection is enabled.
            }
        })
    }

    private fun onConnected() {
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
                purchaseInFlight = true
            }
            BillingClient.BillingResponseCode.ITEM_ALREADY_OWNED -> {
                // Our owned set was stale (record already bought). Recover and let the
                // user retry against the repeat variant.
                reconcilePurchases()
            }
            else -> {
                Log.w(TAG, "launchBillingFlow failed: ${result.responseCode} ${result.debugMessage}")
                _events.trySend(TipEvent.Error)
                Analytics.trackTipFailed(formatBillingResult(result))
            }
        }
    }

    /** Handles a purchase delivered to the listener: grant it and surface the outcome. */
    private fun handleFreshPurchase(purchase: Purchase) {
        val productId = purchase.products.firstOrNull() ?: return
        val tier = tipTierForProductId(productId) ?: return

        when (purchase.purchaseState) {
            Purchase.PurchaseState.PENDING -> {
                // The active flow is over; it may complete later (and grant silently then).
                purchaseInFlight = false
                rememberPending(purchase)
                _events.trySend(TipEvent.Pending)
                Analytics.trackTipPending(productId, tier.name)
            }
            Purchase.PurchaseState.PURCHASED -> {
                grant(purchase, tier, productId)
                val variant = if (isRecordProductId(productId)) "record" else "repeat"
                Analytics.trackTipPurchased(productId, tier.name, variant)
                // Celebrate only if this completed the active flow. A deferred pending
                // purchase clearing later (or an external grant) is granted above but
                // doesn't fire Thanks, so no thank-you appears out of context.
                if (purchaseInFlight) {
                    purchaseInFlight = false
                    _events.trySend(TipEvent.Thanks)
                }
                // Independently, if this tip was one we'd seen pending, announce the clear
                // app-wide (covers a same-session pending->cleared, where Thanks is suppressed).
                announceIfLateClear(purchase, tier)
            }
            else -> Unit
        }
    }

    /** Persists a tip token seen pending, so a later clear can be recognized as a late clear. */
    private fun rememberPending(purchase: Purchase) {
        val token = purchase.purchaseToken
        scope.launch { prefs.addPendingTipToken(token) }
    }

    /**
     * If [purchase] is one we previously recorded as pending, announce the late clear
     * app-wide and forget the token, so repeated reconciles don't re-announce it.
     */
    private fun announceIfLateClear(purchase: Purchase, tier: TipTier) {
        val token = purchase.purchaseToken
        scope.launch {
            if (token in prefs.pendingTipTokens.first()) {
                _tipCleared.trySend(tier)
                prefs.clearPendingTipTokens(setOf(token))
            }
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
                val productId = purchase.products.firstOrNull() ?: return@forEach
                val tier = tipTierForProductId(productId) ?: return@forEach
                when (purchase.purchaseState) {
                    // A tip left pending (possibly bought outside the app while it was closed):
                    // remember it so we announce the clear on a later connect.
                    Purchase.PurchaseState.PENDING -> rememberPending(purchase)
                    Purchase.PurchaseState.PURCHASED -> {
                        if (isRecordProductId(productId)) owned += tier
                        grant(purchase, tier, productId)
                        announceIfLateClear(purchase, tier)
                    }
                    else -> Unit
                }
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
            _supporterTiers.value = _supporterTiers.value.adding(tier)
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
