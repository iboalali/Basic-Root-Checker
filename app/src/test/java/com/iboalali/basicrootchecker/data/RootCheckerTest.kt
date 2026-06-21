package com.iboalali.basicrootchecker.data

import org.junit.Assert.assertEquals
import org.junit.Test

class RootCheckerTest {

    private fun signals(
        granted: Boolean? = false,
        packageManager: RootManager? = null,
        magiskMount: Boolean = false,
        magiskFiles: Boolean = false,
        magiskPath: Boolean = false,
        magiskVersion: String? = null,
        suBinary: Boolean = false,
    ) = RootSignals(
        granted = granted,
        packageManager = packageManager,
        magiskMountHit = magiskMount,
        magiskFilesHit = magiskFiles,
        magiskPathHit = magiskPath,
        magiskVersion = magiskVersion,
        suBinaryHit = suBinary,
    )

    // --- granted = true ----------------------------------------------------

    @Test
    fun `granted with no signals reports Rooted with OTHER provider`() {
        assertEquals(
            RootResult.Rooted(RootProvider.OTHER, null, null),
            classify(signals(granted = true)),
        )
    }

    @Test
    fun `granted with magisk package reports Rooted with MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, RootManager.MAGISK, null),
            classify(signals(granted = true, packageManager = RootManager.MAGISK)),
        )
    }

    @Test
    fun `granted with kitsune package reports Rooted with MAGISK family and KITSUNE_MASK manager`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, RootManager.KITSUNE_MASK, null),
            classify(signals(granted = true, packageManager = RootManager.KITSUNE_MASK)),
        )
    }

    @Test
    fun `granted with magisk mount reports Rooted with MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null, null),
            classify(signals(granted = true, magiskMount = true)),
        )
    }

    @Test
    fun `granted with magisk files reports Rooted with MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null, null),
            classify(signals(granted = true, magiskFiles = true)),
        )
    }

    @Test
    fun `granted with magisk path reports Rooted with MAGISK`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null, null),
            classify(signals(granted = true, magiskPath = true)),
        )
    }

    @Test
    fun `granted with magisk version carries the version through`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null, "27.0"),
            classify(signals(granted = true, magiskVersion = "27.0")),
        )
    }

    @Test
    fun `granted with kitsune package and magisk version keeps the manager and version`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, RootManager.KITSUNE_MASK, "27.0"),
            classify(signals(granted = true, packageManager = RootManager.KITSUNE_MASK, magiskVersion = "27.0")),
        )
    }

    @Test
    fun `granted with kernelsu only reports Rooted with KERNELSU`() {
        assertEquals(
            RootResult.Rooted(RootProvider.KERNELSU, RootManager.KERNELSU, null),
            classify(signals(granted = true, packageManager = RootManager.KERNELSU)),
        )
    }

    @Test
    fun `granted with sukisu package reports Rooted with KERNELSU family and SUKISU_ULTRA manager`() {
        assertEquals(
            RootResult.Rooted(RootProvider.KERNELSU, RootManager.SUKISU_ULTRA, null),
            classify(signals(granted = true, packageManager = RootManager.SUKISU_ULTRA)),
        )
    }

    @Test
    fun `granted with apatch only reports Rooted with APATCH`() {
        assertEquals(
            RootResult.Rooted(RootProvider.APATCH, RootManager.APATCH, null),
            classify(signals(granted = true, packageManager = RootManager.APATCH)),
        )
    }

    @Test
    fun `granted with legacy package reports Rooted with OTHER family and named manager`() {
        assertEquals(
            RootResult.Rooted(RootProvider.OTHER, RootManager.SUPERSU, null),
            classify(signals(granted = true, packageManager = RootManager.SUPERSU)),
        )
    }

    @Test
    fun `granted with magisk signal beats a kernelsu package and drops the mismatched manager`() {
        // A magisk mount/path can force MAGISK even when the installed manager app is KernelSU; we
        // must not then label that KernelSU app as the Magisk we detected.
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null, null),
            classify(signals(granted = true, packageManager = RootManager.KERNELSU, magiskMount = true)),
        )
    }

    @Test
    fun `granted with magisk signal beats an apatch package`() {
        assertEquals(
            RootResult.Rooted(RootProvider.MAGISK, null, null),
            classify(signals(granted = true, packageManager = RootManager.APATCH, magiskPath = true)),
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
            RootResult.RootedNotGranted(RootProvider.MAGISK, RootManager.MAGISK),
            classify(signals(granted = false, packageManager = RootManager.MAGISK)),
        )
    }

    @Test
    fun `denied with kitsune package reports RootedNotGranted MAGISK family with KITSUNE_MASK manager`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK, RootManager.KITSUNE_MASK),
            classify(signals(granted = false, packageManager = RootManager.KITSUNE_MASK)),
        )
    }

    @Test
    fun `denied with magisk mount reports RootedNotGranted with MAGISK`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK, null),
            classify(signals(granted = false, magiskMount = true)),
        )
    }

    @Test
    fun `denied with magisk path reports RootedNotGranted with MAGISK`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK, null),
            classify(signals(granted = false, magiskPath = true)),
        )
    }

    @Test
    fun `denied with kernelsu reports RootedNotGranted with KERNELSU`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.KERNELSU, RootManager.KERNELSU),
            classify(signals(granted = false, packageManager = RootManager.KERNELSU)),
        )
    }

    @Test
    fun `denied with sukisu reports RootedNotGranted KERNELSU family with SUKISU_ULTRA manager`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.KERNELSU, RootManager.SUKISU_ULTRA),
            classify(signals(granted = false, packageManager = RootManager.SUKISU_ULTRA)),
        )
    }

    @Test
    fun `denied with resukisu reports RootedNotGranted KERNELSU family with RESUKISU manager`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.KERNELSU, RootManager.RESUKISU),
            classify(signals(granted = false, packageManager = RootManager.RESUKISU)),
        )
    }

    @Test
    fun `denied with apatch reports RootedNotGranted with APATCH`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.APATCH, RootManager.APATCH),
            classify(signals(granted = false, packageManager = RootManager.APATCH)),
        )
    }

    @Test
    fun `denied with su binary only reports RootedNotGranted with OTHER`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.OTHER, null),
            classify(signals(granted = false, suBinary = true)),
        )
    }

    @Test
    fun `denied with legacy package reports RootedNotGranted with OTHER family and named manager`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.OTHER, RootManager.SUPERSU),
            classify(signals(granted = false, packageManager = RootManager.SUPERSU)),
        )
    }

    @Test
    fun `denied with magisk signal and kernelsu package prefers MAGISK and drops mismatched manager`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK, null),
            classify(signals(granted = false, packageManager = RootManager.KERNELSU, magiskMount = true)),
        )
    }

    @Test
    fun `denied with kernelsu package and su binary prefers KERNELSU over OTHER`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.KERNELSU, RootManager.KERNELSU),
            classify(signals(granted = false, packageManager = RootManager.KERNELSU, suBinary = true)),
        )
    }

    @Test
    fun `denied with legacy package and magisk path prefers MAGISK over OTHER`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK, null),
            classify(signals(granted = false, packageManager = RootManager.SUPERSU, magiskPath = true)),
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
            RootResult.RootedNotGranted(RootProvider.MAGISK, RootManager.MAGISK),
            classify(signals(granted = null, packageManager = RootManager.MAGISK)),
        )
    }

    @Test
    fun `unknown with su binary reports RootedNotGranted with OTHER`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.OTHER, null),
            classify(signals(granted = null, suBinary = true)),
        )
    }

    @Test
    fun `unknown with apatch reports RootedNotGranted with APATCH`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.APATCH, RootManager.APATCH),
            classify(signals(granted = null, packageManager = RootManager.APATCH)),
        )
    }

    @Test
    fun `unknown with magisk path reports RootedNotGranted with MAGISK`() {
        assertEquals(
            RootResult.RootedNotGranted(RootProvider.MAGISK, null),
            classify(signals(granted = null, magiskPath = true)),
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
