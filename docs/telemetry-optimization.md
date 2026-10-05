# Telemetry Optimization

**What** the app sends to TelemetryDeck and **why**: the signal set, its volume and its quality. Companion to [`Basic-Root-Checker-StructuralData.json`](./Basic-Root-Checker-StructuralData.json) (the exported event/parameter inventory) and [`telemetry-dashboard-queries.json`](./telemetry-dashboard-queries.json) (the dashboard panels). For the query language itself, use the `telemetry-and-tql` skill, which bundles the complete TQL reference.

**Basic Root Checker has no background or periodic work.** Every signal is user-initiated (FAB tap, navigation, tip, link), so volume is bounded by real actions and there is no churn signal to collapse. That leaves two levers: keeping **non-user, synthetic traffic** out of production, and keeping the **error channel** clean.

`Analytics.kt` (`analytics/`) holds this app's signal vocabulary. The TelemetryDeck lifecycle, including test-traffic detection, lives in `com.iboalali.telemetry:core`.

## Synthetic traffic is flagged as test mode

Release builds running under **Firebase Test Lab** and the **Play Console pre-launch report robot** (which runs on Test Lab) would otherwise count as real users. That inflates every production metric (installs, sessions, `rootCheckStarted` / `rootCheckCompleted`) and surfaces bogus devices and locales.

So TelemetryDeck **test mode** is on for `BuildConfig.DEBUG` builds and for any device where `com.iboalali.telemetry:core` detects Test Lab. Detection reads Google's documented `firebase.test.lab` system setting and **fails open**: any read failure means `false`, so an odd device is treated as a real user rather than misfiled. Test-mode signals are kept out of all production charts (including the premade dashboards) and stay visible through the dashboard's Test Mode toggle.

Why test mode rather than the alternatives:

- **Per-insight `isBot` filter: rejected.** TelemetryDeck filters are **per-insight only**. There is no app-level filter, and the **premade dashboards can't be filtered at all**. `isBot` is also a server-side heuristic based largely on the user-agent string, which a native Android app doesn't send, so it may not flag this traffic at all.
- **Dropping the signals client-side: rejected.** A false positive (an odd device that exposes the setting) would **silently and permanently destroy a real user's analytics**.
- **Test mode: chosen.** `testMode` is a first-class TelemetryDeck flag. A false positive only misfiles a real user's data into the test bucket, which is recoverable. It also matches how debug builds are treated.

Data caveat: signals ingested before v2.5 (2026-06-29) still count Test Lab and pre-launch traffic as production, so historical charts from before then read slightly high.

## Open review: error channel and parameter cardinality

This needs a live 30-day pull from the TelemetryDeck dashboard or Insights API, which isn't available from this repo: rank signals by volume and by fires per user, then prune the noisy ones. Look at two things first:

1. **Error channel.** Audit the `Analytics.trackError` sites for *expected fallbacks* reported as errors. The candidates are the `RootHaptics` catch blocks (`haptic-*`) and the `RootChecker` filesystem probes (`probeSuBinary` / `probeMagiskPaths` `SecurityException`s, `probeMagiskMounts`). On locked-down devices or devices with odd actuators these can throw routinely and are *expected*, not bugs. If any is high-volume, demote it to a `Log` and drop it from the error channel. Also check that `TelemetryDeck.Error.message` doesn't carry high-cardinality or device-revealing strings.
2. **Parameter cardinality.** `rootProviderDetected.version` and `otherAppClicked.packageName` are bounded (real root-manager versions; the curated catalog). Confirm nothing new carries an unbounded per-instance key.

Diagnostic queries. Set `relativeIntervals` to taste. This app's `appID` is `613251CD-B223-443A-9583-3A18586FAB55`. Add `{"type":"selector","dimension":"isTestMode","value":"False"}` to any filter for production-only numbers.

**Signals ranked by volume** (what dominates?):

```json
{
  "queryType": "topN", "granularity": "all", "threshold": 25,
  "dimension": { "type": "default", "dimension": "type", "outputName": "Signal" },
  "metric": { "type": "numeric", "metric": "count" },
  "aggregations": [{ "type": "eventCount", "name": "count" }],
  "filter": null,
  "baseFilters": "thisApp", "appID": "613251CD-B223-443A-9583-3A18586FAB55"
}
```

**Same, ranked by distinct users:** swap `eventCount` for `userCount` above. `eventCount ÷ userCount` per signal is fires per user, the redundancy detector. A signal that is large on `eventCount` but small on `userCount` is per-user churn.

**Errors by id** (find a noisy expected-fallback error):

```json
{
  "queryType": "topN", "granularity": "all", "threshold": 25,
  "dimension": { "type": "default", "dimension": "TelemetryDeck.Error.id", "outputName": "errorId" },
  "metric": { "type": "numeric", "metric": "count" },
  "aggregations": [{ "type": "eventCount", "name": "count" }],
  "filter": { "type": "selector", "dimension": "type", "value": "TelemetryDeck.Error.occurred" },
  "baseFilters": "thisApp", "appID": "613251CD-B223-443A-9583-3A18586FAB55"
}
```

