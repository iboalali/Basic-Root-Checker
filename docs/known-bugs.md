# Known bugs

Open defects from a full code review (2026-10-05). Each entry names the code by function as well as by line, because line numbers drift. **Confirmed** means the code path was re-read and the failure follows from it. **Likely** means it rests on library internals read from bytecode or on device behavior not yet reproduced. Remove an entry when its fix lands; the numbers are stable IDs, so gaps are expected.

## High

### 3. The in-app update card gets stuck on "Downloading" (confirmed)

`gplay/.../update/GPlayAppUpdateController.kt`, `startFlexibleFlow()` and `checkForUpdate()`.

- `startFlexibleFlow()` sets `Downloading(0, 0)` as soon as Play's dialog launches. If the user declines (`RESULT_CANCELED` / `RESULT_IN_APP_UPDATE_FAILED`), the activity-result callback only logs, and the next `checkForUpdate()` returns early on `Downloading`. The card shows an endless progress bar. `InstallStatus.CANCELED` from the install listener is also ignored.
- `checkForUpdate()` returns early on `Downloading` before it checks `installStatus() == DOWNLOADED`. The install listener is unregistered in `onStop`, so a download that finishes in the background is never offered for install in that process.
- While stuck, `MainViewModel` treats the update as pending (`updateStatus !is None`), so the support card is blocked for the session.

Fix: move the `DOWNLOADED` check above the `Downloading` early return; on a non-OK activity result reset to `Available` (or `None`); handle `InstallStatus.CANCELED` the same way.

### 4. Links crash the app on a device with no browser (confirmed)

- `ui/settings/SettingsScreen.kt` (~line 445): the privacy policy row calls `context.startActivity(ACTION_VIEW)` unguarded.
- `ui/about/AboutScreen.kt` (~line 85): the `openUri` lambda behind Mastodon, Bluesky and Website calls `startActivity` unguarded.

On a device with no `ACTION_VIEW` handler for `https` (FOSS build on a degoogled ROM, a kiosk or managed device, a disabled browser), the tap throws an uncaught `ActivityNotFoundException`. The email row (`runCatching`) and the rate row (`openPlayStoreListing`) on the same screen are already guarded.

Fix: wrap both in `runCatching`, or share one guarded "open URL" helper.

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

### 8. The tip jar opened from the support card is lost on rotation (confirmed)

`ui/main/MainScreen.kt` (~line 176): `showTipDialog` uses `remember`, not `rememberSaveable`.

Tapping "Support development" calls `onSupportPromptOpened`, which hides and snoozes the card immediately. A rotation, fold or resize then recreates the activity, the dialog disappears, and the main screen has no way back to it. `SettingsScreen` (~lines 150-153) has the same pattern for its dialogs, but there the entry row is still visible, so it is only an annoyance.

Fix: `rememberSaveable` for the dialog flags.

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

## Low

### 12. Startup crashes are never reported (confirmed)

`BasicRootCheckerApplication.onCreate()` (~line 66). The comment says crashes during async init are "buffered by Analytics until the opt-out preference is resolved". The buffer is in memory, and the previous handler kills the process right away, so the preference is never resolved and the buffer never flushes. After the SDK has started, `TelemetryDeck.signal` writes its cache from an IO coroutine, which the dying process often never runs, so later crashes are often lost too (likely). Either make the comment true (persist synchronously before delegating) or correct the comment.

### 13. Crash reports send the raw exception message (confirmed)

`analytics/Analytics.kt` `trackError()` (~line 85) forwards `throwable.message` into `TelemetryDeck.Error.message` for uncaught crashes. Messages like `Index 7 out of bounds for length 3` or `Unable to start activity ComponentInfo{…}` are near-unique per event, against `telemetry-optimization.md`'s rule that the field stays low-cardinality and free of device-revealing strings. Also, in release the `simpleName` fallback id of an app exception class is R8-obfuscated.

### 14. Backup restores the last root check onto a new device (confirmed data flow)

`res/xml/data_extraction_rules.xml` and `backup_rules.xml` include all of `datastore/`, which holds `last_root_check_*` (`UserPreferences`). After a device transfer or cloud restore from a rooted phone to a stock one, `getLastRootCheck` answers "Rooted via Magisk" with the old phone's `checkedAt` until the first check. `check_count`, `rooted_check_count` and `pending_tip_tokens` travel too. Fix: move the last-check record to a separate, excluded DataStore file, or clear it on restore.

### 15. A pending tip that clears can be announced twice (likely, timing-dependent)

`gplay/.../billing/GPlayBillingController.kt` `announceIfLateClear()` (~line 290) reads and then clears the stored pending tokens in two non-atomic steps. `handleFreshPurchase` and `reconcilePurchases` each call it from their own `Dispatchers.IO` coroutine, so returning to the app can run both close together and show two "tip cleared" snackbars. Fix: do the read-and-remove inside one DataStore `edit {}` and announce only if this call removed the token.

### 16. The language picker shows nothing selected for a regional tag (likely)

`ui/settings/SettingsScreen.kt` (~line 579) with `util/AppLanguage.kt` (~line 41). `currentTag` returns the full `toLanguageTag()`, compared exactly against the bare tags in `SUPPORTED_TAGS`. If the system per-app language screen sets `de-AT` or `zh-Hans-CN`, no radio button is selected (the subtitle still reads correctly). Not verified that Android's picker offers regional variants for this app. Fix: match by best supported prefix.

### 17. Two IDE previews cannot render (confirmed)

`util/PlayStoreListingPreviews.kt` (~line 36) and `util/ConstrainedDevicePreviews.kt` (~line 94) call `AboutScreen(...)`, which builds `AboutViewModel` and casts the application to `BasicRootCheckerApplication`. Layoutlib has no such Application, so these previews throw. Fix: call `AboutScreenContent(...)`. The screenshot test run is not affected.

### 18. Script nits (confirmed)

- `scripts/generate-store-screenshots.sh` (~lines 42-49) resolves `script` before `cd` to the repo root, so a relative `AI_KIT_SCRIPTS` passes the `-f` check and then fails at `exec python3`. Resolve it to an absolute path first.
- `scripts/generate-iap-icons.sh` (~lines 142-151) sends Chrome's output to `/dev/null`; under `set -e` a Chrome failure exits with no message, leaving the icons already written modified. Print an error and restore on failure.
