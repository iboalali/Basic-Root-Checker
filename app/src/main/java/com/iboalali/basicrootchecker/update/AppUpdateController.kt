package com.iboalali.basicrootchecker.update

import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.StateFlow

interface AppUpdateController {
    val events: StateFlow<AppUpdateEvent>

    fun attach(activity: ComponentActivity)

    fun checkForUpdate()

    fun startFlexibleFlow()

    fun completeUpdate()
}
