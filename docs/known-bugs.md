# Known bugs

Open defects from a full code review (2026-10-05) and a full emulator test pass (2026-10-08/09: API 24 foss, API 37 gplay debug and release, `Resizable_Experimental`, `BRC_api36_magisk`, `BRC_api36_rootsim`; captures under `captures/claude/2026-10-0{8,9}_fulltest/`). Each entry names the code by function as well as by line, because line numbers drift. **Confirmed** means the code path was re-read and the failure follows from it. **Likely** means it rests on library internals read from bytecode or on device behavior not yet reproduced. Remove an entry when its fix lands; the numbers are stable IDs, so gaps are expected.

## High

### 19. Licences rows crash the app on a device with no browser (confirmed, lives in Android-Shared)

`com.iboalali.ui.licences`, `OssCredits.kt` `LibraryRow` (~line 149) calls Compose's `AndroidUriHandler.openUri` unguarded. With no `ACTION_VIEW` handler for `https`, tapping any Licences row throws `ActivityNotFoundException` and the app crashes (reproduced on `e2e_api37` with Chrome disabled). The fix belongs in Android-Shared (catch it around `openUri`, or route through a guarded helper), then this app adopts it by bumping `shared`. The About screen's "Other apps" rows from `com.iboalali.appcatalog:ui` likely open links the same way and were not tested.

### 20. Request Root access does nothing after a denied or timed-out prompt, until the process dies (confirmed)

`data/RootChecker.kt`, `requestRootRecorded()`.

libsu builds its main shell once per process and keeps it. When `su` is refused (Deny, or Magisk's 10 s timeout), the cached shell is a plain non-root shell, and `Shell.cmd("id").exec()` reuses it, so `su` is never asked again. The card offers "Request Root access", the spinner runs, and the same RootedNotGranted result returns with no prompt (zero `SuRequestActivity` launches). A force-stop makes the button work again.

`confirmPriorGrant` makes this easy to reach: a revoked grant on a device whose last check was ROOTED prompts once from the FAB, and if the user denies it, the button the card then offers is dead for the session. Reproduced twice on `BRC_api36_magisk` (Magisk 25.2), after a Deny and after a timeout (`m2_request_same_process.png`, `m2b_request_after_deny_same_process.png`).

Fix: in `requestRootRecorded`, when `Shell.isAppGrantedRoot() == false`, close the cached shell (`Shell.getCachedShell()?.close()`) before `Shell.cmd("id").exec()`, so a new `su` request is made.

### 21. Nothing on iboalali.com loads on Android 7.0 (confirmed)

The site's certificate chain ends at ISRG Root X1 (Let's Encrypt), which Android 7.0's trust store lacks; it only has the expired DST Root CA X3. On API 24:

- Chrome shows `NET::ERR_CERT_AUTHORITY_INVALID` for the privacy policy (Settings), the About Website link and the "Other apps" Website buttons.
- The app's own catalog fetch from `https://iboalali.com/apps` (OkHttp, `com.iboalali.appcatalog:data`) fails with `SSLHandshakeException`; `appCatalogRefresh` signals carry that error. The bundled catalog shows, so users don't see an error, but the list never updates.

Fix: a `network_security_config` that adds ISRG Root X1 as a trust anchor fixes the app's own requests (it does not help the browser). Browser links can only be fixed on the hosting side (a chain these devices trust) or by accepting the gap for Android 7.0. Screenshots: `api24/34_privacy_policy.png`, `api24/39_icon_recomposer_site.png`.

### 22. The language picker cannot scroll, so most languages are unreachable (confirmed)

`ui/settings/SettingsScreen.kt`, `LanguagePickerDialog`: the `Column(Modifier.selectableGroup())` (~line 565) has no `verticalScroll`.

