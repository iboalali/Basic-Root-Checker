package com.iboalali.basicrootchecker.util

import androidx.compose.ui.tooling.preview.Preview

/**
 * Play Store listing multipreviews at **native device resolution** — pixel-based device specs so the
 * Compose Preview Screenshot tool emits **high-resolution** PNGs ready to upload (`output px == spec
 * px`; there is no scale knob). Each entry's `name` is free of special characters so the tool embeds
 * it in the reference filename — e.g. `MainRootedShot_Phone_en_<hash>_0.png` — making the PNGs
 * sortable by device/locale. Locales mirror the app's shipped set (`res/xml/app_locales_config.xml`):
 * en, de, ar, es, ru. Used by the `@PreviewTest` screens in the `screenshotTest` source set.
 *
 * Three matrices, one per Play Console slot, because the app's navigation is adaptive at the 840dp
 * width breakpoint (see `AppNavigation`):
 * - [PreviewPlayStorePhone] and [PreviewPlayStoreTablet7] are both **portrait and < 840dp wide**, so
 *   the secondary screens (Settings/About/Licence) render **single-pane / full-screen** (the 7-inch
 *   is just a wider canvas, not a stretched phone). Stack both on each single-pane `@PreviewTest`.
 * - [PreviewPlayStoreTablet10] is **landscape, 1280dp wide (≥840dp)**, where the secondary screens
 *   open as a **dialog card over the dimmed main screen** instead of replacing it — so its shots
 *   render that presentation (see `DialogOverMain` in the screenshot tests). The main screen itself
 *   stays single-pane at every width, so its shot uses all three matrices.
 *
 * A bare `@Preview(showBackground = true)` would render at wrap-content size — always use a px spec.
 */
// Phone — 411×914dp @420dpi, portrait, single-pane.
@Preview(name = "Phone_en", group = "Phone", showBackground = true, locale = "en", device = "spec:width=1080px,height=2400px,dpi=420")
@Preview(name = "Phone_de", group = "Phone", showBackground = true, locale = "de", device = "spec:width=1080px,height=2400px,dpi=420")
@Preview(name = "Phone_ar", group = "Phone", showBackground = true, locale = "ar", device = "spec:width=1080px,height=2400px,dpi=420")
@Preview(name = "Phone_es", group = "Phone", showBackground = true, locale = "es", device = "spec:width=1080px,height=2400px,dpi=420")
@Preview(name = "Phone_ru", group = "Phone", showBackground = true, locale = "ru", device = "spec:width=1080px,height=2400px,dpi=420")
annotation class PreviewPlayStorePhone

// 7-inch tablet — 800×1280dp @320dpi, portrait. 800dp is below the 840dp breakpoint, so the app
// (correctly) shows the single-pane layout here, just on a wider canvas. Stack alongside
// [PreviewPlayStorePhone] on each single-pane screen `@PreviewTest`.
@Preview(name = "Tablet7_en", group = "Tablet7", showBackground = true, locale = "en", device = "spec:width=1600px,height=2560px,dpi=320")
@Preview(name = "Tablet7_de", group = "Tablet7", showBackground = true, locale = "de", device = "spec:width=1600px,height=2560px,dpi=320")
@Preview(name = "Tablet7_ar", group = "Tablet7", showBackground = true, locale = "ar", device = "spec:width=1600px,height=2560px,dpi=320")
@Preview(name = "Tablet7_es", group = "Tablet7", showBackground = true, locale = "es", device = "spec:width=1600px,height=2560px,dpi=320")
@Preview(name = "Tablet7_ru", group = "Tablet7", showBackground = true, locale = "ru", device = "spec:width=1600px,height=2560px,dpi=320")
annotation class PreviewPlayStoreTablet7

// 10-inch tablet — 1280×800dp @320dpi, landscape. 1280dp is past the 840dp expanded-width
// breakpoint, so the secondary screens render as a dialog over the dimmed main screen, exactly as
// `AppNavigation` shows them on a large screen.
@Preview(name = "Tablet10_en", group = "Tablet10", showBackground = true, locale = "en", device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(name = "Tablet10_de", group = "Tablet10", showBackground = true, locale = "de", device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(name = "Tablet10_ar", group = "Tablet10", showBackground = true, locale = "ar", device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(name = "Tablet10_es", group = "Tablet10", showBackground = true, locale = "es", device = "spec:width=2560px,height=1600px,dpi=320")
@Preview(name = "Tablet10_ru", group = "Tablet10", showBackground = true, locale = "ru", device = "spec:width=2560px,height=1600px,dpi=320")
annotation class PreviewPlayStoreTablet10
