package com.iboalali.basicrootchecker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RootCheckerTest {

    private fun signals(
        granted: Boolean? = false,
        magiskPackage: Boolean = false,
        magiskMount: Boolean = false,
        magiskFiles: Boolean = false,
        magiskVersion: String? = null,
        kernelsu: Boolean = false,
        apatch: Boolean = false,
        suBinary: Boolean = false,
    ) = RootSignals(
        granted = granted,
        magiskPackageHit = magiskPackage,
        magiskMountHit = magiskMount,
        magiskFilesHit = magiskFiles,
        magiskVersion = magiskVersion,
        kernelsuPackageHit = kernelsu,
        apatchPackageHit = apatch,
        suBinaryHit = suBinary,
    )

    // --- granted = true ----------------------------------------------------

    @Test
    fun `granted with no signals reports Rooted with OTHER provider`() {
        assertEquals(
            RootResult.Rooted(RootProvider.OTHER, null),
            classify(signals(granted = true)),
        )
    }

    @Test
    fun `granted with magisk package reports Rooted with MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null),
            classify(signals(granted = true, magiskPackage = true)),
        )
    }

    @Test
    fun `granted with magisk mount reports Rooted with MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null),
            classify(signals(granted = true, magiskMount = true)),
        )
    }

    @Test
    fun `granted with magisk files reports Rooted with MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null),
            classify(signals(granted = true, magiskFiles = true)),
        )
    }

    @Test
    fun `granted with magisk version carries the version through`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, "27.0"),
            classify(signals(granted = true, magiskVersion = "27.0")),
        )
    }

    @Test
    fun `granted with kernelsu only reports Rooted with KERNELSU`() {
        assertEquals(
            RootResult.Rooted(RootProvider.KERNELSU, null),
            classify(signals(granted = true, kernelsu = true)),
        )
    }

    @Test
    fun `granted with apatch only reports Rooted with APATCH`() {
        assertEquals(
            RootResult.Rooted(RootProvider.APATCH, null),
            classify(signals(granted = true, apatch = true)),
        )
    }

    @Test
    fun `granted with magisk and kernelsu prefers MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null),
            classify(signals(granted = true, magiskPackage = true, kernelsu = true)),
        )
    }

    @Test
    fun `granted with magisk and apatch prefers MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null),
            classify(signals(granted = true, magiskMount = true, apatch = true)),
        )
    }

    @Test
    fun `granted with kernelsu and apatch prefers KERNELSU`() {
        assertEquals(
            RootResult.Rooted(RootProvider.KERNELSU, null),
            classify(signals(granted = true, kernelsu = true, apatch = true)),
        )
    }

    // --- granted = false ---------------------------------------------------

    @Test
    fun `denied with no signals reports NotRooted`() {
        assertEquals(
            RootResult.NotRooted,
            classify(signals(granted = false)),
        )
    }

    @Test
    fun `denied with magisk package reports RootedNotGranted with MAGISK`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK),
            classify(signals(granted = false, magiskPackage = true)),
        )
    }

    @Test
    fun `denied with magisk mount reports RootedNotGranted with MAGISK`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK),
            classify(signals(granted = false, magiskMount = true)),
        )
    }

    @Test
    fun `denied with kernelsu reports RootedNotGranted with KERNELSU`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.KERNELSU),
            classify(signals(granted = false, kernelsu = true)),
        )
    }

    @Test
    fun `denied with apatch reports RootedNotGranted with APATCH`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.APATCH),
            classify(signals(granted = false, apatch = true)),
        )
    }

    @Test
    fun `denied with su binary only reports RootedNotGranted with OTHER`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.OTHER),
            classify(signals(granted = false, suBinary = true)),
        )
    }

    @Test
    fun `denied with magisk and kernelsu both present prefers MAGISK`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK),
            classify(signals(granted = false, magiskPackage = true, kernelsu = true)),
        )
    }

    @Test
    fun `denied with kernelsu and su binary prefers KERNELSU over OTHER`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.KERNELSU),
            classify(signals(granted = false, kernelsu = true, suBinary = true)),
        )
    }

    @Test
    fun `denied ignores magisk files signal when not granted`() {
        // probeMagiskFiles requires root; when not granted it should never have run, but
        // verify classify does not treat a stray true value as evidence of root.
        assertEquals(
            RootResult.NotRooted,
            classify(signals(granted = false, magiskFiles = true)),
        )
    }

    // --- granted = null (unknown) ------------------------------------------

    @Test
    fun `unknown with no signals reports Unknown`() {
        assertEquals(
            RootResult.Unknown,
            classify(signals(granted = null)),
        )
    }

    @Test
    fun `unknown with magisk package reports RootedNotGranted with MAGISK`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK),
            classify(signals(granted = null, magiskPackage = true)),
        )
    }

    @Test
    fun `unknown with su binary reports RootedNotGranted with OTHER`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.OTHER),
            classify(signals(granted = null, suBinary = true)),
        )
    }

    @Test
    fun `unknown with apatch reports RootedNotGranted with APATCH`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.APATCH),
            classify(signals(granted = null, apatch = true)),
        )
    }

    // --- parseMagiskVersionCode --------------------------------------------

    @Test
    fun `parseMagiskVersionCode handles modern release codes`() {
        assertEquals("27.0", parseMagiskVersionCode(27000))
        assertEquals("28.0", parseMagiskVersionCode(28000))
        assertEquals("26.4", parseMagiskVersionCode(26400))
        assertEquals("26.1", parseMagiskVersionCode(26100))
        assertEquals("25.2", parseMagiskVersionCode(25208))
    }

    @Test
    fun `parseMagiskVersionCode handles patch suffix codes`() {
        // Magisk packs minor as (code % 1000) / 100; the trailing patch digits are dropped.
        assertEquals("27.0", parseMagiskVersionCode(27006))
        assertEquals("28.1", parseMagiskVersionCode(28102))
        assertEquals("26.3", parseMagiskVersionCode(26301))
    }
}
