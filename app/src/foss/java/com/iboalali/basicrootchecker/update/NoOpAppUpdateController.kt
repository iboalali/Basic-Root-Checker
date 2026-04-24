package com.iboalali.basicrootchecker.update

import androidx.activity.ComponentActivity
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

object NoOpAppUpdateController : AppUpdateController {
    override val events: StateFlow<AppUpdateEvent> =
        MutableStateFlow<AppUpdateEvent>(AppUpdateEvent.None).asStateFlow()

    override fun attach(activity: ComponentActivity) = Unit
    override fun checkForUpdate() = Unit
    override fun startFlexibleFlow() = Unit
    override fun completeUpdate() = Unit
}