On a phone in landscape at the default font, only System, English, Deutsch, Arabic and an empty radio fit; Español, Русский, Nederlands, 中文, Melayu and தமிழ் cannot be chosen. In portrait at font scale 2.0 the last rows collapse into label-less radios and one language is missing. Fix: make the list scrollable (`verticalScroll` on the column, or a `LazyColumn`). Screenshots: `api37/77_language_dialog_landscape_font100.png`, `api37/72_language_dialog_font200.png`.

## Medium

### 5. Double-tapping the FAB runs two checks at once (confirmed)

`ui/main/MainScreen.kt` FAB `onClick` (~line 312) and `MainViewModel.checkRoot()` (~line 126).

The FAB stays enabled while the status is `CHECKING`, and `checkRoot()` launches a new coroutine per tap with no in-flight guard. One real check increments `check_count` and `rooted_check_count` twice. Both coroutines can pass `ReviewGate.shouldRequest` before either writes `lastReviewPromptVersionCode`, so Play's review flow is requested twice. Haptics and analytics also fire twice.

Fix: ignore `checkRoot()`/`requestRoot()` while a check job is active (keep the `Job` and return if it is active), or disable the FAB during `CHECKING`.

### 6. A failed billing launch reports the error twice (likely)

`gplay/.../billing/GPlayBillingController.kt`, `launchPurchase()` (~line 237) and `purchasesUpdatedListener` (~line 110).

Play Billing 9.1.0 also posts a non-OK `launchBillingFlow` result to `PurchasesUpdatedListener` with `purchases = null` (read from `BillingClientImpl` bytecode, not reproduced on a device). So:

- Any launch failure sends `TipEvent.Error` and `trackTipFailed` twice: two snackbars, two failure signals.
- `ITEM_ALREADY_OWNED` runs the silent `reconcilePurchases()` recovery in `launchPurchase`, but the listener's `else` branch still shows an error and counts a failure.

Fix: handle launch failures in one place only (the listener), and treat `ITEM_ALREADY_OWNED` there as "reconcile, no error". Verify on a device by launching with the client disconnected.

### 7. The Settings tip jar can spin forever (confirmed)

`gplay/.../billing/GPlayBillingController.kt` (`isAvailable` ~line 60, product query ~line 188) and `ui/tip/TipJar.kt` (~line 67).

`isAvailable` is the constant `TIPPING_ENABLED`, so the Settings tip row always shows in `gplay`. If billing setup or `queryProductDetailsAsync` fails (`BILLING_UNAVAILABLE`, no Play services, no network at launch), the query is retried only on the next `onStart`, and the tip jar shows "Loading…" indefinitely. The support card avoids this with its `productsLoaded` check.

Fix: expose a load-failed state and show an error with retry in the tip jar, or retry the query when the tip jar opens.

Reproduced in the release build on API 37 (2026-10-08): logcat shows `BillingClient: Reconnection failed with result: 3` and `Max retries reached`, and the app fires `tipProductsUnavailable`, so it already knows the load failed while the dialog keeps showing "Loading…".

### 8. The tip jar opened from the support card is lost on rotation (confirmed)

`ui/main/MainScreen.kt` (~line 176): `showTipDialog` uses `remember`, not `rememberSaveable`.

Tapping "Support development" calls `onSupportPromptOpened`, which hides and snoozes the card immediately. A rotation, fold or resize then recreates the activity, the dialog disappears, and the main screen has no way back to it. `SettingsScreen` (~lines 150-153) has the same pattern for its dialogs, but there the entry row is still visible, so it is only an annoyance.

Fix: `rememberSaveable` for the dialog flags.

Confirmed on API 24 and API 37 (2026-10-08): rotating loses the tip dialog and the support card. The Settings reset dialog, the theme dialog (also lost on a live width change on a large screen) and the debug demo picker behave the same way.

### 9. Turning telemetry off mid-session does not stop the SDK (likely, lives in Android-Shared)

`analytics/Analytics.kt` `setEnabled()` → Android-Shared `telemetry/core` `TelemetryController.setEnabled()`.

