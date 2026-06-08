package com.iboalali.basicrootchecker.billing

import androidx.activity.ComponentActivity
import kotlinx.collections.immutable.ImmutableList
import kotlinx.collections.immutable.ImmutableSet
import kotlinx.collections.immutable.persistentListOf
import kotlinx.collections.immutable.persistentSetOf
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.emptyFlow

object NoOpBillingController : BillingController {
    override val isAvailable: Boolean = false
    override val products: StateFlow<ImmutableList<TipProduct>> =
        MutableStateFlow<ImmutableList<TipProduct>>(persistentListOf()).asStateFlow()
    override val events: Flow<TipEvent> = emptyFlow()
    override val tipCleared: Flow<TipTier> = emptyFlow()
    override val supporterTiers: StateFlow<ImmutableSet<TipTier>> =
        MutableStateFlow<ImmutableSet<TipTier>>(persistentSetOf()).asStateFlow()

    override fun attach(activity: ComponentActivity) = Unit
    override fun launchPurchase(tier: TipTier) = Unit
}
