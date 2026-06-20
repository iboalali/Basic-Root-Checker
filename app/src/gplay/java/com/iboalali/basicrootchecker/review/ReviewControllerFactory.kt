package com.iboalali.basicrootchecker.review

import android.content.Context

fun createReviewController(context: Context): ReviewController =
    GPlayReviewController(context.applicationContext)