Opting out closes the app's signal gate but never calls `TelemetryDeck.stop()`. The SDK keeps its lifecycle observer and broadcast timer, uploads signals already cached, and emits its own `TelemetryDeck.Session.started` when the app returns after more than 5 minutes in the background, until the process dies. Fix belongs in `TelemetryController`; this app then adopts it by bumping `shared`.

### 10. Benchmark and profile runs on physical devices count as production telemetry (confirmed mechanism)

`baselineprofile/build.gradle.kts` (`useConnectedDevices = true`) with `Analytics` (`debug = BuildConfig.DEBUG`).

`nonMinifiedRelease` and `benchmarkRelease` have `DEBUG = false`, and test-traffic detection matches only Test Lab, emulator markers and the monkey check. A run on the SM-G766B and SM-X356B sends `deviceType`, `appCatalogRefresh` and navigation signals as real users, many times per run. The tablet in landscape at 1280dp inflates exactly the expanded-width numbers used to size the large-screen audience. The `tabletApi36` GMD is correctly flagged.

Fix: flag instrumentation runs as test mode (e.g. detect the macrobenchmark instrumentation, or pass an instrumentation argument the app reads). Probably belongs in Android-Shared's test-traffic detection.

### 11. The Baseline Profile journey can never fail (confirmed)

`baselineprofile/.../Journeys.kt`: every `device.wait(...)` result is ignored or `?.`-chained.

If a testTag is renamed, or `testTagsAsResourceId` drops off the overflow popup (the trap in CLAUDE.md), `generate()` still passes and the profile silently loses every rule for Settings, About and Licences. `StartupBenchmarks`' scroll benchmark does fail, because `FrameTimingMetric` errors with no frames; the generator does not.

Fix: make each wait required (`checkNotNull(device.wait(Until.findObject(...), TIMEOUT)) { "…" }`, and check the boolean from `Until.hasObject`).

### 23. After process death the main screen is drawn over a restored detail (confirmed on API 37, cause unknown)

Repro on `e2e_api37` (phone width): open About or Settings, press Home, `am kill com.iboalali.basicrootchecker.debug`, relaunch. The main screen shows; Back then reveals About, and Back again shows main. The back stack is restored as `[Main, About]` but Main is drawn, and it stays that way after a few seconds and a tap. Rotation while on About is fine, and the same test on API 24 restored Settings correctly. `AppNavigation.kt` looks right; suspect Navigation 3 restoration together with `DetailOverlaySceneStrategy` (the overlay library in Android-Shared). Screenshots: `api37/67_after_process_death_amstart.png`, `api37/69_settings_process_death_8s.png`.

### 24. In RTL the overflow glyph fades in on the wrong side when a detail closes (confirmed, lives in Android-Shared)

`com.iboalali.nav3:overlay`, `DetailOverlayScene.kt` (~line 480): the morph icon is placed with `Modifier.offset { IntOffset(r.left, r.top) }`. Compose mirrors `offset` in RTL, but `overflowLocalRect` holds absolute coordinates, so in Arabic at 840dp and up the card collapses toward the real overflow icon on the left while the handoff glyph fades in at the top right, then the real icon pops in. Fix: `Modifier.absoluteOffset { }`. Billboard shares the library. Screenshots: `large/40_rtl_collapse_filmstrip_top.png`, `large/39_rtl_close_collapse_1..4.png`.

### 25. Swipe-down does not dismiss the overlay when the drag starts on the card's header (confirmed)

On all three detail screens at 840dp and up, a downward drag from the title or close-button area of the top app bar does nothing; the same drag from the list content works. Likely cause: the screens' `LargeTopAppBar` with `exitUntilCollapsedScrollBehavior` has its own vertical `draggable`, which consumes the drag before the overlay's `draggable` (`DetailOverlayScene.kt` ~line 465) sees it. The header is the most natural place to grab the card. Screenshots: `large/11_swipe_down_mid.png`, `large/13_header_drag_mid_settings.png`.