## The support-card funnel counts offers, not appearances

The main screen's tip-jar card emits three signals, which together are the funnel:

| Signal | Params | Meaning |
|---|---|---|
| `supportCardShown` | none | one offer reached the screen |
| `tipJarOpened` | `source=support_card` | the offer was taken |
| `supportCardDismissed` | `dismissCount` | the offer was declined, with the running total |

`supportCardShown` is emitted **once per process**. `MainViewModel.onSupportPromptShown` holds the guard, and `MainScreen` calls it from a `LaunchedEffect` keyed on the card's visibility.

The guard is needed because two things make the card become visible more than once for a single offer, and each re-runs the reporting effect:

1. **Activity recreation** (rotation, fold/unfold, resize). `MainActivity` sets no `configChanges`, so the activity is rebuilt and the composition restarts, while the `MainViewModel` holding `supportPromptVisible` survives.
2. **An update card taking the slot.** `MainScreen` gives the slot to a pending in-app update, because that card is functional and time-sensitive while the ask can wait. When the update resolves, the support card comes back.

Without the guard both would inflate `supportCardShown`, while `tipJarOpened` and `supportCardDismissed` stay accurate (both are tap-driven). The conversion rate would then read low for a reason unrelated to the card.

One offer per process is the correct ceiling: answering the card either way snoozes it for a month (`SupportGate.snoozeUntil`), so a second real offer cannot happen in the same process.

This is the same one-shot shape as `deviceType` (`Analytics.trackDeviceType`) and the `reviewRequestedThisSession` flag: three places where a config change would otherwise be counted as a user action.

Reading the conversion:

```json
{
  "queryType": "timeseries", "granularity": "week",
  "aggregations": [
    { "type": "filtered",
      "filter": { "type": "selector", "dimension": "type", "value": "supportCardShown" },
      "aggregator": { "type": "eventCount", "name": "shown" } },
    { "type": "filtered",
      "filter": { "type": "and", "fields": [
        { "type": "selector", "dimension": "type", "value": "tipJarOpened" },
        { "type": "selector", "dimension": "source", "value": "support_card" }
      ]},
      "aggregator": { "type": "eventCount", "name": "opened" } }
  ],
  "filter": { "type": "selector", "dimension": "isTestMode", "value": "False" },
  "baseFilters": "thisApp", "appID": "613251CD-B223-443A-9583-3A18586FAB55"
}
```

Keep the `isTestMode` filter. The debug-only **Demo: support card** overflow item drives the same state, so it emits `supportCardShown` too, as test-flagged data.

## The structural-data export is a window, not an inventory

`Basic-Root-Checker-StructuralData.json` lists what the dashboard has **recently ingested**, so it cannot serve as the app's signal vocabulary. A signal missing from the export can mean it was removed from the app, or that nobody triggered it in the window. Both happen: `websiteClicked` is absent because it no longer exists, while `rateLinkClicked` and `tipPurchased` have been absent while still live.

In the 2026-08-27 export, eight signals in `Analytics.kt` were missing, for two different reasons:

- **Too new to have data:** `supportCardShown`, `supportCardDismissed`, `rateLinkClicked`, `reviewRequested`, `reviewFlowFailed` shipped in 2.5 on 2026-08-26, one day before the export.
- **Live since 2.3 or earlier, so the absence is an outcome:** `tipPurchased` (nobody completed a tip in the window), `billingUnavailable` (Play Billing connected for everyone) and `updateFailed` (no in-app update failed).

The export also contradicts itself: it lists the `dismissCount` parameter while omitting `supportCardDismissed`, the only signal that carries it.

**`Analytics.kt` is the source of truth for dashboard work.** Build panels from it, and use the export only to predict which panels have data yet. The panels are in [`telemetry-dashboard-queries.json`](./telemetry-dashboard-queries.json): 34 queries in seven sections, with the two data-quality levers above as section 7.

## Open item: the `socialLinkClicked` `platform` parameter

The 2026-08-27 export is the first with a populated `parameters` array (the 2026-06-29 one has `"parameters": []`, so it says nothing either way). Two parameters in the code are missing from it: `code` (`billingUnavailable`) and `platform` (`socialLinkClicked`). `code` is consistent with its signal never firing. `platform` is not: `socialLinkClicked` appears in **both** exports, so the signal is being ingested, and `platform` is its only parameter. `TelemetryDeck.Device.platform` is listed, so the name may collide with the built-in dimension.

Verify with panel 6.2 before trusting any social-link breakdown. If it returns a blank or missing row while panel 6.5 shows `socialLinkClicked` firing, the parameter is not queryable under that name. The fix is then app-side in `Analytics.trackSocialLinkClicked`: rename it to something that cannot collide with the built-in dimension. Do not rename it before the query confirms the problem, because the rename splits the dimension and loses continuity with the data already there.
