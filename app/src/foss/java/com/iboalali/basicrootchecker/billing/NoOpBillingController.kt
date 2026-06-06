package com.iboalali.basicrootchecker.billing

import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NoOpBillingController : BillingController {
    override val isAvailable: Boolean = false
    override val products: StateFlow<List<TipProduct>> =
        MutableStateFlow<List<TipProduct>>(emptyList()).asStateFlow()
    override val purchaseState: StateFlow<TipPurchaseState> =
        MutableStateFlow(TipPurchaseState.Idle).asStateFlow()

    override fun attach(activity: ComponentActivity) = Unit
    override fun launchPurchase(productId: String) = Unit
    override fun consumeThanks() = Unit
}
