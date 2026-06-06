package com.iboalali.basicrootchecker.billing

import android.content.Context

@Suppress("UNUSED_PARAMETER")
fun createBillingController(context: Context): BillingController = NoOpBillingController
