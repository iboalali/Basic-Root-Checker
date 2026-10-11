package com.iboalali.basicrootchecker.ui.main

import androidx.lifecycle.SavedStateHandle
import com.iboalali.basicrootchecker.data.RootManager
import com.iboalali.basicrootchecker.data.RootProvider
import org.junit.Assert.assertEquals
import org.junit.Test

class SavedRootResultTest {

    private val deviceOnly = MainUiState(deviceMarketingName = "Pixel 8 Pro", androidVersionName = "16")

    @Test
    fun `a rooted result survives a round trip`() {
        val handle = SavedStateHandle()
        val shown =
            deviceOnly.copy(
                rootStatus = RootStatus.ROOTED,
                rootProvider = RootProvider.KERNELSU,
                rootManager = RootManager.SUKISU_ULTRA,
                rootProviderVersion = "12345",
            )

        SavedRootResult.save(handle, shown)

        assertEquals(shown, SavedRootResult.restore(handle, deviceOnly))
    }

    @Test
    fun `a later result without a provider clears the earlier one`() {
        val handle = SavedStateHandle()
        SavedRootResult.save(
            handle,
            deviceOnly.copy(
                rootStatus = RootStatus.ROOTED,
                rootProvider = RootProvider.MAGISK,
                rootManager = RootManager.MAGISK,
                rootProviderVersion = "27.0",
            ),
        )
        val notRooted = deviceOnly.copy(rootStatus = RootStatus.NOT_ROOTED)

        SavedRootResult.save(handle, notRooted)

        assertEquals(notRooted, SavedRootResult.restore(handle, deviceOnly))
    }

    @Test
    fun `nothing saved leaves the state alone`() {
        assertEquals(deviceOnly, SavedRootResult.restore(SavedStateHandle(), deviceOnly))
    }

    @Test
    fun `a check in flight is not restored, since nothing would finish it`() {
        val handle = SavedStateHandle()
        SavedRootResult.save(handle, deviceOnly.copy(rootStatus = RootStatus.CHECKING))

        assertEquals(deviceOnly, SavedRootResult.restore(handle, deviceOnly))
    }

    @Test
    fun `an unknown saved name is ignored`() {
        val handle = SavedStateHandle(mapOf("root_status" to "NO_SUCH_STATUS"))

        assertEquals(deviceOnly, SavedRootResult.restore(handle, deviceOnly))
    }
}
