package com.iboalali.basicrootchecker.update

import com.google.android.play.core.install.model.InstallStatus
import org.junit.Assert.assertEquals
import org.junit.Assert.assertSame
import org.junit.Test

class UpdateEventResolverTest {

    private val downloading = AppUpdateEvent.Downloading(3_000, 10_000)

    private fun resolve(
        current: AppUpdateEvent,
        installStatus: Int,
        offerable: Boolean = true,
        bytesDownloaded: Long = 0,
        totalBytes: Long = 0,
    ) = resolveUpdateEvent(current, installStatus, offerable, bytesDownloaded, totalBytes)

    @Test
    fun `finished download is offered for install even while downloading`() {
        assertEquals(AppUpdateEvent.Downloaded, resolve(downloading, InstallStatus.DOWNLOADED))
        assertEquals(
            AppUpdateEvent.Downloaded,
            resolve(AppUpdateEvent.None, InstallStatus.DOWNLOADED, offerable = false),
        )
    }

    @Test
    fun `download in progress keeps the listener's progress`() {
        assertSame(downloading, resolve(downloading, InstallStatus.DOWNLOADING, true, 1, 2))
        assertSame(downloading, resolve(downloading, InstallStatus.PENDING))
    }

    @Test
    fun `download in progress found in a fresh process uses the info's progress`() {
        assertEquals(
            AppUpdateEvent.Downloading(5, 50),
            resolve(AppUpdateEvent.None, InstallStatus.DOWNLOADING, offerable = true, 5, 50),
        )
    }

    @Test
    fun `downloading with no download behind it returns to the offer`() {
        for (status in listOf(
            InstallStatus.UNKNOWN,
            InstallStatus.CANCELED,
            InstallStatus.FAILED,
        )) {
            assertEquals(AppUpdateEvent.Available, resolve(downloading, status))
        }
    }

    @Test
    fun `downloading with no download and no offer clears the card`() {
        assertEquals(
            AppUpdateEvent.None,
            resolve(downloading, InstallStatus.CANCELED, offerable = false),
        )
    }

    @Test
    fun `offer appears and disappears with availability`() {
        assertEquals(AppUpdateEvent.Available, resolve(AppUpdateEvent.None, InstallStatus.UNKNOWN))
        assertEquals(
            AppUpdateEvent.None,
            resolve(AppUpdateEvent.Available, InstallStatus.UNKNOWN, offerable = false),
        )
    }

    @Test
    fun `failure stays visible when nothing is offered`() {
        val failed = AppUpdateEvent.Failed(-100)
        assertSame(failed, resolve(failed, InstallStatus.UNKNOWN, offerable = false))
        assertEquals(AppUpdateEvent.Available, resolve(failed, InstallStatus.UNKNOWN))
    }
}