### 26. Users on Android 7 to 12 cannot choose the app language (confirmed)

`util/AppLanguage.kt`: `isSupported` requires API 33, so the Settings language row is hidden below it. The manifest still declares AppCompat's `AppLocalesMetadataHolderService` with `autoStoreLocales`, which only works with `AppCompatDelegate.setApplicationLocales`, and nothing calls that. Either support older versions through AppCompat (and keep the service) or drop the service entry.

## Low

### 12. Startup crashes are never reported (confirmed)

`BasicRootCheckerApplication.onCreate()` (~line 66). The comment says crashes during async init are "buffered by Analytics until the opt-out preference is resolved". The buffer is in memory, and the previous handler kills the process right away, so the preference is never resolved and the buffer never flushes. After the SDK has started, `TelemetryDeck.signal` writes its cache from an IO coroutine, which the dying process often never runs, so later crashes are often lost too (likely). Either make the comment true (persist synchronously before delegating) or correct the comment.

### 13. Crash reports send the raw exception message (confirmed)

`analytics/Analytics.kt` `trackError()` (~line 85) forwards `throwable.message` into `TelemetryDeck.Error.message` for uncaught crashes. Messages like `Index 7 out of bounds for length 3` or `Unable to start activity ComponentInfo{…}` are near-unique per event, against `telemetry-optimization.md`'s rule that the field stays low-cardinality and free of device-revealing strings. Also, in release the `simpleName` fallback id of an app exception class is R8-obfuscated.

### 14. Backup restores the last root check onto a new device (confirmed data flow)

`res/xml/data_extraction_rules.xml` and `backup_rules.xml` include all of `datastore/`, which holds `last_root_check_*` (`UserPreferences`). After a device transfer or cloud restore from a rooted phone to a stock one, `getLastRootCheck` answers "Rooted via Magisk" with the old phone's `checkedAt` until the first check. `check_count`, `rooted_check_count` and `pending_tip_tokens` travel too. Fix: move the last-check record to a separate, excluded DataStore file, or clear it on restore.

### 15. A pending tip that clears can be announced twice (likely, timing-dependent)

`gplay/.../billing/GPlayBillingController.kt` `announceIfLateClear()` (~line 290) reads and then clears the stored pending tokens in two non-atomic steps. `handleFreshPurchase` and `reconcilePurchases` each call it from their own `Dispatchers.IO` coroutine, so returning to the app can run both close together and show two "tip cleared" snackbars. Fix: do the read-and-remove inside one DataStore `edit {}` and announce only if this call removed the token.

### 16. The language picker shows nothing selected for a regional tag (confirmed)

`ui/settings/SettingsScreen.kt` (~line 579) with `util/AppLanguage.kt` (~line 41). `currentTag` returns the full `toLanguageTag()`, compared exactly against the bare tags in `SUPPORTED_TAGS`. If the system per-app language screen sets `de-AT` or `zh-Hans-CN`, no radio button is selected (the subtitle still reads correctly). Android's per-app language page (`APP_LOCALE_SETTINGS`) offers only regional entries such as "Deutsch (Österreich)" and "English (United States)", so any choice made there sets a regional tag (verified on API 37, 2026-10-08). Fix: match by best supported prefix.

### 17. Two IDE previews cannot render (confirmed)

`util/PlayStoreListingPreviews.kt` (~line 36) and `util/ConstrainedDevicePreviews.kt` (~line 94) call `AboutScreen(...)`, which builds `AboutViewModel` and casts the application to `BasicRootCheckerApplication`. Layoutlib has no such Application, so these previews throw. Fix: call `AboutScreenContent(...)`. The screenshot test run is not affected.

### 18. Script nits (confirmed)

- `scripts/generate-store-screenshots.sh` (~lines 42-49) resolves `script` before `cd` to the repo root, so a relative `AI_KIT_SCRIPTS` passes the `-f` check and then fails at `exec python3`. Resolve it to an absolute path first.
- `scripts/generate-iap-icons.sh` (~lines 142-151) sends Chrome's output to `/dev/null`; under `set -e` a Chrome failure exits with no message, leaving the icons already written modified. Print an error and restore on failure.


