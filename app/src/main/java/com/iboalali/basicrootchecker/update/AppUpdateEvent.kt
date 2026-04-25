package com.iboalali.basicrootchecker.update

sealed class AppUpdateEvent {
    data object None : AppUpdateEvent()
    data object Available : AppUpdateEvent()
    data class Downloading(val bytesDownloaded: Long, val totalBytes: Long) : AppUpdateEvent()
    data object Downloaded : AppUpdateEvent()
    data class Failed(val errorCode: Int) : AppUpdateEvent()
}
