# Telemetry Optimization

A record of deliberate changes to **what** the app sends to TelemetryDeck and **why** — the
signal set, its volume, and its quality. Companion to
[`Basic-Root-Checker-StructuralData.json`](./Basic-Root-Checker-StructuralData.json) (the exported
event/parameter inventory) and [`TQL-Guideline-v0.1.0.md`](./TQL-Guideline-v0.1.0.md) (the query
language reference). Append a new dated section here for each future review.

Modeled on the same review done for the *Hide Persistent Notification* app. The big difference: that
app runs a 5-second periodic foreground service, so its volume was dominated (~75%) by a signal
re-firing on every sweep. **Basic Root Checker has no background or periodic work** — every signal
is user-initiated (FAB tap, navigation, tip, link), so volume is naturally bounded by real actions
and there is no churn signal to collapse. That leaves two levers worth pulling: keeping
**non-user / synthetic traffic** out of production, and keeping the **error channel** clean.

---

## 2026-06-29 — exclude bot & test-lab traffic (v2.5)

### Finding

The app flagged only `BuildConfig.DEBUG` builds as TelemetryDeck **test-mode**
(`BasicRootCheckerApplication`). Release builds running under Google's CI — **Firebase Test Lab**
and the **Play Console pre-launch report robot** (which runs on Test Lab) — were therefore counted
as real users. That inflates every production metric (installs, sessions, `rootCheckStarted` /
`rootCheckCompleted`) and surfaces bogus devices/locales, exactly the synthetic-traffic problem the
companion app hit (its `appIconNotFound` package breakdown exposed `androidx.test.tools.crawler` and
`com.android.google.gce.gceservice`).

### Change shipped

| Change | Effect |
|---|---|
| New `TestEnvironment.isFirebaseTestLab(context)` (`util/`) — reads Google's documented `firebase.test.lab` system setting, **fail-open** (any read failure ⇒ `false`, so an odd device is treated as a real user, not misfiled). | Detects both Firebase Test Lab and the Play Console pre-launch robot. |
| `BasicRootCheckerApplication` → `.testMode(BuildConfig.DEBUG \|\| TestEnvironment.isFirebaseTestLab(applicationContext))`. | Synthetic traffic is segregated out of all production charts (including the premade dashboards) and remains viewable via the dashboard's Test Mode toggle. |

No user-facing behavior changed.

### Why test-mode, not drop or per-query filter

Same reasoning as the companion app:

- **Per-insight `isBot` filter** — rejected. TelemetryDeck filters are **per-insight only**; there
  is no global/app-level filter and the **premade dashboards can't be filtered at all**. `isBot` is
  also a server-side heuristic derived largely from the user-agent string, which a native Android
  app doesn't have — so it may not even flag this traffic.
- **Drop the signals client-side** — rejected. A detection false-positive (an odd device that
  exposes the setting) would **silently and permanently destroy a real user's analytics**.
- **Flag as test-mode** ✅ — chosen. `testMode` is a first-class TelemetryDeck flag; test-mode
  signals are globally segregated out of the production view yet stay inspectable via the Test Mode
  toggle. A false-positive merely misfiles a real user's data into the test bucket — recoverable,
  not destroyed. It's also consistent with how the app already treats DEBUG builds, and it's the
  smallest change.

### Residual notes

- **Forward-only.** This flags *new* signals; data ingested before this build still contains Test
  Lab / pre-launch traffic as production data, so historical charts read slightly high. New data is
  clean.
- **No global filter exists** in TelemetryDeck — per-insight filtering is the only query-side lever,
  which is exactly why this is handled client-side at signal time.

### Open follow-up — a real volume/quality pull (needs dashboard access)

Not done here: the data-driven half of the companion review (rank signals by volume and by
fires/user, then prune the noisy ones). It needs a live 30-day pull from the TelemetryDeck dashboard
/ Insights API, which isn't available from this repo. When doing it, two things to look at first:

1. **Error channel.** Audit the `Analytics.trackError` sites for *expected fallbacks* masquerading
   as errors (the companion's `appIconNotFound` was 97% of its error channel). The candidates here
   are the `RootHaptics` catch blocks (`haptic-*`) and the `RootChecker` filesystem probes
   (`probeSuBinary` / `probeMagiskPaths` `SecurityException`s, `probeMagiskMounts`) — on locked-down
   or quirky-actuator devices these can throw routinely and are *expected*, not bugs. If any is
   high-volume, demote it to a `Log` and drop it from the error channel, and check that
   `TelemetryDeck.Error.message` isn't carrying high-cardinality / device-revealing strings.
2. **Param cardinality.** `rootProviderDetected.version` and `otherAppClicked.packageName` are
   bounded (real root-manager versions; the curated catalog) and fine; just confirm nothing new
   carries an unbounded per-instance key.

Diagnostic queries (set `relativeIntervals` to taste; `appID` is this app's —
`613251CD-B223-443A-9583-3A18586FAB55`). Add
`{"type":"selector","dimension":"isTestMode","value":"False"}` to any filter for production-only
numbers now that bots are test-flagged.

**Signals ranked by volume** (the "what dominates?" query):

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

**Same ranked by distinct users** — `eventCount ÷ userCount` per signal = fires/user, the redundancy
detector (swap `eventCount` → `userCount` above). A signal huge on eventCount but small on userCount
is per-user churn.

**Error breakdown by id** — find an `appIconNotFound`-style noise offender:

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