### 27. The "Checking for root…" snackbar outlives the check (confirmed)

`ui/main/MainScreen.kt` (~lines 327, 335, 516, 651) shows it with the default short duration (about 4 s), but a check takes about 1 s, so it sits over the result. Dismiss it when the result arrives, or don't show it while the status icon already animates. Seen on API 24 and API 37.

### 28. The main screen forgets its last result after process death (confirmed)

Rotation keeps the result, but after the process is killed and restored the card is back to the "Touch the # button" hint. `last_root_check_*` is persisted, but `MainViewModel` never reads it back. Consider restoring it (showing its time) on start.

### 29. The Android version row keeps the old language (confirmed)

`MainViewModel.loadDeviceInfo()` builds "Android <version>" from `R.string.textViewAndroidVersion` once, when the ViewModel is created, so after an in-app language switch the row keeps the previous language until the process restarts. Resolve the string in the UI instead. Screenshot: `api37/48_main_tamil.png`.

### 30. The update "Failed" card is a dead end (confirmed by reading)

The Failed card says the update "will retry automatically" but has no action, and `resolveUpdateEvent` keeps `Failed` when Play no longer offers the update, so the card stays for the session and blocks the support card. Either offer a retry or let a later `checkForUpdate` clear it. Screenshot: `api37/65_update_failed.png`.

### 31. The telemetry setting's description is wrong (confirmed)

It says the change "takes effect on next app launch", but turning it off and on again resumes signals in the same session. Update the copy (and see bug 9 for what "off" does not stop).

### 32. Copy and translation fixes (confirmed)

- `about_part1` (English, German): XML indentation after each `\n` puts a space at the start of every following line.
- Arabic: `textViewAndroidVersion` "اندرود" should be "أندرويد"; the disclaimer's "بظهر" should be "يظهر"; Licences is rendered as "حقوق" ("rights"); the Mastodon and Bluesky handles show "@" at the wrong end in RTL ("iboalali@mastodon.social@"), so they need LTR direction.
- German: `about_not_affiliated` is mistranslated; "app", "zugriff" and "gefahr" are lowercase where they must be capitalized (including the FAB description "Auf Root zugriff überprüfen"); the main hint "um den Root-Status überprüfen" is missing "zu".
- The main hint says the button is at the "bottom right" (English, Arabic); in RTL the FAB is at the bottom left. Make the wording position-free.
- The "(Debug)" app name exists only for English, German and Arabic, so the other six locales show the release name in debug builds (debug only).

### 33. Shared-library layout nits at large font and in license text (confirmed, lives in Android-Shared)

- `com.iboalali.appcatalog:ui`: at font scale 2.0 the "Website" button breaks mid-word ("Webs / ite") and its globe icon disappears (`api24/57_fs20_about_3.png`).
- `com.iboalali.ui:licences`: the expanded license text is hard-wrapped at about 80 characters, so it shows ragged lines on a phone (`api24/23_licences_mit_text.png`).
- `com.iboalali.appcatalog` feed: the German Hide Persistent Notification text mixes "Sie" and "du".

### 34. A back press right after opening a detail is ignored (confirmed, minor)

Pressing back within about 150 ms of tapping an overflow item leaves the overlay open; at 250 ms or later it works. Probably the closing dropdown `Popup` absorbs it. Users are unlikely to hit it.

### 35. Doc drift (confirmed)

- CLAUDE.md says the Licences screen shows "all seven shipped libraries"; it lists eight.
- `docs/root-provider-detection-gaps.md` could note that a stock API 24 image reports `RootedNotGranted(OTHER)` by default, because its `/system/xbin` is `drwxr-xr-x`, so `/system/xbin/su` is stat-able (the same state as the `chmod 755` recipe).
