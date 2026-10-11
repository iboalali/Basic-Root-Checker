# Known bugs

Open defects from a full code review (2026-10-05) and a full emulator test pass (2026-10-08/09: API 24 foss, API 37 gplay debug and release, `Resizable_Experimental`, `BRC_api36_magisk`, `BRC_api36_rootsim`; captures under `captures/claude/2026-10-0{8,9}_fulltest/`). Each entry names the code by function as well as by line, because line numbers drift. **Confirmed** means the code path was re-read and the failure follows from it. **Likely** means it rests on library internals read from bytecode or on device behavior not yet reproduced. Remove an entry when its fix lands; the numbers are stable IDs, so gaps are expected.

## High

### 21. Nothing on iboalali.com loads on Android 7.0 (confirmed)

The site's certificate chain ends at ISRG Root X1 (Let's Encrypt), which Android 7.0's trust store lacks; it only has the expired DST Root CA X3. On API 24:

- Chrome shows `NET::ERR_CERT_AUTHORITY_INVALID` for the privacy policy (Settings), the About Website link and the "Other apps" Website buttons.
- The app's own catalog fetch from `https://iboalali.com/apps` (OkHttp, `com.iboalali.appcatalog:data`) fails with `SSLHandshakeException`; `appCatalogRefresh` signals carry that error. The bundled catalog shows, so users don't see an error, but the list never updates.

Fix: a `network_security_config` that adds ISRG Root X1 as a trust anchor fixes the app's own requests (it does not help the browser). Browser links can only be fixed on the hosting side (a chain these devices trust) or by accepting the gap for Android 7.0. Screenshots: `api24/34_privacy_policy.png`, `api24/39_icon_recomposer_site.png`.

## Medium

### 23. After process death the main screen is drawn over a restored detail (confirmed on API 37, cause unknown)

Repro on `e2e_api37` (phone width): open About or Settings, press Home, `am kill com.iboalali.basicrootchecker.debug`, relaunch. The main screen shows; Back then reveals About, and Back again shows main. The back stack is restored as `[Main, About]` but Main is drawn, and it stays that way after a few seconds and a tap. Rotation while on About is fine, and the same test on API 24 restored Settings correctly. `AppNavigation.kt` looks right; suspect Navigation 3 restoration together with `DetailOverlaySceneStrategy` (the overlay library in Android-Shared). Screenshots: `api37/67_after_process_death_amstart.png`, `api37/69_settings_process_death_8s.png`.

### 25. Swipe-down does not dismiss the overlay when the drag starts on the card's header (confirmed)

On all three detail screens at 840dp and up, a downward drag from the title or close-button area of the top app bar does nothing; the same drag from the list content works. Likely cause: the screens' `LargeTopAppBar` with `exitUntilCollapsedScrollBehavior` has its own vertical `draggable`, which consumes the drag before the overlay's `draggable` (`DetailOverlayScene.kt` ~line 465) sees it. The header is the most natural place to grab the card. Screenshots: `large/11_swipe_down_mid.png`, `large/13_header_drag_mid_settings.png`.

### 26. Users on Android 7 to 12 cannot choose the app language (confirmed)

`util/AppLanguage.kt`: `isSupported` requires API 33, so the Settings language row is hidden below it. The manifest still declares AppCompat's `AppLocalesMetadataHolderService` with `autoStoreLocales`, which only works with `AppCompatDelegate.setApplicationLocales`, and nothing calls that. Either support older versions through AppCompat (and keep the service) or drop the service entry.

## Low

### 34. A back press right after opening a detail is ignored (confirmed, minor)

Pressing back within about 150 ms of tapping an overflow item leaves the overlay open; at 250 ms or later it works. Probably the closing dropdown `Popup` absorbs it. Users are unlikely to hit it.

### 35. Doc drift (confirmed)

- The project instructions (`CLAUDE.md`, or `AGENTS.md`) say the Licences screen shows "all seven shipped libraries"; it lists eight.
