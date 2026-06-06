package com.iboalali.basicrootchecker.billing

import android.content.Context
import android.util.Log
import androidx.activity.ComponentActivity
import androidx.lifecycle.DefaultLifecycleObserver
import androidx.lifecycle.LifecycleOwner
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
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

private const val TAG = "GPlayBilling"

/**
 * Master switch for the tip jar. Temporarily `false` while we reconsider the
 * model (consumable tips can't be retrieved later to retroactively unlock
 * features). Flip to `true` to bring the tip jar — and the Settings "Support
 * development" row — back. All billing code below is retained and unused.
 */
private const val TIPPING_ENABLED = false

class GPlayBillingController(context: Context) : BillingController {

    override val isAvailable: Boolean = TIPPING_ENABLED

    private val _products = MutableStateFlow<List<TipProduct>>(emptyList())
    override val products: StateFlow<List<TipProduct>> = _products.asStateFlow()

    private val _purchaseState = MutableStateFlow(TipPurchaseState.Idle)
    override val purchaseState: StateFlow<TipPurchaseState> = _purchaseState.asStateFlow()

    private var activity: ComponentActivity? = null

    /** Raw Play product details, keyed by product id, for building the purchase flow. */
    private val detailsById = mutableMapOf<String, ProductDetails>()

    private val purchasesUpdatedListener =
        PurchasesUpdatedListener { result, purchases ->
            when (result.responseCode) {
                BillingClient.BillingResponseCode.OK -> {
                    purchases?.forEach { handlePurchase(it, fromUser = true) }
                }
                BillingClient.BillingResponseCode.USER_CANCELED -> {
                    _purchaseState.value = TipPurchaseState.Idle
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
        val productList = TIP_PRODUCT_IDS.map { id ->
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
                return@queryProductDetailsAsync
            }
            val details = queryResult.productDetailsList
            detailsById.clear()
            details.forEach { detailsById[it.productId] = it }

            _products.value = details
                .sortedBy { it.priceAmountMicros() }
                .mapNotNull { it.toTipProductOrNull() }
        }
    }

    override fun launchPurchase(productId: String) {
        val activity = this.activity ?: return
        val details = detailsById[productId] ?: return

        val productParams = BillingFlowParams.ProductDetailsParams.newBuilder()
            .setProductDetails(details)
            .build()
        val flowParams = BillingFlowParams.newBuilder()
            .setProductDetailsParamsList(listOf(productParams))
            .build()

        val result = billingClient.launchBillingFlow(activity, flowParams)
        if (result.responseCode == BillingClient.BillingResponseCode.OK) {
            _purchaseState.value = TipPurchaseState.Pending
        } else {
            Log.w(TAG, "launchBillingFlow failed: ${result.responseCode} ${result.debugMessage}")
            _purchaseState.value = TipPurchaseState.Error
            Analytics.trackTipFailed(formatBillingResult(result))
        }
    }

    override fun consumeThanks() {
        _purchaseState.compareAndSet(TipPurchaseState.Thanks, TipPurchaseState.Idle)
        _purchaseState.compareAndSet(TipPurchaseState.Error, TipPurchaseState.Idle)
    }

    /**
     * Consumes any tip purchases left over from a previous session (e.g. the app was
     * killed mid-flow) so the user is not blocked from tipping that tier again.
     */
    private fun reconcilePurchases() {
        val params = QueryPurchasesParams.newBuilder()
            .setProductType(ProductType.INAPP)
            .build()
        billingClient.queryPurchasesAsync(params) { result, purchases ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) return@queryPurchasesAsync
            purchases.forEach { handlePurchase(it, fromUser = false) }
        }
    }

    /**
     * Grants and consumes a tip purchase. Tips are consumable so they can be bought
     * again. [fromUser] distinguishes a fresh purchase (show thank-you) from a
     * leftover one reconciled on connect (consume silently).
     */
    private fun handlePurchase(purchase: Purchase, fromUser: Boolean) {
        if (purchase.purchaseState != Purchase.PurchaseState.PURCHASED) return

        if (fromUser) {
            purchase.products.forEach { Analytics.trackTipPurchased(it) }
            _purchaseState.value = TipPurchaseState.Thanks
        }

        val consumeParams = ConsumeParams.newBuilder()
            .setPurchaseToken(purchase.purchaseToken)
            .build()
        billingClient.consumeAsync(consumeParams) { result, _ ->
            if (result.responseCode != BillingClient.BillingResponseCode.OK) {
                Log.w(TAG, "consume failed: ${result.responseCode} ${result.debugMessage}")
            }
        }
    }

    private fun ProductDetails.toTipProductOrNull(): TipProduct? {
        val offer = oneTimePurchaseOfferDetails ?: return null
        return TipProduct(
            productId = productId,
            title = name.ifBlank { title },
            formattedPrice = offer.formattedPrice,
        )
    }

    private fun ProductDetails.priceAmountMicros(): Long =
        oneTimePurchaseOfferDetails?.priceAmountMicros ?: Long.MAX_VALUE

    private fun formatBillingResult(result: BillingResult): String =
        "${result.responseCode}"
}
