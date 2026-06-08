package com.iboalali.basicrootchecker.billing

import android.content.Context

fun createBillingController(context: Context): BillingController =
    GPlayBillingController(context.applicationContext)
