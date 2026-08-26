package com.iboalali.basicrootchecker.ui.main

import android.content.Intent

/**
 * Debug-only: the device name and model shown on the main screen, armed from outside the app.
 *
 * The sibling of [DemoRootOverride], for the other half of what the store videos put on camera. The
 * status area answers "are you rooted"; the card under it says which phone was asked, and on an
 * emulator that card reads "emulator" and "emu64xa", because `DeviceMarketingNames` has no entry
 * for `emu64xa` and `Build.DEVICE` is the emulator's own codename.
 *
 * That is wrong for a recording rather than merely ugly. The finished frame composites the capture
 * into a Pixel 9 Pro XL bezel, so a card naming the emulator contradicts the handset the viewer can
 * see, and it does not match the promo, which was shot on the real hardware.
 *
 *     adb shell am start -n com.iboalali.basicrootchecker.debug/com.iboalali.basicrootchecker.MainActivity \
 *       --es demo_device_name "Pixel 9 Pro XL" --es demo_device_model komodo
 *
 * Read once in `MainActivity.onCreate`, like [DemoRootOverride]: each cold start decides, launching
 * without the extras disarms, and there is no state to reset between runs. Both call sites are
 * behind `BuildConfig.DEBUG`, so none of this is reachable in release.
 */
object DemoDeviceOverride {

    const val EXTRA_NAME: String = "demo_device_name"
    const val EXTRA_MODEL: String = "demo_device_model"

    @Volatile
    var name: String? = null
        private set

    @Volatile
    var model: String? = null
        private set

    /**
     * Whether this launch is a recording.
     *
     * Nothing but the store-video recorder arms a demo device, so debug-only controls that would
     * otherwise sit in shot consult this and hide themselves. Filming needs a debug build, and
     * neutralizing `src/debug/res` does not touch the controls the debug *code* adds: Settings'
     * "Debug: record products" card would otherwise sit at the bottom of a filmed settings list.
     */
    val recording: Boolean
        get() = name != null

    /**
     * Arm (or disarm) from a launch intent. An absent or blank extra leaves the real value alone.
     */
    fun applyFrom(intent: Intent?) {
        name = intent?.getStringExtra(EXTRA_NAME)?.trim()?.takeIf { it.isNotEmpty() }
        model = intent?.getStringExtra(EXTRA_MODEL)?.trim()?.takeIf { it.isNotEmpty() }
    }
}
