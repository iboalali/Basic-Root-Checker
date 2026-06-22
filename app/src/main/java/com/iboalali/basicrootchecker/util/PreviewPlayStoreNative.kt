package com.iboalali.basicrootchecker.util

import androidx.compose.ui.tooling.preview.Preview

/**
 * Play Store listing multipreview at **native device resolution** — the companion to
 * [PreviewPlayStoreListing], but with pixel-based device specs so the Compose Preview Screenshot
 * tool emits **high-resolution** PNGs ready to upload. **3 devices × 5 locales = 15 images.**
 *
 * The screenshot tool sizes the output PNG purely from the device spec (`output px == spec px`);
 * there is no separate scale knob, so resolution is controlled here via `unit=px` + real density.
 * Each entry's `name` is free of special characters (no quotes) so the tool embeds it in the
 * reference filename — e.g. `MainRootedShot_Phone_en_<hash>_0.png` — making the PNGs sortable by
 * device/locale when uploading to the Play Console.
 *
 * Locales mirror the app's shipped set (`res/xml/app_locales_config.xml`): en, de, ar, es, ru.
 * Used by the `@PreviewTest` screens in the `screenshotTest` source set.
 */
// Phone
@Preview(name = "Phone_en", group = "Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2400px,dpi=420")
@Preview(name = "Phone_de", group = "Phone", showBackground = true, locale = "de", device = "spec:width=1080px,height=2400px,dpi=420")
@Preview(name = "Phone_ar", group = "Phone", showBackground = true, locale = "ar", device = "spec:width=1080px,height=2400px,dpi=420")
@Preview(name = "Phone_es", group = "Phone", showBackground = true, locale = "es", device = "spec:width=1080px,height=2400px,dpi=420")
@Preview(name = "Phone_ru", group = "Phone", showBackground = true, locale = "ru", device = "spec:width=1080px,height=2400px,dpi=420")
// 7-inch tablet (portrait)
@Preview(name = "Tablet7_en", group = "Tablet7", showBackground = true, locale = "en", device = "spec:width=1600px,height=2560px,dpi=320")
@Preview(name = "Tablet7_de", group = "Tablet7", showBackground = true, locale = "de", device = "spec:width=1600px,height=2560px,dpi=320")
@Preview(name = "Tablet7_ar", group = "Tablet7", showBackground = true, locale = "ar", device = "spec:width=1600px,height=2560px,dpi=320")
@Preview(name = "Tablet7_es", group = "Tablet7", showBackground = true, locale = "es", device = "spec:width=1600px,height=2560px,dpi=320")
@Preview(name = "Tablet7_ru", group = "Tablet7", showBackground = true, locale = "ru", device = "spec:width=1600px,height=2560px,dpi=320")
// 10-inch tablet (landscape)
@Preview(name = "Tablet10_en", group = "Tablet10", showBackground = true, locale = "en", device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(name = "Tablet10_de", group = "Tablet10", showBackground = true, locale = "de", device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(name = "Tablet10_ar", group = "Tablet10", showBackground = true, locale = "ar", device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(name = "Tablet10_es", group = "Tablet10", showBackground = true, locale = "es", device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(name = "Tablet10_ru", group = "Tablet10", showBackground = true, locale = "ru", device = "spec:width=2560px,height=1600px,dpi=320")
annotation class PreviewPlayStoreNative
