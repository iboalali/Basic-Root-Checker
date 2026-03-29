package com.iboalali.basicrootchecker.data

import com.topjohnwu.superuser.Shell
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.delay
import kotlinx.coroutines.withContext

object RootChecker {

    suspend fun checkRoot(): Boolean? = withContext(Dispatchers.IO) {
        val result = Shell.isAppGrantedRoot()
        delay(1000)
        result
    }
}
