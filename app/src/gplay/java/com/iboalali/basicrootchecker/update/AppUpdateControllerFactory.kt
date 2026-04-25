package com.iboalali.basicrootchecker.update

import android.content.Context

fun createAppUpdateController(context: Context): AppUpdateController =
    GPlayAppUpdateController(context.applicationContext)
