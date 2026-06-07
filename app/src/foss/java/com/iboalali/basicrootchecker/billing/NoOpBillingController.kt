package com.iboalali.basicrootchecker.billing

import androidx.activity.ComponentActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NoOpBillingController : BillingController {
    override val isAvailable: Boolean = false
    override val products: StateFlow<ImmutableList<TipProduct>> =
        MutableStateFlow<ImmutableList<TipProduct>>(persistentListOf()).asStateFlow()
    override val purchaseState: StateFlow<TipPurchaseState> =
        MutableStateFlow(TipPurchaseState.Idle).asStateFlow()
    override val supporterTiers: StateFlow<ImmutableSet<TipTier>> =
        MutableStateFlow<ImmutableSet<TipTier>>(persistentSetOf()).asStateFlow()

    override fun attach(activity: ComponentActivity) = Unit
    override fun launchPurchase(tier: TipTier) = Unit
    override fun consumeThanks() = Unit
}
