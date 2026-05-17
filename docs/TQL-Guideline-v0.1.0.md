# TQL Complete Reference & Guide

**Version 0.1.0 (Beta)** | For AI Assistants | TelemetryDeck Query Language

---

## Table of Contents

**Part 1: Introduction & Core Concepts**

- 1.1 What is TQL?
- 1.2 Understanding the Data Model
- 1.3 Query Anatomy
- 1.4 How to Use This Guide
- 1.5 Dashboard vs API (Overview)

**Part 2: Query Types - Complete Reference** ⭐ CORE

- 2.1 Choosing the Right Query Type
- 2.2 Timeseries Queries (65% of charts)
- 2.3 TopN Queries (28% of charts)
- 2.4 Funnel Queries (3% of charts)
- 2.5 Retention Queries (2% of charts)
- 2.6 Experiment Queries (1% of charts)
- 2.7 GroupBy Queries (1% of charts)
- 2.8 Scan Queries (<1% of charts)

**Part 3: Query Components Reference**

- 3.1 Aggregations (What to Measure)
- 3.2 Filters (What to Include/Exclude)
- 3.3 Post-Aggregations (Calculations)
- 3.4 Time Handling
- 3.5 Dimensions & Extraction Functions
- 3.6 Value Formatting

**Part 4: Data Reference**

- 4.1 Understanding Available Data
- 4.2 Default Parameters (86 Parameters)
- 4.3 Custom Parameters & floatValue
- 4.4 Common Signal Types

**Part 5: Dashboard vs API & Troubleshooting**

- 5.1 Understanding Dashboard Magic
- 5.2 Converting Dashboard to API
- 5.3 Troubleshooting Guide

**Part 6: Advanced Techniques**

- 6.1 Special Calculations
- 6.2 Workarounds & Tips
- 6.3 Bot Detection & Data Quality
- 6.4 Registered Lookups
- 6.5 Version Sorting
- 6.6 Complex Filter Patterns
- 6.7 Performance Optimization

**Part 7: Quick Reference**

- 7.1 Query Type Decision Chart
- 7.2 All Aggregator Types
- 7.3 All Filter Types
- 7.4 All Granularities
- 7.5 Common Dimensions
- 7.6 Registered Lookups
- 7.7 Common Patterns Cheat Sheet
- 7.8 Field Quick Lookup

---

# Part 1: Introduction & Core Concepts

## 1.1 What is TQL?

**TQL (TelemetryDeck Query Language)** is a JSON-based query language for analyzing app analytics data in TelemetryDeck.

**Key Facts:**

- Built on Apache Druid (you don't need to know Druid to use TQL)
- Used for creating dashboard charts and API queries
- Queries return aggregated analytics data, not raw events
- Optimized for time-series analysis and dimensional breakdowns

**What TQL Can Do:**

- Track metrics over time (daily/weekly/monthly trends)
- Rank top values (top platforms, versions, countries)
- Analyze user journeys (funnels, retention)
- Calculate derived metrics (percentages, ratios, averages)
- Filter and segment data by any dimension

## 1.2 Understanding the Data Model

### Signals

**Signals** are individual events sent from your app to TelemetryDeck. Each signal includes:

- **Timestamp**: When the event occurred
- **Signal Type**: Event name (e.g., "appLaunchedRegularly", "Button.clicked")
- **Default Parameters**: 86 parameters automatically included (device, app, user info)
- **Custom Parameters**: Your own data (via payload dictionary)
- **Numeric Values**: Stored in `floatValue` field

### Dimensions

**Dimensions** are categorical data you can filter and group by:

- Device information (platform, model, OS version)
- App information (app version, build number)
- User information (country, language, locale)
- Context (time of day, day of week)
- Custom dimensions (any parameter you send)

### Metrics

**Metrics** are numeric measurements calculated from signals:

- User counts (unique users)
- Event counts (total signals)
- Sums (total revenue, total duration)
- Averages (avg session time, avg purchase value)
- Distributions (histogram of values)

### Time

Every signal has a timestamp. Queries aggregate data into time buckets:

- **Granularity**: hour, day, week, month, quarter, year, or "all" (no bucketing)
- **Intervals**: Time range to query (last 30 days, specific date range)

## 1.3 Query Anatomy

Every TQL query is a JSON object with this basic structure:

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "userCount",
      "name": "Active Users"
    }
  ],
  "filter": null,
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "day",
        "offset": -30,
        "position": "beginning"
      },
      "endDate": {
        "component": "day",
        "offset": 0,
        "position": "end"
      }
    }
  ],
  "baseFilters": "thisApp",
  "appID": null
}
```

**Core Components:**

| Component           | Purpose                                           | Required                   |
| ------------------- | ------------------------------------------------- | -------------------------- |
| `queryType`         | Type of analysis (timeseries, topN, funnel, etc.) | Yes                        |
| `granularity`       | Time bucketing (day, week, month, all)            | Yes                        |
| `aggregations`      | What to measure (user count, sum, average)        | Yes                        |
| `filter`            | What data to include/exclude                      | No                         |
| `relativeIntervals` | Time range to query                               | No (defaults to dashboard) |
| `postAggregations`  | Calculations on results (ratios, percentages)     | No                         |
| `baseFilters`       | Scope filter ("thisApp" or "thisOrganization")    | Yes for API                |
| `appID`             | Which app to query                                | Yes for API                |
| `valueFormatter`    | How to format numbers (currency, percent)         | No                         |

## 1.4 How to Use This Guide

This guide is organized to answer different types of questions:

**"I want to build a query that shows [X]"**
→ Start with **Part 2.1** (Choosing Query Type), then go to the specific query type section

**"What aggregators/filters/etc. are available?"**
→ Jump to **Part 3** (Components Reference)

**"What data can I query? What parameters exist?"**
→ See **Part 4** (Data Reference) for all 86 default parameters

**"My query doesn't work / returns no results"**
→ Check **Part 5.3** (Troubleshooting Guide)

**"I copied a query from the dashboard and it doesn't work in the API"**
→ See **Part 5.1-5.2** (Dashboard vs API Conversion)

**"How do I [advanced technique]?"**
→ Browse **Part 6** (Advanced Techniques)

**"Quick lookup of syntax"**
→ Use **Part 7** (Quick Reference)

**Navigation Tips:**

- Each query type in Part 2 includes complete examples at multiple complexity levels
- Examples progress from simple (90% of queries) to complex (1% of queries)
- All examples are API-ready (no dashboard placeholders)
- Cross-references link to detailed explanations

## 1.5 Dashboard vs API (Overview)

**Important:** Queries in the TelemetryDeck dashboard use **Ember.js placeholders** that don't work directly in the API.

**Dashboard Query Example:**

```json
{
  "dataSource": this.dataSource,
  "appID": this.args.appID,
  "baseFilters": "thisApp"
}
```

**API-Ready Query:**

```json
{
  "dataSource": "telemetry-signals",
  "appID": "12345678-1234-1234-1234-123456789012",
  "baseFilters": "thisApp"
}
```

**Common Conversions Needed:**

- `this.dataSource` → `"telemetry-signals"` (or your data source name)
- `this.args.appID` → Your actual app UUID
- `baseFilters: 'thisApp'` → Keep as is, but add explicit `appID`
- `relativeIntervals` → Usually works as-is, or convert to absolute dates

**Complete conversion guide in Part 5.2**

---

**Ready to build queries?** Continue to Part 2 to choose your query type and see examples.

---

# Part 2: Query Types - Complete Reference

## 2.1 Choosing the Right Query Type

### Decision Guide

Ask yourself: **"What do I want to know?"**

| User Question Pattern                       | Query Type | % of Charts | Jump To                       |
| ------------------------------------------- | ---------- | ----------- | ----------------------------- |
| "Show me **[metric] over time**"            | Timeseries | 65%         | [2.2](#22-timeseries-queries) |
| "Show me [metric] **per day/week/month**"   | Timeseries | 65%         | [2.2](#22-timeseries-queries) |
| "How has [metric] **changed/trended**"      | Timeseries | 65%         | [2.2](#22-timeseries-queries) |
| "What are the **top/most** [dimension]"     | TopN       | 28%         | [2.3](#23-topn-queries)       |
| "Show me a **breakdown** of [dimension]"    | TopN       | 28%         | [2.3](#23-topn-queries)       |
| "**Distribution** of [dimension]"           | TopN       | 28%         | [2.3](#23-topn-queries)       |
| "How many users go from **A → B → C**"      | Funnel     | 3%          | [2.4](#24-funnel-queries)     |
| "**Conversion** rate from [step] to [step]" | Funnel     | 3%          | [2.4](#24-funnel-queries)     |
| "**Drop-off** analysis"                     | Funnel     | 3%          | [2.4](#24-funnel-queries)     |
| "How many users **return/come back**"       | Retention  | 2%          | [2.5](#25-retention-queries)  |
| "**Cohort** analysis"                       | Retention  | 2%          | [2.5](#25-retention-queries)  |
| "N-day **retention**"                       | Retention  | 2%          | [2.5](#25-retention-queries)  |
| "**Compare A vs B** groups"                 | Experiment | 1%          | [2.6](#26-experiment-queries) |
| "**A/B test** results"                      | Experiment | 1%          | [2.6](#26-experiment-queries) |
| "Break down by **multiple dimensions**"     | GroupBy    | 1%          | [2.7](#27-groupby-queries)    |
| "Show me **raw data/events**"               | Scan       | <1%         | [2.8](#28-scan-queries)       |
| "List of recent **signals**"                | Scan       | <1%         | [2.8](#28-scan-queries)       |

### Complexity Principle: Start Simple

**Critical Rule:** 90% of queries need only 3 things:

1. `queryType` (what kind of analysis)
2. `granularity` (time bucketing)
3. `aggregations` (what to measure - usually just 1)

**Complexity Distribution in Real-World Usage:**

| Level       | Description           | % of Queries | Add These Fields                          |
| ----------- | --------------------- | ------------ | ----------------------------------------- |
| **Level 1** | Simple single metric  | **90%**      | queryType + granularity + 1 aggregation   |
| **Level 2** | Filtered view         | **7%**       | + simple `filter`                         |
| **Level 3** | Multiple metrics      | **2%**       | + multiple `aggregations` (filtered)      |
| **Level 4** | Calculated metrics    | **1%**       | + `postAggregations` + `valueFormatter`   |
| **Level 5** | Distribution analysis | **<1%**      | + `histogram` aggregator + `range` filter |

**Before adding complexity, ask:**

- ❓ Does the user explicitly need this?
- ❓ Can I achieve this with a simpler approach?
- ❓ Am I adding fields "just in case"?

**Golden Rule:** Start with Level 1. Add complexity only when the user's question explicitly requires it.

---

## 2.2 Timeseries Queries

**Purpose:** Track any metric over time

**When to use:**

- "Show me daily/weekly/monthly [metric]"
- "How has [metric] changed over time"
- "What's the trend for [metric]"
- "[metric] per [time period]"

**Coverage:** 65% of all dashboard charts

### Required Fields

```json
{
  "queryType": "timeseries", // ALWAYS "timeseries"
  "granularity": "day", // Time bucketing: hour|day|week|month|quarter|year|all
  "aggregations": [
    // What to measure (at least 1)
    { "type": "userCount", "name": "Users" }
  ]
}
```

### Optional Fields

```json
{
  "filter": null,                     // Restrict to subset of data
  "relativeIntervals": [...],         // Time range (defaults to dashboard selection)
  "postAggregations": null,           // Calculate ratios/percentages
  "baseFilters": "thisApp",           // Scope to app (required for API)
  "appID": null,                      // App UUID (required for API)
  "valueFormatter": null              // Format numbers (currency, percent)
}
```

### Level 1: Simple - Single Metric (90% of queries)

**User asks:** "Show me daily active users"

**What you need:** Just `queryType` + `granularity` + `aggregations`

**What to omit:** Everything else (or set to `null`)

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "userCount",
      "name": "Active Users"
    }
  ],
  "filter": null,
  "relativeIntervals": null,
  "postAggregations": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": null
}
```

**Variations:**

- Change `"day"` → `"week"` for weekly users
- Change `"day"` → `"month"` for monthly users
- Change `"day"` → `"all"` for total count (single data point)

**Other Level 1 Examples:**

- Daily signal count: `{"type": "eventCount", "name": "Total Signals"}`
- Daily total revenue: `{"type": "doubleSum", "name": "Revenue", "fieldName": "floatValue"}`
- Daily avg session time: `{"type": "doubleMean", "name": "Avg Duration", "fieldName": "TelemetryDeck.Signal.durationInSeconds"}`

### Level 2: Filtered - Single Metric with Filter (7% of queries)

**User asks:** "Show me iOS users per day"

**What to add:** Simple `filter` to restrict to subset

**When to use:** User says "only", "just", "exclude", "for [specific value]"

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "userCount",
      "name": "iOS Users"
    }
  ],
  "filter": {
    "type": "selector",
    "dimension": "TelemetryDeck.Device.platform",
    "value": "iOS"
  },
  "relativeIntervals": null,
  "postAggregations": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": null
}
```

**Common Filter Dimensions:**

- `TelemetryDeck.Device.platform` → "iOS", "Android", "macOS", etc.
- `TelemetryDeck.AppInfo.version` → "2.1.0", "1.5.3", etc.
- `TelemetryDeck.UserPreference.region` → "US", "DE", "GB", etc.
- `type` → Signal type names ("Purchase.completed", etc.)

**Other Level 2 Examples:**

**Android users only:**

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [{ "type": "userCount", "name": "Android Users" }],
  "filter": {
    "type": "selector",
    "dimension": "TelemetryDeck.Device.platform",
    "value": "Android"
  },
  "baseFilters": "thisApp",
  "appID": null
}
```

**Specific signal type:**

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [{ "type": "eventCount", "name": "Purchases" }],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "Purchase.completed"
  },
  "baseFilters": "thisApp",
  "appID": null
}
```

### Level 3: Multi-Metric - Comparing Metrics (2% of queries)

**User asks:** "Show me total users and new users on the same chart"

**What to add:** Multiple `aggregations` with different filters

**When to use:** User wants to compare metrics with different conditions in ONE query

**Important:** Use `filtered` aggregations + `thetaSketch` (not `userCount`)

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "thetaSketch",
      "name": "_Total_Users",
      "fieldName": "clientUser"
    },
    {
      "type": "filtered",
      "name": "_New_Users_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "TelemetryDeck.Acquisition.newInstallDetected"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "New Users",
        "fieldName": "clientUser"
      }
    }
  ],
  "filter": null,
  "postAggregations": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": null
}
```

**Why `thetaSketch` instead of `userCount`?**

- `userCount` is simpler but can't be used in filtered aggregations
- `thetaSketch` works for complex scenarios
- Both count unique users, thetaSketch is more flexible

**Key Pattern:**

- Name internal aggregators with `_` prefix (hides from UI display)
- Filtered aggregator contains the public name
- Total/baseline goes in regular aggregation

**Other Level 3 Examples:**

**iOS vs Android users:**

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "filtered",
      "name": "_iOS_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Device.platform",
        "value": "iOS"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "iOS Users",
        "fieldName": "clientUser"
      }
    },
    {
      "type": "filtered",
      "name": "_Android_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Device.platform",
        "value": "Android"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Android Users",
        "fieldName": "clientUser"
      }
    }
  ],
  "filter": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

### Level 4: Calculated - Ratios and Percentages (1% of queries)

**User asks:** "What percentage of my daily active users are new users?"

**What to add:**

- Multiple aggregations (total + subset)
- `postAggregations` for division
- `valueFormatter` for percentage display

**When to use:** User wants ratios, percentages, "X per Y", "share of X"

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "thetaSketch",
      "name": "_Total_Users",
      "fieldName": "clientUser"
    },
    {
      "type": "filtered",
      "name": "_New_Users_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "TelemetryDeck.Acquisition.newInstallDetected"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "_New_Users",
        "fieldName": "clientUser"
      }
    }
  ],
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "New User Percentage",
      "fn": "/",
      "fields": [
        {
          "type": "finalizingFieldAccess",
          "fieldName": "_New_Users"
        },
        {
          "type": "finalizingFieldAccess",
          "fieldName": "_Total_Users"
        }
      ]
    }
  ],
  "filter": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": {
    "options": {
      "style": "percent",
      "minimumFractionDigits": 1
    }
  }
}
```

**Key Points:**

- Use `finalizingFieldAccess` for `thetaSketch` results
- Use `fieldAccess` for simple aggregators (count, sum, mean)
- Division: `[numerator, denominator]`
- Multiply by 100 if you want percentage without formatter

**Post-Aggregation Operations:**

- `/` - Division (for ratios, percentages)
- `*` - Multiplication (for scaling)
- `+` - Addition (for sums)
- `-` - Subtraction (for differences)

**Other Level 4 Examples:**

**Revenue per user:**

```json
{
  "queryType": "timeseries",
  "granularity": "month",
  "aggregations": [
    {
      "type": "doubleSum",
      "name": "_Total_Revenue",
      "fieldName": "floatValue"
    },
    {
      "type": "thetaSketch",
      "name": "_Total_Users",
      "fieldName": "clientUser"
    }
  ],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "Purchase.completed"
  },
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "Revenue Per User",
      "fn": "/",
      "fields": [
        { "type": "fieldAccess", "fieldName": "_Total_Revenue" },
        { "type": "finalizingFieldAccess", "fieldName": "_Total_Users" }
      ]
    }
  ],
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": {
    "options": {
      "style": "currency",
      "currency": "USD",
      "minimumFractionDigits": 2
    },
    "locale": "en-US"
  }
}
```

### Level 5: Distribution - Histograms (<1% of queries)

**User asks:** "Show me the distribution of onboarding completion times"

**What to add:**

- `histogram` aggregator with `splitPoints`
- `range` filter on same field (REQUIRED)
- Usually `granularity: "all"` (not time-based)

**When to use:** User wants distribution, buckets, "how many in each range", histogram

```json
{
  "queryType": "timeseries",
  "granularity": "all",
  "aggregations": [
    {
      "type": "histogram",
      "name": "Onboarding Duration Distribution",
      "fieldName": "TelemetryDeck.Signal.durationInSeconds",
      "splitPoints": [0, 5, 10, 30, 60, 120, 300]
    }
  ],
  "filter": {
    "type": "and",
    "fields": [
      {
        "type": "range",
        "column": "TelemetryDeck.Signal.durationInSeconds",
        "matchValueType": "DOUBLE",
        "lower": "0",
        "upper": "300",
        "upperOpen": true
      },
      {
        "type": "selector",
        "dimension": "type",
        "value": "Onboarding.completed"
      }
    ]
  },
  "postAggregations": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": null
}
```

**How `splitPoints` Work:**

- Creates N+1 buckets from N split points
- `[0, 5, 10, 30]` creates: [0-5), [5-10), [10-30), [30+]
- Values are inclusive at lower bound, exclusive at upper

**Common Duration Split Points:**

- Short (0-10s): `[0, 1, 2, 5, 10]`
- Medium (0-5min): `[0, 5, 10, 30, 60, 120, 300]`
- Long (0-1hr): `[0, 60, 300, 600, 1800, 3600]`

**CRITICAL:** Histogram REQUIRES matching range filter on same field

### Additional Timeseries Examples

#### Monthly Revenue in USD

**User asks:** "Show me monthly revenue for the last year"

```json
{
  "queryType": "timeseries",
  "granularity": "month",
  "aggregations": [
    {
      "type": "doubleSum",
      "name": "Revenue",
      "fieldName": "floatValue"
    }
  ],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "Purchase.completed"
  },
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "month",
        "offset": -12,
        "position": "beginning"
      },
      "endDate": {
        "component": "month",
        "offset": 0,
        "position": "end"
      }
    }
  ],
  "postAggregations": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": {
    "options": {
      "style": "currency",
      "currency": "USD",
      "minimumFractionDigits": 2,
      "maximumFractionDigits": 2
    },
    "locale": "en-US"
  }
}
```

**Key Points:**

- `granularity: "month"` for monthly buckets
- `doubleSum` on `floatValue` (where numeric revenue is stored)
- Filter to purchase signal type
- `relativeIntervals` specifies last 12 months
- `valueFormatter` for currency display

#### Average Session Duration

**User asks:** "What's the average session duration per day?"

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "doubleMean",
      "name": "Avg Session Duration (seconds)",
      "fieldName": "TelemetryDeck.Signal.durationInSeconds"
    }
  ],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "TelemetryDeck.Session.ended"
  },
  "postAggregations": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": null
}
```

**Key Points:**

- `doubleMean` calculates average
- `fieldName` points to duration field
- Filter to session end signals (where duration is captured)

#### Hourly Active Users (Last 24 Hours)

**User asks:** "Show me hourly active users for the past day"

```json
{
  "queryType": "timeseries",
  "granularity": "hour",
  "aggregations": [
    {
      "type": "userCount",
      "name": "Active Users"
    }
  ],
  "filter": null,
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "hour",
        "offset": -24,
        "position": "beginning"
      },
      "endDate": {
        "component": "hour",
        "offset": 0,
        "position": "end"
      }
    }
  ],
  "postAggregations": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": null
}
```

**Key Points:**

- `granularity: "hour"` for hourly buckets
- `relativeIntervals` with `component: "hour"` and `offset: -24`

#### Users by Day of Week

**User asks:** "Which days of the week have the most users?"

```json
{
  "queryType": "timeseries",
  "granularity": "all",
  "aggregations": [
    {
      "type": "filtered",
      "name": "_Monday_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Calendar.dayOfWeek",
        "value": "1"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Monday",
        "fieldName": "clientUser"
      }
    },
    {
      "type": "filtered",
      "name": "_Tuesday_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Calendar.dayOfWeek",
        "value": "2"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Tuesday",
        "fieldName": "clientUser"
      }
    },
    {
      "type": "filtered",
      "name": "_Wednesday_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Calendar.dayOfWeek",
        "value": "3"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Wednesday",
        "fieldName": "clientUser"
      }
    },
    {
      "type": "filtered",
      "name": "_Thursday_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Calendar.dayOfWeek",
        "value": "4"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Thursday",
        "fieldName": "clientUser"
      }
    },
    {
      "type": "filtered",
      "name": "_Friday_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Calendar.dayOfWeek",
        "value": "5"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Friday",
        "fieldName": "clientUser"
      }
    },
    {
      "type": "filtered",
      "name": "_Saturday_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Calendar.dayOfWeek",
        "value": "6"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Saturday",
        "fieldName": "clientUser"
      }
    },
    {
      "type": "filtered",
      "name": "_Sunday_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Calendar.dayOfWeek",
        "value": "7"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Sunday",
        "fieldName": "clientUser"
      }
    }
  ],
  "filter": null,
  "postAggregations": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": null
}
```

**Key Points:**

- `granularity: "all"` (single time bucket showing all 7 days)
- Seven filtered aggregations, one per day of week
- `TelemetryDeck.Calendar.dayOfWeek`: 1=Monday, 7=Sunday
- Results show which weekday has most users

---

## 2.3 TopN Queries

**Purpose:** Rank top N values by dimension

**When to use:**

- "What are the top 10 [platforms/versions/countries]"
- "Most common [dimension]"
- "Breakdown by [dimension]"
- "Show distribution of [dimension]"

**Coverage:** 28% of all dashboard charts

### Required Fields

```json
{
  "queryType": "topN",
  "granularity": "all",           // Usually "all" (not time-based)
  "threshold": 10,                // How many top results
  "dimension": {...},             // What to group by
  "metric": {...},                // How to sort
  "aggregations": [...]           // What to count/measure
}
```

### Optional Fields

```json
{
  "filter": null, // Restrict to subset
  "baseFilters": "thisApp", // Required for API
  "appID": null // Required for API
}
```

### Simple TopN - Top 10 Platforms

**User asks:** "What are my top 10 platforms by user count?"

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 10,
  "dimension": {
    "type": "default",
    "dimension": "TelemetryDeck.Device.platform",
    "outputName": "Platform"
  },
  "metric": {
    "type": "numeric",
    "metric": "count"
  },
  "aggregations": [
    {
      "type": "userCount",
      "name": "count",
      "fieldName": null
    }
  ],
  "filter": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `granularity: "all"` → Single snapshot, not time-based
- `threshold: 10` → Return top 10 results
- `dimension.type: "default"` → Simple dimension, no transformation
- `metric.type: "numeric"` → Sort by aggregation value
- `aggregations` name must match `metric.metric` ("count")

**Common Threshold Values:**

- `10` - Standard "top 10"
- `20` - Broader view
- `50` - Comprehensive breakdown
- `100` - Nearly complete picture

### TopN with Registered Lookup - Top Device Models

**User asks:** "Show me the top 50 device models"

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 50,
  "dimension": {
    "type": "extraction",
    "dimension": "TelemetryDeck.Device.modelName",
    "outputName": "Device",
    "extractionFn": {
      "type": "registeredLookup",
      "lookup": "appleModelNames",
      "retainMissingValue": true
    }
  },
  "metric": {
    "type": "numeric",
    "metric": "count"
  },
  "aggregations": [
    {
      "type": "userCount",
      "name": "count",
      "fieldName": null
    }
  ],
  "filter": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `dimension.type: "extraction"` → Transform dimension values
- `extractionFn.type: "registeredLookup"` → Use built-in lookup table
- `lookup: "appleModelNames"` → Converts "iPhone14,2" to "iPhone 13 Pro"
- `retainMissingValue: true` → Keep original if no match found

**Available Registered Lookups:**

- `appleModelNames` - iPhone/iPad/Mac model names
- `deviceType` - Phone/Tablet/Desktop/etc
- `processorFamily` - Processor family names
- `processorType` - Processor type names

### TopN with Version Sorting - Top App Versions

**User asks:** "What are the top app versions?"

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 20,
  "dimension": {
    "type": "default",
    "dimension": "TelemetryDeck.AppInfo.version",
    "outputName": "App Version"
  },
  "metric": {
    "type": "dimension",
    "ordering": "version"
  },
  "aggregations": [
    {
      "type": "userCount",
      "name": "count",
      "fieldName": null
    }
  ],
  "filter": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `metric.type: "dimension"` → Sort by dimension value, not count
- `ordering: "version"` → Semantic version sorting (10.0 > 9.0 > 2.1)
- Without version ordering: "9.0" would come after "10.0" alphabetically

**Other Ordering Options:**

- `"lexicographic"` - Alphabetical (A-Z)
- `"alphanumeric"` - Alphanumeric sorting
- `"numeric"` - Numeric values
- `"strlen"` - By string length
- `"version"` - Semantic version sorting (USE FOR APP/OS VERSIONS)

### TopN with Filters - Top Countries (Exclude Bots)

**User asks:** "Show me top 20 countries excluding bots and empty values"

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 20,
  "dimension": {
    "type": "default",
    "dimension": "TelemetryDeck.UserPreference.region",
    "outputName": "Country"
  },
  "metric": {
    "type": "numeric",
    "metric": "count"
  },
  "aggregations": [
    {
      "type": "userCount",
      "name": "count",
      "fieldName": null
    }
  ],
  "filter": {
    "type": "and",
    "fields": [
      {
        "type": "selector",
        "dimension": "isBot",
        "value": "False"
      },
      {
        "type": "not",
        "field": {
          "type": "selector",
          "dimension": "TelemetryDeck.UserPreference.region",
          "value": ""
        }
      }
    ]
  },
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `filter.type: "and"` → All conditions must match
- `isBot: "False"` → Exclude bot traffic
- `not` + empty selector → Exclude empty values
- Common pattern for data quality

### TopN by Event Count - Most Used Features

**User asks:** "What are the top 10 features by usage?"

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 10,
  "dimension": {
    "type": "default",
    "dimension": "type",
    "outputName": "Feature"
  },
  "metric": {
    "type": "numeric",
    "metric": "count"
  },
  "aggregations": [
    {
      "type": "eventCount",
      "name": "count",
      "fieldName": null
    }
  ],
  "filter": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `dimension: "type"` → Signal type (event names)
- `eventCount` instead of `userCount` → Counts signals, not unique users
- Shows most frequently triggered events

### TopN with Range Filter - Power Users

**User asks:** "Show me top countries for users with more than 10 sessions"

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 20,
  "dimension": {
    "type": "default",
    "dimension": "TelemetryDeck.UserPreference.region",
    "outputName": "Country"
  },
  "metric": {
    "type": "numeric",
    "metric": "count"
  },
  "aggregations": [
    {
      "type": "userCount",
      "name": "count",
      "fieldName": null
    }
  ],
  "filter": {
    "type": "range",
    "column": "TelemetryDeck.Retention.totalSessionsCount",
    "matchValueType": "DOUBLE",
    "lower": "10",
    "lowerOpen": false
  },
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `filter.type: "range"` → Numeric range filter
- `column: "TelemetryDeck.Retention.totalSessionsCount"` → Session count field
- `lower: "10"` + `lowerOpen: false` → >= 10 (inclusive)
- Filters to engaged/power users

---

## 2.4 Funnel Queries

**Purpose:** Track users through sequential steps (conversion analysis)

**When to use:**

- "How many users go from A → B → C"
- "Conversion rate from [step] to [step]"
- "Drop-off analysis"
- "User journey completion"

**Coverage:** 3% of all dashboard charts

### Required Fields

```json
{
  "queryType": "funnel",
  "granularity": "all",           // Usually "all" (overall funnel)
  "steps": [                      // 2-10 sequential steps
    {"name": "Step 1", "filter": {...}},
    {"name": "Step 2", "filter": {...}},
    {"name": "Step 3", "filter": {...}}
  ]
}
```

### Optional Fields

```json
{
  "filter": null, // Global filter for all steps
  "relativeIntervals": null, // Time range
  "baseFilters": "thisApp", // Required for API
  "appID": null // Required for API
}
```

### Key Concepts

**How Funnels Work:**

- Users must complete steps **in order**
- Users automatically matched by `clientUser` (user ID)
- No time limit between steps (unless you add interval filters)
- Each step shows: count + % of previous step + % of first step

**Step Structure:**

```json
{
  "name": "Step Name", // Display name
  "filter": {
    // Usually selector for signal type
    "type": "selector",
    "dimension": "type",
    "value": "SignalType.name"
  }
}
```

### Funnel Example: Onboarding Flow

**User asks:** "Show me conversion from app launch through onboarding"

```json
{
  "queryType": "funnel",
  "granularity": "all",
  "steps": [
    {
      "name": "App Launched",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "appLaunchedRegularly"
      }
    },
    {
      "name": "Tutorial Started",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Tutorial.started"
      }
    },
    {
      "name": "Tutorial Completed",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Tutorial.completed"
      }
    },
    {
      "name": "First Action Completed",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "FirstAction.completed"
      }
    }
  ],
  "filter": null,
  "relativeIntervals": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

**Typical Results:**

- Step 1: 10,000 users (100%)
- Step 2: 7,000 users (70% of step 1)
- Step 3: 5,000 users (71% of step 2, 50% of step 1)
- Step 4: 3,500 users (70% of step 3, 35% of step 1)

### Funnel Example: Paywall Conversion

**User asks:** "Show me conversion from viewing paywall to purchasing"

```json
{
  "queryType": "funnel",
  "granularity": "all",
  "steps": [
    {
      "name": "Free Feature Used",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Feature.free.used"
      }
    },
    {
      "name": "Paywall Viewed",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Paywall.viewed"
      }
    },
    {
      "name": "Purchase Initiated",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Purchase.initiated"
      }
    },
    {
      "name": "Purchase Completed",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Purchase.completed"
      }
    }
  ],
  "filter": null,
  "relativeIntervals": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

### Funnel Example: Feature Adoption

**User asks:** "Show me feature discovery to adoption funnel"

```json
{
  "queryType": "funnel",
  "granularity": "all",
  "steps": [
    {
      "name": "Feature Discovered",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Feature.X.discovered"
      }
    },
    {
      "name": "Feature Tried",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Feature.X.firstUse"
      }
    },
    {
      "name": "Feature Used Multiple Times",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Feature.X.used"
      }
    }
  ],
  "filter": null,
  "relativeIntervals": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

**Best Practices:**

- 2-5 steps is most common (too many steps = low completion rates)
- Name steps clearly for business users
- Use consistent signal naming conventions
- Each step filter should be mutually exclusive if possible

---

## 2.5 Retention Queries

**Purpose:** Cohort retention analysis - track how many users return over time

**When to use:**

- "How many users come back"
- "N-day retention"
- "Cohort analysis"
- "User retention rate"

**Coverage:** 2% of all dashboard charts

### Required Fields

```json
{
  "queryType": "retention",
  "granularity": "month",         // Cohort period: day|week|month|quarter|year
  "relativeIntervals": [...]      // Time range for analysis
}
```

### Optional Fields

```json
{
  "baseFilters": "thisApp", // Required for API
  "appID": null // Required for API
}
```

### Key Concepts

**How Retention Works:**

- **Cohorts** are groups of users who first used the app in the same period
- **Granularity** defines cohort period (day/week/month)
- Shows what % of each cohort returns in subsequent periods
- Automatically tracks user IDs across time

**Results Format:**

- Rows = Cohorts (by time period)
- Columns = Periods after initial use (0, 1, 2, 3...)
- Values = % of cohort still active

### Retention Example: Monthly Cohorts

**User asks:** "Show me monthly user retention for the last 6 months"

```json
{
  "queryType": "retention",
  "granularity": "month",
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "month",
        "offset": -6,
        "position": "beginning"
      },
      "endDate": {
        "component": "month",
        "offset": 0,
        "position": "end"
      }
    }
  ],
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `granularity: "month"` → Monthly cohorts
- `offset: -6` → Last 6 months
- Results show: Month 0 (100%), Month 1, Month 2, etc.

**Typical Results:**

- January cohort: 100% → 45% → 32% → 25% → 20% → 18%
- February cohort: 100% → 50% → 35% → 28% → 22%
- March cohort: 100% → 48% → 33% → 26%

### Retention Example: Weekly Cohorts

**User asks:** "Show me week-by-week retention for the last 12 weeks"

```json
{
  "queryType": "retention",
  "granularity": "week",
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "week",
        "offset": -12,
        "position": "beginning"
      },
      "endDate": {
        "component": "week",
        "offset": 0,
        "position": "end"
      }
    }
  ],
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `granularity: "week"` → Weekly cohorts
- Good for apps with weekly usage patterns
- Shows Week 0 → Week 1 → Week 2, etc.

### Retention Example: Daily Cohorts (7-Day Window)

**User asks:** "Show me daily retention for the last 30 days"

```json
{
  "queryType": "retention",
  "granularity": "day",
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "day",
        "offset": -30,
        "position": "beginning"
      },
      "endDate": {
        "component": "day",
        "offset": 0,
        "position": "end"
      }
    }
  ],
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `granularity: "day"` → Daily cohorts
- Good for high-engagement apps (gaming, social)
- Shows Day 0 → Day 1 → Day 7 → Day 30

**Choosing Granularity:**

- **Daily**: High-frequency apps (games, social media, messaging)
- **Weekly**: Medium-frequency apps (productivity, fitness)
- **Monthly**: Low-frequency apps (B2B, utilities, seasonal)

---

## 2.6 Experiment Queries

**Purpose:** A/B testing and variant comparison

**When to use:**

- "Compare A vs B groups"
- "A/B test results"
- "Variant performance"
- "Test control vs treatment"

**Coverage:** 1% of all dashboard charts

### Required Fields

```json
{
  "queryType": "experiment",
  "granularity": "all",
  "dimension": "...",             // Dimension defining variants
  "aggregations": [...]           // What to measure
}
```

### Optional Fields

```json
{
  "filter": null,
  "relativeIntervals": null,
  "baseFilters": "thisApp",
  "appID": null
}
```

### Experiment Example: Paywall A/B Test

**User asks:** "Compare conversion rates between paywall variants"

```json
{
  "queryType": "experiment",
  "granularity": "all",
  "dimension": {
    "type": "default",
    "dimension": "paywallVariant",
    "outputName": "Variant"
  },
  "aggregations": [
    {
      "type": "thetaSketch",
      "name": "_TotalUsers",
      "fieldName": "clientUser"
    },
    {
      "type": "filtered",
      "name": "_Converted",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Purchase.completed"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "_ConvertedUsers",
        "fieldName": "clientUser"
      }
    }
  ],
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "Conversion Rate",
      "fn": "/",
      "fields": [
        { "type": "finalizingFieldAccess", "fieldName": "_ConvertedUsers" },
        { "type": "finalizingFieldAccess", "fieldName": "_TotalUsers" }
      ]
    }
  ],
  "filter": null,
  "baseFilters": "thisApp",
  "appID": null,
  "valueFormatter": {
    "options": { "style": "percent", "minimumFractionDigits": 2 }
  }
}
```

**Key Points:**

- `dimension` defines the groups to compare (variant A, B, C)
- Calculate metrics for each variant
- Often uses postAggregations for rates/percentages
- Results show side-by-side comparison

**Typical Results:**

- Control: 100,000 users, 2,500 conversions (2.5%)
- Variant A: 98,000 users, 2,940 conversions (3.0%)
- Variant B: 102,000 users, 3,570 conversions (3.5%)

---

## 2.7 GroupBy Queries

**Purpose:** Multi-dimensional breakdowns

**When to use:**

- "Break down by multiple dimensions"
- "Show [metric] by [dimension1] and [dimension2]"
- "Cross-tabulation"
- Complex segmentation

**Coverage:** 1% of all dashboard charts

### Required Fields

```json
{
  "queryType": "groupBy",
  "granularity": "all",
  "dimensions": ["dim1", "dim2"],  // Multiple dimensions
  "aggregations": [...]            // What to measure
}
```

### Optional Fields

```json
{
  "filter": null,
  "limitSpec": {...},              // Sorting and limiting results
  "baseFilters": "thisApp",
  "appID": null
}
```

### GroupBy Example: Platform × Version Breakdown

**User asks:** "Show me users by platform and app version"

```json
{
  "queryType": "groupBy",
  "granularity": "all",
  "dimensions": [
    "TelemetryDeck.Device.platform",
    "TelemetryDeck.AppInfo.version"
  ],
  "aggregations": [
    {
      "type": "userCount",
      "name": "Users",
      "fieldName": null
    }
  ],
  "filter": null,
  "limitSpec": {
    "type": "default",
    "columns": [
      {
        "dimension": "Users",
        "direction": "descending"
      }
    ],
    "limit": 100
  },
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `dimensions` array defines breakdown dimensions
- Results are all combinations (iOS × 2.1.0, iOS × 2.0.5, Android × 2.1.0, etc.)
- `limitSpec` controls sorting and limiting
- Can be very large result set - use filters to narrow

**Typical Results:**
| Platform | Version | Users |
|----------|---------|-------|
| iOS | 2.1.0 | 45,000 |
| iOS | 2.0.5 | 23,000 |
| Android | 2.1.0 | 38,000 |
| Android | 2.0.5 | 19,000 |
| macOS | 2.1.0 | 8,500 |

**Note:** For simple breakdowns, use TopN instead. Use GroupBy only when you need multiple dimensions.

---

## 2.8 Scan Queries

**Purpose:** Retrieve raw data/events (not aggregated)

**When to use:**

- "Show me raw data"
- "List of recent signals"
- "Event details"
- Debugging/inspection

**Coverage:** <1% of all dashboard charts

### Required Fields

```json
{
  "queryType": "scan",
  "granularity": "all",
  "columns": ["col1", "col2"], // Which fields to return
  "limit": 100 // Max rows to return
}
```

### Optional Fields

```json
{
  "filter": null,
  "intervals": [...],              // Time range
  "baseFilters": "thisApp",
  "appID": null
}
```

### Scan Example: Recent Signals

**User asks:** "Show me the most recent 50 signals"

```json
{
  "queryType": "scan",
  "granularity": "all",
  "columns": [
    "__time",
    "type",
    "clientUser",
    "TelemetryDeck.Device.platform",
    "TelemetryDeck.AppInfo.version"
  ],
  "limit": 50,
  "filter": null,
  "intervals": ["2025-01-01T00:00:00Z/2025-12-31T23:59:59Z"],
  "baseFilters": "thisApp",
  "appID": null
}
```

**Key Points:**

- `columns` specifies which fields to retrieve
- `__time` is the timestamp column
- `limit` prevents returning too much data
- Returns individual events, not aggregations
- Useful for debugging but expensive for large volumes

**Typical Results:**

```json
[
  {
    "__time": "2025-01-15T10:23:45Z",
    "type": "appLaunchedRegularly",
    "clientUser": "user-abc-123",
    "TelemetryDeck.Device.platform": "iOS",
    "TelemetryDeck.AppInfo.version": "2.1.0"
  },
  {
    "__time": "2025-01-15T10:23:12Z",
    "type": "Purchase.completed",
    "clientUser": "user-def-456",
    "TelemetryDeck.Device.platform": "Android",
    "TelemetryDeck.AppInfo.version": "2.0.5"
  }
]
```

**Warning:** Scan queries can be expensive and slow on large datasets. Always use:

- Small `limit` values (50-1000)
- Specific time ranges
- Filters to narrow results

---

**End of Part 2: Query Types**

You now have comprehensive coverage of all 7 TQL query types with 30+ complete, working examples progressing from simple to advanced complexity.

**Next:** Part 3 will provide detailed reference for all query components (aggregations, filters, post-aggregations, etc.)

---

# Part 3: Query Components Reference

This section provides detailed reference for all query components. Use this when you need to look up specific syntax or understand what options are available.

## 3.1 Aggregations (What to Measure)

Aggregations define what metrics to calculate from your data. Every query needs at least one aggregation.

### userCount

**Purpose:** Count unique users (most common aggregation)

**When to use:**

- Daily/monthly active users
- User counts by dimension
- Any "how many users" question

**Syntax:**

```json
{
  "type": "userCount",
  "name": "Active Users"
}
```

**No fieldName needed** - automatically counts unique `clientUser` values

**Example use:**

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [{ "type": "userCount", "name": "Daily Active Users" }]
}
```

**Limitations:**

- Cannot be used inside filtered aggregations (use thetaSketch instead)
- Simple counting only (no set operations)

---

### eventCount (or count)

**Purpose:** Count total signals/events (not unique users)

**When to use:**

- Total signals received
- Feature usage frequency
- Event counts per period
- "How many times did X happen"

**Syntax:**

```json
{
  "type": "eventCount",
  "name": "Total Signals"
}
```

**Alternative:** `"type": "count"` (same as eventCount)

**No fieldName needed** - counts all matching rows

**Example use:**

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [{ "type": "eventCount", "name": "Purchase Events" }],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "Purchase.completed"
  }
}
```

**Difference from userCount:**

- userCount: One user clicking 10 times = 1
- eventCount: One user clicking 10 times = 10

---

### doubleSum

**Purpose:** Sum numeric values

**When to use:**

- Total revenue
- Total duration
- Sum of any numeric field
- Cumulative metrics

**Syntax:**

```json
{
  "type": "doubleSum",
  "name": "Total Revenue",
  "fieldName": "floatValue"
}
```

**fieldName required** - specifies which numeric field to sum

**Common fieldName values:**

- `"floatValue"` - Custom numeric data (revenue, prices, scores)
- `"TelemetryDeck.Signal.durationInSeconds"` - Duration data

**Example use:**

```json
{
  "queryType": "timeseries",
  "granularity": "month",
  "aggregations": [
    {
      "type": "doubleSum",
      "name": "Monthly Revenue",
      "fieldName": "floatValue"
    }
  ],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "Purchase.completed"
  }
}
```

---

### doubleMean

**Purpose:** Calculate average of numeric values

**When to use:**

- Average session duration
- Average purchase value
- Average response time
- Mean of any numeric field

**Syntax:**

```json
{
  "type": "doubleMean",
  "name": "Avg Session Duration",
  "fieldName": "TelemetryDeck.Signal.durationInSeconds"
}
```

**fieldName required** - specifies which field to average

**Example use:**

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "doubleMean",
      "name": "Average Session (seconds)",
      "fieldName": "TelemetryDeck.Signal.durationInSeconds"
    }
  ],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "TelemetryDeck.Session.ended"
  }
}
```

---

### doubleMin / doubleMax

**Purpose:** Find minimum or maximum value

**When to use:**

- Shortest/longest session
- Min/max purchase value
- Peak values

**Syntax:**

```json
{
  "type": "doubleMin",
  "name": "Shortest Session",
  "fieldName": "TelemetryDeck.Signal.durationInSeconds"
}
```

```json
{
  "type": "doubleMax",
  "name": "Longest Session",
  "fieldName": "TelemetryDeck.Signal.durationInSeconds"
}
```

**Less common** - usually mean is more useful than min/max

---

### thetaSketch

**Purpose:** Advanced unique counting with set operations

**When to use:**

- Inside filtered aggregations
- When you need set operations (union, intersect) in postAggregations
- Complex multi-metric queries
- Comparing user segments

**Syntax:**

```json
{
  "type": "thetaSketch",
  "name": "Users",
  "fieldName": "clientUser"
}
```

**fieldName required** - always `"clientUser"` for user counting

**Why use instead of userCount?**

- userCount is simpler but limited
- thetaSketch required for filtered aggregations
- thetaSketch enables set operations

**Example use:**

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "thetaSketch",
      "name": "_Total_Users",
      "fieldName": "clientUser"
    },
    {
      "type": "filtered",
      "name": "_iOS_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Device.platform",
        "value": "iOS"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "iOS Users",
        "fieldName": "clientUser"
      }
    }
  ]
}
```

**Important:** Use `finalizingFieldAccess` in postAggregations when referencing thetaSketch results

---

### histogram

**Purpose:** Distribution analysis (bucket counts)

**When to use:**

- Duration distribution
- Value range breakdowns
- "How many users in each time bucket"
- Distribution histograms

**Syntax:**

```json
{
  "type": "histogram",
  "name": "Duration Distribution",
  "fieldName": "TelemetryDeck.Signal.durationInSeconds",
  "splitPoints": [0, 5, 10, 30, 60, 120, 300]
}
```

**Required fields:**

- `fieldName` - Numeric field to analyze
- `splitPoints` - Array of bucket boundaries

**How splitPoints work:**

- N split points create N+1 buckets
- `[0, 5, 10]` creates: [0-5), [5-10), [10+]
- Inclusive at lower bound, exclusive at upper

**CRITICAL:** Histogram requires matching range filter on same field

**Example use:**

```json
{
  "queryType": "timeseries",
  "granularity": "all",
  "aggregations": [
    {
      "type": "histogram",
      "name": "Onboarding Time Distribution",
      "fieldName": "TelemetryDeck.Signal.durationInSeconds",
      "splitPoints": [0, 5, 10, 30, 60, 120, 300]
    }
  ],
  "filter": {
    "type": "and",
    "fields": [
      {
        "type": "range",
        "column": "TelemetryDeck.Signal.durationInSeconds",
        "matchValueType": "DOUBLE",
        "lower": "0",
        "upper": "300",
        "upperOpen": true
      },
      {
        "type": "selector",
        "dimension": "type",
        "value": "Onboarding.completed"
      }
    ]
  }
}
```

---

### filtered

**Purpose:** Apply aggregation to a subset of data

**When to use:**

- Comparing multiple metrics with different filters in ONE query
- "iOS users vs Android users on same chart"
- Multiple segments simultaneously

**Syntax:**

```json
{
  "type": "filtered",
  "name": "_Filtered_Aggregation",
  "filter": {
    "type": "selector",
    "dimension": "TelemetryDeck.Device.platform",
    "value": "iOS"
  },
  "aggregator": {
    "type": "thetaSketch",
    "name": "iOS Users",
    "fieldName": "clientUser"
  }
}
```

**When NOT to use:**

- Single metric with filter → Use top-level `filter` instead
- Example: "Show me iOS users" → Don't use filtered aggregation, use simple filter

**When to use:**

- Multiple metrics with different filters
- Example: "Show me iOS users AND Android users" → Use filtered aggregations

**Example use:**

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    {
      "type": "filtered",
      "name": "_iOS_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Device.platform",
        "value": "iOS"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "iOS Users",
        "fieldName": "clientUser"
      }
    },
    {
      "type": "filtered",
      "name": "_Android_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "TelemetryDeck.Device.platform",
        "value": "Android"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "Android Users",
        "fieldName": "clientUser"
      }
    }
  ]
}
```

**Tip:** Name outer aggregation with `_` prefix to hide from UI

---

## 3.2 Filters (What to Include/Exclude)

Filters restrict which data to include in your query. All filter types can be combined with `and`, `or`, and `not`.

### selector

**Purpose:** Exact match on a dimension value

**When to use:**

- Filter to specific platform
- Filter to specific signal type
- Filter to specific version
- Any "equals" condition

**Most common filter type**

**Syntax:**

```json
{
  "type": "selector",
  "dimension": "TelemetryDeck.Device.platform",
  "value": "iOS"
}
```

**Example uses:**

**Platform filter:**

```json
{
  "type": "selector",
  "dimension": "TelemetryDeck.Device.platform",
  "value": "iOS"
}
```

**Signal type filter:**

```json
{
  "type": "selector",
  "dimension": "type",
  "value": "Purchase.completed"
}
```

**Version filter:**

```json
{
  "type": "selector",
  "dimension": "TelemetryDeck.AppInfo.version",
  "value": "2.1.0"
}
```

---

### in

**Purpose:** Match any value in a list (OR logic for single dimension)

**When to use:**

- "iOS OR macOS"
- Multiple signal types
- Multiple versions
- Any "is one of" condition

**Syntax:**

```json
{
  "type": "in",
  "dimension": "TelemetryDeck.Device.platform",
  "values": ["iOS", "macOS", "iPadOS"]
}
```

**Example uses:**

**Multiple platforms:**

```json
{
  "type": "in",
  "dimension": "TelemetryDeck.Device.platform",
  "values": ["iOS", "iPadOS"]
}
```

**Multiple signal types:**

```json
{
  "type": "in",
  "dimension": "type",
  "values": [
    "Purchase.completed",
    "Purchase.restored",
    "RevenueCat.Events.RENEWAL"
  ]
}
```

---

### range

**Purpose:** Numeric range filter (inclusive/exclusive bounds)

**When to use:**

- Duration between X and Y seconds
- Session count > N
- Any numeric range condition
- **Required for histogram aggregators**

**Syntax:**

```json
{
  "type": "range",
  "column": "TelemetryDeck.Signal.durationInSeconds",
  "matchValueType": "DOUBLE",
  "lower": "0",
  "upper": "300",
  "lowerOpen": false,
  "upperOpen": true
}
```

**Fields:**

- `column` - Numeric field name
- `matchValueType` - Always `"DOUBLE"`
- `lower` - Minimum value (as string)
- `upper` - Maximum value (as string)
- `lowerOpen` - `true` for > (exclusive), `false`/`null` for >= (inclusive)
- `upperOpen` - `true` for < (exclusive), `false`/`null` for <= (inclusive)

**Example uses:**

**Duration 0-300 seconds:**

```json
{
  "type": "range",
  "column": "TelemetryDeck.Signal.durationInSeconds",
  "matchValueType": "DOUBLE",
  "lower": "0",
  "upper": "300",
  "upperOpen": true
}
```

**Session count >= 10:**

```json
{
  "type": "range",
  "column": "TelemetryDeck.Retention.totalSessionsCount",
  "matchValueType": "DOUBLE",
  "lower": "10",
  "lowerOpen": false
}
```

**Values > 100:**

```json
{
  "type": "range",
  "column": "floatValue",
  "matchValueType": "DOUBLE",
  "lower": "100",
  "lowerOpen": true
}
```

---

### regex

**Purpose:** Pattern matching with regular expressions

**When to use:**

- Exclude empty values (pattern: `.+`)
- Partial string matching
- Complex string patterns
- **Less common** - use selector/in when possible

**Syntax:**

```json
{
  "type": "regex",
  "dimension": "TelemetryDeck.UserPreference.region",
  "pattern": ".+"
}
```

**Example uses:**

**Exclude empty values:**

```json
{
  "type": "regex",
  "dimension": "TelemetryDeck.UserPreference.region",
  "pattern": ".+"
}
```

**Match pattern:**

```json
{
  "type": "regex",
  "dimension": "type",
  "pattern": "^Purchase\\."
}
```

**Note:** Alternative to excluding empty - use `not` with selector for `""`

---

### and

**Purpose:** Combine filters - all must match

**When to use:**

- Multiple conditions must be true
- "iOS AND version 2.0"
- "Signal type X AND not bot"

**Syntax:**

```json
{
  "type": "and",
  "fields": [
    {
      "type": "selector",
      "dimension": "TelemetryDeck.Device.platform",
      "value": "iOS"
    },
    {
      "type": "selector",
      "dimension": "TelemetryDeck.AppInfo.version",
      "value": "2.0.0"
    }
  ]
}
```

**Example uses:**

**Platform AND version:**

```json
{
  "type": "and",
  "fields": [
    {
      "type": "selector",
      "dimension": "TelemetryDeck.Device.platform",
      "value": "iOS"
    },
    {
      "type": "selector",
      "dimension": "TelemetryDeck.AppInfo.version",
      "value": "2.1.0"
    }
  ]
}
```

**Exclude bots AND empty values:**

```json
{
  "type": "and",
  "fields": [
    {
      "type": "selector",
      "dimension": "isBot",
      "value": "False"
    },
    {
      "type": "not",
      "field": {
        "type": "selector",
        "dimension": "TelemetryDeck.UserPreference.region",
        "value": ""
      }
    }
  ]
}
```

---

### or

**Purpose:** Combine filters - any must match

**When to use:**

- Alternative conditions
- "Signal type A OR B"
- Complex OR logic across dimensions

**Syntax:**

```json
{
  "type": "or",
  "fields": [
    {
      "type": "selector",
      "dimension": "type",
      "value": "Purchase.completed"
    },
    {
      "type": "selector",
      "dimension": "type",
      "value": "Purchase.restored"
    }
  ]
}
```

**Note:** For OR on single dimension, prefer `in` filter

**Better:**

```json
{
  "type": "in",
  "dimension": "type",
  "values": ["Purchase.completed", "Purchase.restored"]
}
```

**Use `or` for different dimensions:**

```json
{
  "type": "or",
  "fields": [
    {
      "type": "selector",
      "dimension": "TelemetryDeck.Device.platform",
      "value": "iOS"
    },
    {
      "type": "selector",
      "dimension": "TelemetryDeck.AppInfo.version",
      "value": "2.0.0"
    }
  ]
}
```

---

### not

**Purpose:** Negate a filter (exclude matches)

**When to use:**

- Exclude specific values
- "NOT bot"
- "NOT empty"
- Inverse of any filter

**Syntax:**

```json
{
  "type": "not",
  "field": {
    "type": "selector",
    "dimension": "isBot",
    "value": "True"
  }
}
```

**Example uses:**

**Exclude bots:**

```json
{
  "type": "not",
  "field": {
    "type": "selector",
    "dimension": "isBot",
    "value": "True"
  }
}
```

**Alternative (preferred for bots):**

```json
{
  "type": "selector",
  "dimension": "isBot",
  "value": "False"
}
```

**Exclude empty values:**

```json
{
  "type": "not",
  "field": {
    "type": "selector",
    "dimension": "TelemetryDeck.UserPreference.region",
    "value": ""
  }
}
```

**Exclude specific platform:**

```json
{
  "type": "not",
  "field": {
    "type": "selector",
    "dimension": "TelemetryDeck.Device.platform",
    "value": "watchOS"
  }
}
```

---

## 3.3 Post-Aggregations (Calculations)

Post-aggregations perform calculations on aggregation results. Use for ratios, percentages, "per user" metrics.

### arithmetic

**Purpose:** Mathematical operations on aggregated values

**When to use:**

- Percentages (divide, optionally multiply by 100)
- Ratios (divide)
- Per-user metrics (divide by user count)
- Scaling (multiply)
- Differences (subtract)

**Syntax:**

```json
{
  "type": "arithmetic",
  "name": "Conversion Rate",
  "fn": "/",
  "fields": [
    {
      "type": "fieldAccess",
      "fieldName": "Converted Users"
    },
    {
      "type": "fieldAccess",
      "fieldName": "Total Users"
    }
  ]
}
```

**Operations (`fn`):**

- `"/"` - Division (most common - for ratios/percentages)
- `"*"` - Multiplication (for scaling)
- `"+"` - Addition (for sums)
- `"-"` - Subtraction (for differences)

**Field Types:**

**fieldAccess** - For simple aggregations:

```json
{
  "type": "fieldAccess",
  "fieldName": "Revenue"
}
```

Use for: `count`, `eventCount`, `userCount`, `doubleSum`, `doubleMean`

**finalizingFieldAccess** - For thetaSketch:

```json
{
  "type": "finalizingFieldAccess",
  "fieldName": "_Total_Users"
}
```

Use for: `thetaSketch` results only

**constant** - For fixed values:

```json
{
  "type": "constant",
  "value": 100
}
```

Use for: Multiplying by 100 for percentages, fixed denominators

### Example: Percentage Calculation

**Without multiplying by 100 (use percent formatter):**

```json
{
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "New User Percentage",
      "fn": "/",
      "fields": [
        { "type": "finalizingFieldAccess", "fieldName": "_New_Users" },
        { "type": "finalizingFieldAccess", "fieldName": "_Total_Users" }
      ]
    }
  ],
  "valueFormatter": {
    "options": { "style": "percent", "minimumFractionDigits": 1 }
  }
}
```

**With multiplying by 100 (no formatter):**

```json
{
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "New User Percentage",
      "fn": "*",
      "fields": [
        {
          "type": "arithmetic",
          "fn": "/",
          "fields": [
            { "type": "finalizingFieldAccess", "fieldName": "_New_Users" },
            { "type": "finalizingFieldAccess", "fieldName": "_Total_Users" }
          ]
        },
        { "type": "constant", "value": 100 }
      ]
    }
  ]
}
```

### Example: Revenue Per User

```json
{
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "Revenue Per User",
      "fn": "/",
      "fields": [
        { "type": "fieldAccess", "fieldName": "_Total_Revenue" },
        { "type": "finalizingFieldAccess", "fieldName": "_Total_Users" }
      ]
    }
  ]
}
```

**Key Points:**

- Division order matters: [numerator, denominator]
- Use correct field access type (fieldAccess vs finalizingFieldAccess)
- Can nest arithmetic operations
- Reference aggregation names (not display names if different)

---

## 3.4 Time Handling

### Granularities

**Purpose:** Define time bucket size

**Available values:**

| Granularity | Use Case                    | Example            |
| ----------- | --------------------------- | ------------------ |
| `"hour"`    | Hourly trends, real-time    | Last 24 hours      |
| `"day"`     | Daily metrics (most common) | Daily active users |
| `"week"`    | Weekly trends               | Weekly retention   |
| `"month"`   | Monthly aggregations        | Monthly revenue    |
| `"quarter"` | Quarterly reports           | Q1 vs Q2           |
| `"year"`    | Annual trends               | Year-over-year     |
| `"all"`     | No time bucketing           | Single total       |

**When to use "all":**

- TopN queries (snapshot, not time-based)
- Funnel queries (overall conversion)
- Histogram queries (distribution)
- Any single-value result

---

### relativeIntervals

**Purpose:** Specify time range relative to now

**Syntax:**

```json
{
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "day",
        "offset": -30,
        "position": "beginning"
      },
      "endDate": {
        "component": "day",
        "offset": 0,
        "position": "end"
      }
    }
  ]
}
```

**Fields:**

- `component` - Time unit: `"hour"` | `"day"` | `"week"` | `"month"` | `"quarter"` | `"year"`
- `offset` - How far back (negative number)
- `position` - `"beginning"` or `"end"` of the period

**Common Patterns:**

**Last 30 days:**

```json
{
  "beginningDate": {
    "component": "day",
    "offset": -30,
    "position": "beginning"
  },
  "endDate": { "component": "day", "offset": 0, "position": "end" }
}
```

**Last 12 months:**

```json
{
  "beginningDate": {
    "component": "month",
    "offset": -12,
    "position": "beginning"
  },
  "endDate": { "component": "month", "offset": 0, "position": "end" }
}
```

**Last 7 days:**

```json
{
  "beginningDate": {
    "component": "day",
    "offset": -7,
    "position": "beginning"
  },
  "endDate": { "component": "day", "offset": 0, "position": "end" }
}
```

**Last 24 hours:**

```json
{
  "beginningDate": {
    "component": "hour",
    "offset": -24,
    "position": "beginning"
  },
  "endDate": { "component": "hour", "offset": 0, "position": "end" }
}
```

**When to omit:**

- Query will use dashboard's selected time range
- Good for flexible queries where user controls time range

---

### Absolute Intervals (intervals)

**Purpose:** Specify exact date ranges

**Syntax:**

```json
{
  "intervals": ["2025-01-01T00:00:00Z/2025-12-31T23:59:59Z"]
}
```

**Format:** ISO8601 interval: `start/end`

- Start: ISO8601 timestamp
- End: ISO8601 timestamp

**Examples:**

**Specific month:**

```json
{
  "intervals": ["2025-01-01T00:00:00Z/2025-01-31T23:59:59Z"]
}
```

**Specific year:**

```json
{
  "intervals": ["2025-01-01T00:00:00Z/2025-12-31T23:59:59Z"]
}
```

**When to use:**

- Specific date ranges
- Historical analysis
- Fixed reporting periods

**When to use relativeIntervals instead:**

- "Last N days/months"
- Rolling windows
- Dynamic time ranges

---

## 3.5 Dimensions & Extraction Functions

### Default Dimension

**Purpose:** Use dimension as-is, no transformation

**Syntax:**

```json
{
  "type": "default",
  "dimension": "TelemetryDeck.Device.platform",
  "outputName": "Platform"
}
```

**Use for:** Most dimensions - platform, version, country, etc.

---

### Extraction Dimension

**Purpose:** Transform dimension values (lookups, regex, etc.)

**Syntax:**

```json
{
  "type": "extraction",
  "dimension": "TelemetryDeck.Device.modelName",
  "outputName": "Device",
  "extractionFn": {
    "type": "registeredLookup",
    "lookup": "appleModelNames",
    "retainMissingValue": true
  }
}
```

---

### registeredLookup

**Purpose:** Use built-in lookup table for value transformation

**Available Lookups:**

| Lookup Name       | Purpose                     | Example Transformation         |
| ----------------- | --------------------------- | ------------------------------ |
| `appleModelNames` | iPhone/iPad/Mac model names | "iPhone14,2" → "iPhone 13 Pro" |
| `deviceType`      | Device category             | "iPhone14,2" → "Phone"         |
| `processorFamily` | CPU family                  | Value → Family name            |
| `processorType`   | CPU type                    | Value → Type name              |

**Syntax:**

```json
{
  "type": "registeredLookup",
  "lookup": "appleModelNames",
  "retainMissingValue": true
}
```

**retainMissingValue:**

- `true` - Keep original value if not in lookup
- `false` - Replace with null if not in lookup

**Most common:** `true` (keep unknowns)

---

### Custom Map Lookup

**Purpose:** Define your own value transformations

**Syntax:**

```json
{
  "type": "extraction",
  "dimension": "country.isoCode",
  "outputName": "Country",
  "extractionFn": {
    "type": "lookup",
    "lookup": {
      "type": "map",
      "map": {
        "US": "United States",
        "GB": "United Kingdom",
        "DE": "Germany",
        "FR": "France"
      }
    },
    "retainMissingValue": true,
    "injective": true
  }
}
```

**injective:**

- `true` - One-to-one mapping (optimization)
- `false` - Many-to-one possible

**Performance:** Keep map small (max 20-30 entries)

---

## 3.6 Value Formatting

**Purpose:** Format numbers for display (currency, percentages, decimals)

**When to use:**

- Revenue queries (currency)
- Percentage/ratio queries (percent)
- Custom decimal precision

### Currency Formatting

```json
{
  "valueFormatter": {
    "options": {
      "style": "currency",
      "currency": "USD",
      "minimumFractionDigits": 2,
      "maximumFractionDigits": 2
    },
    "locale": "en-US"
  }
}
```

**Common currencies:** `"USD"`, `"EUR"`, `"GBP"`, `"JPY"`

**Common locales:** `"en-US"`, `"de-DE"`, `"en-GB"`, `"ja-JP"`

### Percent Formatting

```json
{
  "valueFormatter": {
    "options": {
      "style": "percent",
      "minimumFractionDigits": 1,
      "maximumFractionDigits": 2
    }
  }
}
```

**Note:** Input should be decimal (0.25 for 25%)

### Decimal Formatting

```json
{
  "valueFormatter": {
    "options": {
      "style": "decimal",
      "minimumFractionDigits": 0,
      "maximumFractionDigits": 0
    }
  }
}
```

**Use for:** Integer display (no decimals)

---

**End of Part 3: Query Components Reference**

You now have complete reference for all aggregations, filters, post-aggregations, time handling, dimensions, and value formatting.

**Next:** Part 4 will document all 86 default parameters available in TelemetryDeck signals.

---

# Part 4: Data Reference

## 4.1 Understanding Available Data

**Every TelemetryDeck signal includes two types of data:**

1. **Default Parameters** - 86 parameters sent automatically with every signal
2. **Custom Parameters** - Your own data sent via payload dictionary

### What Are Signals?

**Signals** are events sent from your app to TelemetryDeck. Each signal includes:

- **Timestamp** (`__time`) - When it was received
- **Signal Type** (`type`) - Event name you define (e.g., "Purchase.completed")
- **User ID** (`clientUser`) - Unique user identifier
- **All 86 default parameters** - Automatically included
- **Custom data** - Anything you add via payload

### How to Query This Data

**Default parameters** can be used directly in filters and dimensions:

```json
{
  "filter": {
    "type": "selector",
    "dimension": "TelemetryDeck.Device.platform",
    "value": "iOS"
  }
}
```

**Custom parameters** are accessed by name:

```json
{
  "filter": {
    "type": "selector",
    "dimension": "myCustomField",
    "value": "someValue"
  }
}
```

**Numeric data** (revenue, duration, scores) goes in `floatValue`:

```json
{
  "aggregations": [
    {
      "type": "doubleSum",
      "name": "Total Revenue",
      "fieldName": "floatValue"
    }
  ]
}
```

---

## 4.2 Default Parameters (86 Parameters)

These parameters are automatically sent with every TelemetryDeck signal. No configuration needed - they just work.

### Main Parameters (8 parameters)

Core identifying information sent with every signal.

| Parameter    | Type   | Description                                   |
| ------------ | ------ | --------------------------------------------- |
| `appID`      | String | The ID of the app                             |
| `clientUser` | String | The ID of the user (hashed)                   |
| `type`       | String | The signal type (event name)                  |
| `__time`     | Date   | When signal was received by TelemetryDeck     |
| `count`      | Int    | Number of events (auto-incremented by server) |
| `isTestMode` | Bool   | Whether sent in test mode                     |
| `sessionID`  | String | ID of the current session                     |
| `floatValue` | Double | Numeric value sent with signal                |

**Most used:**

- `type` - Filter to specific events
- `clientUser` - For user counting (automatic in userCount)
- `floatValue` - For revenue, duration, scores

---

### App Info Parameters (3 parameters)

Information about the app build.

| Parameter                                     | Type   | Description                |
| --------------------------------------------- | ------ | -------------------------- |
| `TelemetryDeck.AppInfo.buildNumber`           | String | Build number (e.g., "123") |
| `TelemetryDeck.AppInfo.version`               | String | Version (e.g., "2.1.0")    |
| `TelemetryDeck.AppInfo.versionAndBuildNumber` | String | Both separated by space    |

**Most used:**

- `TelemetryDeck.AppInfo.version` - Filter/group by app version

---

### Device Parameters (14 parameters)

Information about the device running the app.

| Parameter                                      | Type   | Description                      |
| ---------------------------------------------- | ------ | -------------------------------- |
| `TelemetryDeck.Device.architecture`            | String | CPU architecture (arm64, x86_64) |
| `TelemetryDeck.Device.modelName`               | String | Device model (iPhone14,2)        |
| `TelemetryDeck.Device.operatingSystem`         | String | OS name                          |
| `TelemetryDeck.Device.orientation`             | String | Portrait/Landscape               |
| `TelemetryDeck.Device.platform`                | String | iOS, Android, macOS, etc.        |
| `TelemetryDeck.Device.screenResolutionHeight`  | Double | Screen height (points)           |
| `TelemetryDeck.Device.screenResolutionWidth`   | Double | Screen width (points)            |
| `TelemetryDeck.Device.screenScaleFactor`       | Double | Retina factor (2.0, 3.0)         |
| `TelemetryDeck.Device.systemMajorMinorVersion` | String | OS version (16.1)                |
| `TelemetryDeck.Device.systemMajorVersion`      | String | OS major (16)                    |
| `TelemetryDeck.Device.systemVersion`           | String | Full OS version                  |
| `TelemetryDeck.Device.timeZone`                | String | Device time zone                 |
| `TelemetryDeck.Device.screenDensity`           | Double | Screen density (Android)         |
| `TelemetryDeck.Device.brand`                   | String | Device brand (Android)           |

**Most used:**

- `TelemetryDeck.Device.platform` - iOS/Android/macOS splits
- `TelemetryDeck.Device.modelName` - Device breakdown (use with appleModelNames lookup)
- `TelemetryDeck.Device.systemVersion` - OS version filtering

---

### Run Context Parameters (10 parameters)

Context the app runs in (App Store, TestFlight, simulator, etc.).

| Parameter                                      | Type   | Description            |
| ---------------------------------------------- | ------ | ---------------------- |
| `TelemetryDeck.RunContext.isAppStore`          | Bool   | Running from App Store |
| `TelemetryDeck.RunContext.isDebug`             | Bool   | Debug build            |
| `TelemetryDeck.RunContext.isSimulator`         | Bool   | Running in simulator   |
| `TelemetryDeck.RunContext.isTestFlight`        | Bool   | TestFlight build       |
| `TelemetryDeck.RunContext.language`            | String | App language           |
| `TelemetryDeck.RunContext.locale`              | String | App locale             |
| `TelemetryDeck.RunContext.targetEnvironment`   | String | Target environment     |
| `TelemetryDeck.RunContext.extensionIdentifier` | String | Extension ID (widgets) |
| `TelemetryDeck.RunContext.isSideLoaded`        | Bool   | Side-loaded app        |
| `TelemetryDeck.RunContext.sourceMarketplace`   | String | Source marketplace     |

**Most used:**

- `TelemetryDeck.RunContext.isTestFlight` - Exclude beta testers
- `TelemetryDeck.RunContext.isSimulator` - Exclude development

---

### User Preference Parameters (4 parameters)

User's system preferences.

| Parameter                                      | Type   | Description                           |
| ---------------------------------------------- | ------ | ------------------------------------- |
| `TelemetryDeck.UserPreference.colorScheme`     | Enum   | Dark/Light mode                       |
| `TelemetryDeck.UserPreference.language`        | String | User's language                       |
| `TelemetryDeck.UserPreference.layoutDirection` | Enum   | LTR/RTL                               |
| `TelemetryDeck.UserPreference.region`          | String | User's country/region (2-letter code) |

**Most used:**

- `TelemetryDeck.UserPreference.region` - Country/region breakdown
- `TelemetryDeck.UserPreference.colorScheme` - Dark mode usage

---

### SDK Parameters (4 parameters)

TelemetryDeck SDK information.

| Parameter                          | Type   | Description    |
| ---------------------------------- | ------ | -------------- |
| `TelemetryDeck.SDK.name`           | String | SDK name       |
| `TelemetryDeck.SDK.nameAndVersion` | String | Name + version |
| `TelemetryDeck.SDK.version`        | String | SDK version    |
| `TelemetryDeck.SDK.buildType`      | String | SDK build type |

**Rarely used** - mostly for debugging SDK issues.

---

### Web Analytics - URL Data (6 parameters)

Web-specific parameters from page URLs.

| Parameter        | Type   | Description                          |
| ---------------- | ------ | ------------------------------------ |
| `url`            | String | Full URL (https://example.com/about) |
| `host`           | String | Domain (example.com)                 |
| `path`           | String | Path (/about)                        |
| `scheme`         | String | Protocol (https)                     |
| `referer`        | String | Referrer URL                         |
| `combinedSource` | String | Best source parameter                |

**Most used:**

- `path` - Page breakdown
- `host` - Domain tracking
- `referer` - Traffic source

---

### Web Analytics - Country/Region Data (3 parameters)

Geo data from IP address (web only).

| Parameter                   | Type   | Description            |
| --------------------------- | ------ | ---------------------- |
| `country.isoCode`           | String | Country code (US, DE)  |
| `country.isInEuropeanUnion` | Bool   | EU membership          |
| `continent.code`            | String | Continent (EU, NA, AS) |

---

### Web Analytics - User Agent Data (13 parameters)

Browser and device info from user agent string (web only).

| Parameter                 | Type   | Description      |
| ------------------------- | ------ | ---------------- |
| `systemVersion`           | String | OS version       |
| `majorSystemVersion`      | String | OS major version |
| `majorMinorSystemVersion` | String | OS major.minor   |
| `platform`                | String | OS platform      |
| `modelName`               | String | Device model     |
| `browserName`             | String | Browser family   |
| `browserVersion`          | String | Browser version  |
| `device`                  | String | Device type      |
| `isMobile`                | Bool   | Mobile device    |
| `isTablet`                | Bool   | Tablet device    |
| `isTouchCapable`          | Bool   | Touch capable    |
| `isDesktop`               | Bool   | Desktop device   |
| `isBot`                   | Bool   | Bot detected     |

**Most used:**

- `isBot` - Exclude bots (filter `isBot: "False"`)
- `browserName` - Browser breakdown
- `platform` - OS breakdown (web)

---

### Navigation Analytics Parameters (4 parameters)

Navigation tracking (opt-in).

| Parameter                                  | Type   | Description          |
| ------------------------------------------ | ------ | -------------------- |
| `TelemetryDeck.Navigation.schemaVersion`   | String | Must be "1"          |
| `TelemetryDeck.Navigation.sourcePath`      | String | Where user came from |
| `TelemetryDeck.Navigation.destinationPath` | String | Where user went      |
| `TelemetryDeck.Navigation.identifier`      | String | Unique navigation ID |

**Requires**: Explicit navigation tracking in your code.

---

### Pirate Metrics (AARRR) Parameters (15 parameters)

Acquisition, Activation, Retention, Referral, Revenue analytics.

#### Calendar Parameters (8 parameters)

| Parameter                              | Type   | Description         |
| -------------------------------------- | ------ | ------------------- |
| `TelemetryDeck.Calendar.dayOfMonth`    | String | Day (1-31)          |
| `TelemetryDeck.Calendar.dayOfWeek`     | String | Day (1=Mon, 7=Sun)  |
| `TelemetryDeck.Calendar.dayOfYear`     | String | Day of year (1-366) |
| `TelemetryDeck.Calendar.weekOfYear`    | String | Week number         |
| `TelemetryDeck.Calendar.isWeekend`     | Bool   | Saturday/Sunday     |
| `TelemetryDeck.Calendar.monthOfYear`   | String | Month (1-12)        |
| `TelemetryDeck.Calendar.quarterOfYear` | String | Quarter (1-4)       |
| `TelemetryDeck.Calendar.hourOfDay`     | String | Hour (1-24)         |

**Most used:**

- `TelemetryDeck.Calendar.dayOfWeek` - Weekday analysis
- `TelemetryDeck.Calendar.hourOfDay` - Time-of-day patterns

#### Acquisition Parameters (1 parameter)

| Parameter                                    | Type   | Description                 |
| -------------------------------------------- | ------ | --------------------------- |
| `TelemetryDeck.Acquisition.firstSessionDate` | String | First use date (YYYY-MM-DD) |

**Use for**: New vs returning user analysis.

#### Retention Parameters (5 parameters)

| Parameter                                           | Type   | Description           |
| --------------------------------------------------- | ------ | --------------------- |
| `TelemetryDeck.Retention.averageSessionSeconds`     | String | Avg session duration  |
| `TelemetryDeck.Retention.distinctDaysUsed`          | String | Total days used       |
| `TelemetryDeck.Retention.totalSessionsCount`        | Int    | Total sessions        |
| `TelemetryDeck.Retention.previousSessionSeconds`    | Int    | Last session duration |
| `TelemetryDeck.Retention.distinctDaysUsedLastMonth` | Int    | Days used last month  |

**Most used:**

- `TelemetryDeck.Retention.totalSessionsCount` - Power user analysis
- `TelemetryDeck.Retention.distinctDaysUsed` - Engagement tracking

**Special:** `averageSessionSeconds` has special calculation logic (see Part 6.1)

---

### API Parameters (2 parameters)

TelemetryDeck API metadata.

| Parameter                          | Type   | Description             |
| ---------------------------------- | ------ | ----------------------- |
| `TelemetryDeck.API.Ingest.version` | String | Ingest API version used |
| `TelemetryDeck.API.namespace`      | String | Data namespace          |

**Rarely used** - set automatically by server.

---

## 4.3 Custom Parameters & floatValue

### Custom String Parameters

**Any parameter you send** via payload becomes queryable:

```swift
// Swift SDK
TelemetryManager.send("Purchase.completed", with: [
  "productID": "premium_monthly",
  "category": "subscription"
])
```

**Query custom parameters:**

```json
{
  "filter": {
    "type": "selector",
    "dimension": "productID",
    "value": "premium_monthly"
  }
}
```

### floatValue for Numeric Data

**For numeric data** (revenue, duration, scores), use `floatValue`:

```swift
// Swift SDK
TelemetryManager.send("Purchase.completed", with: [
  "productID": "premium_monthly"
], floatValue: 9.99)  // Revenue amount
```

**Query numeric data:**

```json
{
  "aggregations": [
    {
      "type": "doubleSum",
      "name": "Total Revenue",
      "fieldName": "floatValue"
    }
  ],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "Purchase.completed"
  }
}
```

**Common uses for floatValue:**

- Revenue amounts
- Purchase prices
- Duration values
- Scores/ratings
- Quantities

**Important:** Only ONE numeric value per signal. Plan accordingly.

---

## 4.4 Common Signal Types

**Signal types** are event names you define. TelemetryDeck provides some defaults:

### Built-in Signal Types

| Signal Type                                    | When Sent    | Purpose              |
| ---------------------------------------------- | ------------ | -------------------- |
| `appLaunchedRegularly`                         | App launch   | Track opens          |
| `TelemetryDeck.Acquisition.newInstallDetected` | First launch | New user acquisition |
| `TelemetryDeck.Session.ended`                  | Session end  | Session tracking     |

### Custom Signal Types

**You define signal types** based on your app's events:

```
Purchase.completed
Purchase.initiated
Purchase.canceled
Feature.X.used
Tutorial.started
Tutorial.completed
Error.occurred
Settings.changed
```

**Best practices:**

- Use dot notation for hierarchy
- Be consistent with naming
- Don't use spaces (use underscores or dots)
- Be specific but not too granular

### Finding Your App's Signals

**Export your app's structure** to see what signals you're sending:

1. Go to App Setup in TelemetryDeck
2. Export signal types
3. Use export JSON to understand your data

**Or query for all signal types:**

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 100,
  "dimension": {
    "type": "default",
    "dimension": "type",
    "outputName": "Signal Type"
  },
  "metric": { "type": "numeric", "metric": "count" },
  "aggregations": [{ "type": "eventCount", "name": "count" }]
}
```

---

**End of Part 4: Data Reference**

You now have complete reference for all 86 default parameters and understand how to use custom parameters.

**Next:** Part 5 will cover dashboard vs API conversion and troubleshooting.

---

# Part 5: Dashboard vs API & Troubleshooting

## 5.1 Understanding Dashboard Magic

Dashboard queries in TelemetryDeck use **Ember.js placeholders** that don't work directly in API calls. This section explains how to convert them.

### Common Dashboard Values

#### 1. `this.dataSource` (Computed Property)

**Dashboard:** Automatically computed based on organization settings
**For API:** Use `"telemetry-signals"` or your namespace

```json
{ "dataSource": "telemetry-signals" }
```

#### 2. `this.args.appID` (Injected App ID)

**Dashboard:** Automatically injected from UI context
**For API:** Replace with your actual app UUID

```json
{ "appID": "12345678-1234-1234-1234-123456789012" }
```

**Where to find your app ID:**

- Dashboard → App Settings → Setup tab
- Look for "App ID" field

#### 3. `baseFilters: 'thisApp'` (Magic String)

**Dashboard:** Expands to app + test mode filters automatically
**For API:** Keep as `"thisApp"` AND provide `appID`

```json
{
  "baseFilters": "thisApp",
  "appID": "12345678-1234-1234-1234-123456789012"
}
```

**What it does:**

- Filters to your app (using appID)
- Filters based on test mode toggle in dashboard

#### 4. `this.globalState.testMode` (Test Mode Toggle)

**Dashboard:** Reads from global UI toggle
**For API:** Explicit filter if needed

```json
{
  "filter": {
    "type": "selector",
    "dimension": "isTestMode",
    "value": "false"
  }
}
```

#### 5. `relativeIntervals` (Works in API!)

**Dashboard format works directly in API:**

```json
{
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "day",
        "offset": -30,
        "position": "beginning"
      },
      "endDate": { "component": "day", "offset": 0, "position": "end" }
    }
  ]
}
```

**Alternative - absolute intervals:**

```json
{
  "intervals": ["2025-01-01T00:00:00Z/2025-01-31T23:59:59Z"]
}
```

#### 6. `this.intl.t(...)` (Translations)

**Dashboard:** Dynamic translated strings
**For API:** Use plain strings

```json
{ "name": "Monday" }
```

Instead of: `this.intl.t("data.calendar.weekday.monday")`

---

## 5.2 Converting Dashboard to API

### Complete Conversion Example

**Dashboard Query:**

```javascript
{
  dataSource: this.dataSource,
  queryType: 'timeseries',
  granularity: 'day',
  appID: this.args.appID,
  baseFilters: 'thisApp',
  aggregations: [{type: 'userCount', name: 'Users'}],
  relativeIntervals: [{
    beginningDate: {component: 'day', offset: -30, position: 'beginning'},
    endDate: {component: 'day', offset: 0, position: 'end'}
  }]
}
```

**API-Ready Query:**

```json
{
  "dataSource": "telemetry-signals",
  "queryType": "timeseries",
  "granularity": "day",
  "appID": "12345678-1234-1234-1234-123456789012",
  "baseFilters": "thisApp",
  "aggregations": [{ "type": "userCount", "name": "Users" }],
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "day",
        "offset": -30,
        "position": "beginning"
      },
      "endDate": { "component": "day", "offset": 0, "position": "end" }
    }
  ]
}
```

### Conversion Checklist

When converting dashboard query to API:

- [ ] Replace `this.dataSource` → `"telemetry-signals"`
- [ ] Replace `this.args.appID` → Your actual app UUID
- [ ] Keep `baseFilters: "thisApp"` (and ensure appID is set)
- [ ] Keep `relativeIntervals` as-is (or convert to absolute)
- [ ] Replace `this.intl.t(...)` → Plain strings
- [ ] Remove JavaScript syntax (single quotes → double quotes)
- [ ] Remove trailing commas (not valid in JSON)
- [ ] Ensure all keys and strings use double quotes

### Common Pitfalls

**❌ Missing appID:**

```json
{
  "baseFilters": "thisApp"
  // ERROR: appID required!
}
```

**✅ Include appID:**

```json
{
  "baseFilters": "thisApp",
  "appID": "12345678-1234-1234-1234-123456789012"
}
```

**❌ JavaScript syntax:**

```javascript
{
  dataSource: 'telemetry-signals',  // Single quotes, no quotes on key
}
```

**✅ Valid JSON:**

```json
{
  "dataSource": "telemetry-signals"
}
```

---

## 5.3 Troubleshooting Guide

### Query Returns No Results

**Symptom:** Query executes but returns empty array `[]`

#### Check 1: Time Range

**Most common issue** - time range doesn't match data.

```json
{
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "day",
        "offset": -30,
        "position": "beginning"
      },
      "endDate": { "component": "day", "offset": 0, "position": "end" }
    }
  ]
}
```

**Solutions:**

- Verify you have data in the last 30 days
- Try wider range: `offset: -365` for last year
- Check dashboard to see when data exists
- Omit `relativeIntervals` to use dashboard's range

#### Check 2: Filters Too Restrictive

**Problem:** Filter excludes all data

```json
{
  "filter": {
    "type": "selector",
    "dimension": "TelemetryDeck.Device.platform",
    "value": "iOS" // Maybe no iOS users?
  }
}
```

**Solutions:**

- Remove filter temporarily to see if data appears
- Check filter values match exactly (case-sensitive!)
- Try TopN query to see what values exist:

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 10,
  "dimension": {
    "type": "default",
    "dimension": "TelemetryDeck.Device.platform",
    "outputName": "Platform"
  },
  "metric": { "type": "numeric", "metric": "count" },
  "aggregations": [{ "type": "userCount", "name": "count" }]
}
```

#### Check 3: Signal Type Spelling

**Problem:** Signal type doesn't exist or is misspelled

```json
{
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "Purchase.completed" // Check spelling!
  }
}
```

**Solutions:**

- Query all signal types (see Part 4.4)
- Check exact capitalization
- Verify signal type exists in your app

#### Check 4: Wrong App ID

```json
{
  "appID": "wrong-uuid",
  "baseFilters": "thisApp"
}
```

**Solutions:**

- Double-check app ID from dashboard
- Try without appID to see if data appears (will show all apps)

---

### Validation Errors

**Symptom:** Query rejected with error message

#### Error: "Missing required field"

**Message:** `"aggregations is required"`

**Solution:** Every query needs at least one aggregation:

```json
{
  "aggregations": [{ "type": "userCount", "name": "Users" }]
}
```

#### Error: "Invalid aggregation combination"

**Message:** `"histogram requires range filter"`

**Solution:** Histogram MUST have matching range filter:

```json
{
  "aggregations": [
    {
      "type": "histogram",
      "name": "Duration",
      "fieldName": "TelemetryDeck.Signal.durationInSeconds",
      "splitPoints": [0, 5, 10, 30]
    }
  ],
  "filter": {
    "type": "range",
    "column": "TelemetryDeck.Signal.durationInSeconds",
    "matchValueType": "DOUBLE",
    "lower": "0",
    "upper": "30"
  }
}
```

#### Error: "Post-aggregation references unknown field"

**Message:** `"Field 'Revenue' not found"`

**Solution:** postAggregations must reference aggregation names:

```json
{
  "aggregations": [
    { "type": "doubleSum", "name": "_Revenue", "fieldName": "floatValue" }
  ],
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "Total",
      "fn": "+",
      "fields": [
        { "type": "fieldAccess", "fieldName": "_Revenue" } // Must match name above
      ]
    }
  ]
}
```

#### Error: "Invalid filter syntax"

**Message:** `"field is required for 'not' filter"`

**Solution:** `not` filter needs nested `field`:

```json
{
  "type": "not",
  "field": {
    // Don't forget 'field'!
    "type": "selector",
    "dimension": "isBot",
    "value": "True"
  }
}
```

---

### Dashboard Query Doesn't Work in API

**Symptom:** Copied from dashboard, but fails in API

#### Missing Conversion

**Check:**

- [ ] Replaced `this.dataSource`?
- [ ] Replaced `this.args.appID`?
- [ ] Kept `baseFilters: "thisApp"` with appID?
- [ ] Removed JavaScript syntax?
- [ ] Valid JSON (double quotes, no trailing commas)?

#### Common Dashboard-Only Features

**These don't work in API:**

- `chartConfiguration` - UI display settings
- `colorScheme` - Chart colors
- `displayMode` - Chart type
- `isExpanded` - UI state

**Solution:** Remove these fields for API use.

---

### Performance Issues

**Symptom:** Query times out or runs very slowly

#### Issue 1: Too Many Dimensions in GroupBy

```json
{
  "queryType": "groupBy",
  "dimensions": [
    "platform",
    "version",
    "modelName",
    "region",
    "hour" // 5 dimensions = thousands of combinations!
  ]
}
```

**Solution:** Limit to 2-3 dimensions max

#### Issue 2: Very Wide Time Range + High Granularity

```json
{
  "granularity": "hour",
  "relativeIntervals": [
    {
      "beginningDate": { "component": "year", "offset": -5 } // 5 years hourly!
    }
  ]
}
```

**Solution:** Use coarser granularity for long ranges:

- Last year hourly → Use "day"
- Last 5 years → Use "month" or "quarter"

#### Issue 3: Too Many Results in TopN

```json
{
  "queryType": "topN",
  "threshold": 10000 // Too many!
}
```

**Solution:** Limit threshold:

- Typical: 10-50
- Max recommended: 100

---

### Common Mistakes Reference

#### Using count instead of userCount

**❌ Wrong:**

```json
{ "type": "count", "name": "Users" }
```

**✅ Correct:**

```json
{ "type": "userCount", "name": "Users" }
```

**Why:** `count` counts rows, not unique users

#### Forgetting fieldName for doubleSum

**❌ Wrong:**

```json
{ "type": "doubleSum", "name": "Revenue" }
```

**✅ Correct:**

```json
{ "type": "doubleSum", "name": "Revenue", "fieldName": "floatValue" }
```

#### Using bound filters (deprecated)

**❌ Wrong:**

```json
{ "type": "bound", "dimension": "age", "lower": "18" }
```

**✅ Correct:**

```json
{ "type": "range", "column": "age", "matchValueType": "DOUBLE", "lower": "18" }
```

#### Incorrect Filter Nesting

**❌ Wrong:**

```json
{
  "type": "and",
  "filters": [...]  // Wrong key!
}
```

**✅ Correct:**

```json
{
  "type": "and",
  "fields": [...]  // Correct key
}
```

#### Wrong fieldAccess Type

**❌ Wrong (for thetaSketch):**

```json
{
  "postAggregations": [
    {
      "type": "arithmetic",
      "fn": "/",
      "fields": [
        { "type": "fieldAccess", "fieldName": "_Users" } // Wrong!
      ]
    }
  ]
}
```

**✅ Correct:**

```json
{
  "postAggregations": [
    {
      "type": "arithmetic",
      "fn": "/",
      "fields": [
        { "type": "finalizingFieldAccess", "fieldName": "_Users" } // Correct!
      ]
    }
  ]
}
```

**Rule:** Use `finalizingFieldAccess` for `thetaSketch`, `fieldAccess` for everything else

---

**End of Part 5: Dashboard vs API & Troubleshooting**

You now understand how to convert dashboard queries to API and troubleshoot common issues.

**Next:** Part 6 will cover advanced techniques including special calculations, lookups, and performance optimization.

---

# Part 6: Advanced Techniques

## 6.1 Special Calculations

### averageSessionSeconds Calculation Logic

The `TelemetryDeck.Retention.averageSessionSeconds` parameter has **special calculation logic** that differs from a simple average:

**How it works:**

- **0 sessions:** Returns `-1`
- **1 session (current):** Returns current session duration (NOT -1!)
- **2+ sessions:** Returns average of all COMPLETED sessions (excludes current)

**Example:**

User has 3 sessions: 60s, 120s, and currently active for 30s.

- Result: `(60 + 120) / 2 = 90` seconds
- Current 30s session NOT included in average

**Why this matters:**

- Filter for `-1` to find users with no completed sessions
- Don't be surprised if 1-session users show duration (not -1)
- Active session excluded from average (prevents skewing by incomplete sessions)

**Query example - users with no sessions:**

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 100,
  "dimension": {
    "type": "default",
    "dimension": "clientUser",
    "outputName": "User"
  },
  "metric": { "type": "numeric", "metric": "count" },
  "aggregations": [{ "type": "userCount", "name": "count" }],
  "filter": {
    "type": "selector",
    "dimension": "TelemetryDeck.Retention.averageSessionSeconds",
    "value": "-1"
  }
}
```

---

## 6.2 Workarounds & Tips

### Hide Aggregators with `_` Prefix

**Problem:** Internal aggregator shows in chart legend

**Solution:** Prefix aggregation name with `_`

**Example:**

```json
{
  "aggregations": [
    {
      "type": "thetaSketch",
      "name": "_Total_Users",  // Hidden from UI
      "fieldName": "clientUser"
    },
    {
      "type": "filtered",
      "name": "_New_Filtered",  // Hidden from UI
      "filter": {...},
      "aggregator": {
        "type": "thetaSketch",
        "name": "New Users",  // Shown in UI (inside filtered aggregator)
        "fieldName": "clientUser"
      }
    }
  ]
}
```

**Result:** Only "New Users" appears in legend, "\_Total_Users" and "\_New_Filtered" hidden.

### Line vs Bar Chart Selection

**When to use Line charts:**

- Cumulative metrics over time
- Continuous trends
- Multiple series comparison
- When zero values should show as zero (not gaps)

**When to use Bar charts:**

- Discrete counts or events
- Period comparisons (month-over-month)
- When emphasizing individual data points
- Clearer for sparse data

**Examples:**

- **Line:** Daily active users, cumulative revenue, retention rates
- **Bar:** Monthly new users, quarterly revenue, version adoption

**Note:** This is a UI setting (`displayMode`), not part of TQL query.

---

## 6.3 Bot Detection & Data Quality

### Exclude Bots

**Most important filter** for clean analytics:

```json
{
  "filter": {
    "type": "selector",
    "dimension": "isBot",
    "value": "False"
  }
}
```

**Why:**

- `isBot` uses user agent + heuristics
- Excludes web scrapers, crawlers, automated tools
- Essential for accurate user metrics

### Exclude Test Data

```json
{
  "filter": {
    "type": "selector",
    "dimension": "isTestMode",
    "value": "false"
  }
}
```

**Or use `baseFilters: "thisApp"`** which respects dashboard test mode toggle.

### Exclude Empty Values

**Method 1 - regex filter:**

```json
{
  "filter": {
    "type": "regex",
    "dimension": "TelemetryDeck.UserPreference.region",
    "pattern": ".+"
  }
}
```

**Method 2 - not + selector:**

```json
{
  "filter": {
    "type": "not",
    "field": {
      "type": "selector",
      "dimension": "TelemetryDeck.UserPreference.region",
      "value": ""
    }
  }
}
```

### Combined Data Quality Filter

**Exclude bots + empty regions:**

```json
{
  "filter": {
    "type": "and",
    "fields": [
      {
        "type": "selector",
        "dimension": "isBot",
        "value": "False"
      },
      {
        "type": "not",
        "field": {
          "type": "selector",
          "dimension": "TelemetryDeck.UserPreference.region",
          "value": ""
        }
      }
    ]
  }
}
```

---

## 6.4 Registered Lookups

TelemetryDeck provides built-in lookup tables for value transformation.

### appleModelNames

**Purpose:** Convert Apple model identifiers to friendly names

**Transforms:** `"iPhone14,2"` → `"iPhone 13 Pro"`

**Usage:**

```json
{
  "dimension": {
    "type": "extraction",
    "dimension": "TelemetryDeck.Device.modelName",
    "outputName": "Device",
    "extractionFn": {
      "type": "registeredLookup",
      "lookup": "appleModelNames",
      "retainMissingValue": true
    }
  }
}
```

**Complete TopN example:**

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 50,
  "dimension": {
    "type": "extraction",
    "dimension": "TelemetryDeck.Device.modelName",
    "outputName": "Device Model",
    "extractionFn": {
      "type": "registeredLookup",
      "lookup": "appleModelNames",
      "retainMissingValue": true
    }
  },
  "metric": { "type": "numeric", "metric": "count" },
  "aggregations": [{ "type": "userCount", "name": "count" }],
  "baseFilters": "thisApp",
  "appID": null
}
```

### deviceType

**Purpose:** Categorize devices by type

**Transforms:** `"iPhone14,2"` → `"Phone"`, `"iPad13,1"` → `"Tablet"`

**Usage:**

```json
{
  "extractionFn": {
    "type": "registeredLookup",
    "lookup": "deviceType",
    "retainMissingValue": true
  }
}
```

**Results:** Phone, Tablet, Desktop, Watch, TV, etc.

### processorFamily & processorType

**Purpose:** CPU information

**Less commonly used** - for hardware analysis.

```json
{
  "extractionFn": {
    "type": "registeredLookup",
    "lookup": "processorFamily",
    "retainMissingValue": true
  }
}
```

### retainMissingValue

**Always use `true`** to keep unknown values:

```json
{
  "retainMissingValue": true // Keeps original if not in lookup
}
```

**If `false`:** Unknown models show as null/empty.

---

## 6.5 Version Sorting

**Problem:** Version "10.0.0" comes before "2.0.0" alphabetically

**Solution:** Use `ordering: "version"` in TopN metric

### Version-Sorted TopN

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 20,
  "dimension": {
    "type": "default",
    "dimension": "TelemetryDeck.AppInfo.version",
    "outputName": "App Version"
  },
  "metric": {
    "type": "dimension",
    "ordering": "version" // Semantic version sort
  },
  "aggregations": [{ "type": "userCount", "name": "count" }],
  "baseFilters": "thisApp",
  "appID": null
}
```

**Results ordered:** 10.0.0, 9.1.2, 9.1.1, 9.0.0, 2.5.0, 2.1.0, 1.0.0

**Without version ordering:** 10.0.0, 2.5.0, 2.1.0, 9.1.2, 9.1.1, 9.0.0, 1.0.0 (wrong!)

**When to use:**

- `TelemetryDeck.AppInfo.version` (app versions)
- `TelemetryDeck.Device.systemVersion` (OS versions)
- Any semantic version strings

**Alternative orderings:**

- `"lexicographic"` - Alphabetical A-Z
- `"alphanumeric"` - Alphanumeric sort
- `"numeric"` - Numeric values
- `"strlen"` - By string length

---

## 6.6 Complex Filter Patterns

### Nested AND/OR/NOT

**Combine multiple conditions** for precise filtering:

```json
{
  "filter": {
    "type": "and",
    "fields": [
      {
        "type": "or",
        "fields": [
          {
            "type": "selector",
            "dimension": "TelemetryDeck.Device.platform",
            "value": "iOS"
          },
          {
            "type": "selector",
            "dimension": "TelemetryDeck.Device.platform",
            "value": "iPadOS"
          }
        ]
      },
      {
        "type": "not",
        "field": {
          "type": "selector",
          "dimension": "isBot",
          "value": "True"
        }
      },
      {
        "type": "range",
        "column": "TelemetryDeck.Retention.totalSessionsCount",
        "matchValueType": "DOUBLE",
        "lower": "5"
      }
    ]
  }
}
```

**This filter means:**

- (iOS OR iPadOS)
- AND not a bot
- AND has 5+ sessions

### Multiple Signal Types (Purchase Events)

```json
{
  "filter": {
    "type": "in",
    "dimension": "type",
    "values": [
      "Purchase.completed",
      "Purchase.restored",
      "RevenueCat.Events.RENEWAL"
    ]
  }
}
```

### Power Users (High Engagement)

```json
{
  "filter": {
    "type": "and",
    "fields": [
      {
        "type": "range",
        "column": "TelemetryDeck.Retention.totalSessionsCount",
        "matchValueType": "DOUBLE",
        "lower": "10"
      },
      {
        "type": "range",
        "column": "TelemetryDeck.Retention.distinctDaysUsed",
        "matchValueType": "DOUBLE",
        "lower": "7"
      }
    ]
  }
}
```

**Meaning:** 10+ sessions AND used on 7+ different days

---

## 6.7 Performance Optimization

### Best Practices

#### 1. Use Appropriate Data Source

- **`telemetry-signals`** - Default, works for all queries
- **`com.telemetrydeck.all`** - Optimized for default parameters only
- **Custom namespace** - Best performance for your organization

```json
{
  "dataSource": "your-namespace.cns" // Best performance
}
```

#### 2. Limit Time Ranges

**Don't query years of data** when you need days:

**❌ Bad:**

```json
{
  "relativeIntervals": [
    {
      "beginningDate": { "component": "year", "offset": -5 }
    }
  ]
}
```

**✅ Good:**

```json
{
  "relativeIntervals": [
    {
      "beginningDate": { "component": "day", "offset": -30 }
    }
  ]
}
```

#### 3. Use Coarser Granularity for Long Ranges

**❌ Bad:**

```json
{
  "granularity": "hour", // 43,800 data points!
  "relativeIntervals": [
    {
      "beginningDate": { "component": "year", "offset": -5 }
    }
  ]
}
```

**✅ Good:**

```json
{
  "granularity": "month", // 60 data points
  "relativeIntervals": [
    {
      "beginningDate": { "component": "year", "offset": -5 }
    }
  ]
}
```

#### 4. Limit TopN Threshold

**Typical:** 10-20
**Max recommended:** 100

```json
{
  "threshold": 20 // Not 1000!
}
```

#### 5. Minimize GroupBy Dimensions

**❌ Bad:**

```json
{
  "queryType": "groupBy",
  "dimensions": ["platform", "version", "region", "hour"] // Thousands of combinations!
}
```

**✅ Good:**

```json
{
  "queryType": "groupBy",
  "dimensions": ["platform", "version"] // Manageable
}
```

#### 6. Use Filters to Reduce Data Volume

**Filter before aggregating:**

```json
{
  "filter": {
    "type": "and",
    "fields": [
      {"type": "selector", "dimension": "isBot", "value": "False"},
      {"type": "selector", "dimension": "isTestMode", "value": "false"}
    ]
  },
  "aggregations": [...]
}
```

#### 7. Prefer `in` Filter Over Multiple `or` Filters

**❌ Less efficient:**

```json
{
  "type": "or",
  "fields": [
    { "type": "selector", "dimension": "platform", "value": "iOS" },
    { "type": "selector", "dimension": "platform", "value": "iPadOS" },
    { "type": "selector", "dimension": "platform", "value": "macOS" }
  ]
}
```

**✅ More efficient:**

```json
{
  "type": "in",
  "dimension": "platform",
  "values": ["iOS", "iPadOS", "macOS"]
}
```

---

**End of Part 6: Advanced Techniques**

You now know special calculations, workarounds, lookups, and performance optimization techniques.

**Next:** Part 7 will provide quick reference tables for rapid lookup.

---

# Part 7: Quick Reference

## 7.1 Query Type Decision Chart

**"What do I want to know?" → Query Type**

| User Question          | Query Type | Typical Use              |
| ---------------------- | ---------- | ------------------------ |
| "X over time"          | Timeseries | Daily active users       |
| "Top N by dimension"   | TopN       | Top 10 platforms         |
| "A → B → C conversion" | Funnel     | Onboarding completion    |
| "How many return"      | Retention  | Monthly cohort retention |
| "Compare A vs B"       | Experiment | A/B test results         |
| "Multiple dimensions"  | GroupBy    | Platform × Version       |
| "Raw event data"       | Scan       | Recent signals list      |

**Complexity Check:**

- 90% of queries → Level 1 (just queryType + granularity + 1 aggregation)
- 7% of queries → Level 2 (+ simple filter)
- 3% of queries → Levels 3-5 (advanced features)

---

## 7.2 All Aggregator Types

| Type                   | Purpose                | fieldName                | Common For            |
| ---------------------- | ---------------------- | ------------------------ | --------------------- |
| `userCount`            | Unique users           | No                       | Daily active users    |
| `eventCount` / `count` | Total signals          | No                       | Event frequency       |
| `doubleSum`            | Sum numeric values     | Required                 | Total revenue         |
| `doubleMean`           | Average numeric values | Required                 | Avg session time      |
| `doubleMin`            | Minimum value          | Required                 | Shortest duration     |
| `doubleMax`            | Maximum value          | Required                 | Longest duration      |
| `thetaSketch`          | Advanced user counting | `"clientUser"`           | Filtered aggregations |
| `histogram`            | Distribution buckets   | Required + `splitPoints` | Duration distribution |
| `filtered`             | Aggregation on subset  | No (wraps another agg)   | iOS vs Android        |

**Quick Tips:**

- userCount: Simple unique user counts
- thetaSketch: When you need filtered aggregations or postAgg set operations
- doubleSum: Always use `fieldName: "floatValue"` for revenue
- histogram: MUST have matching range filter

---

## 7.3 All Filter Types

| Type       | Purpose           | Syntax                                                                | Use When                  |
| ---------- | ----------------- | --------------------------------------------------------------------- | ------------------------- |
| `selector` | Exact match       | `{type: "selector", dimension: "X", value: "Y"}`                      | Most common (90%)         |
| `in`       | Match any in list | `{type: "in", dimension: "X", values: [...]`                          | OR logic (same dimension) |
| `range`    | Numeric range     | `{type: "range", column: "X", matchValueType: "DOUBLE", lower/upper}` | Duration, count filters   |
| `regex`    | Pattern match     | `{type: "regex", dimension: "X", pattern: "..."}`                     | Exclude empty (.+)        |
| `and`      | All must match    | `{type: "and", fields: [...]}`                                        | Multiple conditions       |
| `or`       | Any must match    | `{type: "or", fields: [...]}`                                         | Alternative conditions    |
| `not`      | Exclude matches   | `{type: "not", field: {...}}`                                         | Exclude bots, empty       |

**Common Patterns:**

**Exclude bots:**

```json
{ "type": "selector", "dimension": "isBot", "value": "False" }
```

**iOS or Android:**

```json
{
  "type": "in",
  "dimension": "TelemetryDeck.Device.platform",
  "values": ["iOS", "Android"]
}
```

**10+ sessions:**

```json
{
  "type": "range",
  "column": "TelemetryDeck.Retention.totalSessionsCount",
  "matchValueType": "DOUBLE",
  "lower": "10"
}
```

---

## 7.4 All Granularities

| Granularity | Use Case             | Typical Interval  | Data Points (30 days) |
| ----------- | -------------------- | ----------------- | --------------------- |
| `"hour"`    | Real-time, intraday  | Last 24-48 hours  | 720                   |
| `"day"`     | Daily trends         | Last 30-90 days   | 30                    |
| `"week"`    | Weekly trends        | Last 12 weeks     | 4-5                   |
| `"month"`   | Monthly aggregations | Last 12 months    | 1                     |
| `"quarter"` | Quarterly reports    | Last 4-8 quarters | 0-1                   |
| `"year"`    | Annual trends        | Multiple years    | 0                     |
| `"all"`     | Single total         | Any               | 1                     |

**When to use "all":**

- TopN queries
- Funnel queries
- Histogram queries
- Single-value results

---

## 7.5 Common Dimensions

### Device & Platform

| Dimension                            | Values                    | Use                                             |
| ------------------------------------ | ------------------------- | ----------------------------------------------- |
| `TelemetryDeck.Device.platform`      | iOS, Android, macOS, etc. | Platform breakdown                              |
| `TelemetryDeck.Device.modelName`     | iPhone14,2, etc.          | Device models (use with appleModelNames lookup) |
| `TelemetryDeck.Device.systemVersion` | 16.1, 15.7, etc.          | OS version                                      |
| `TelemetryDeck.Device.orientation`   | portrait, landscape       | Orientation                                     |

### App Information

| Dimension                           | Values             | Use                   |
| ----------------------------------- | ------------------ | --------------------- |
| `TelemetryDeck.AppInfo.version`     | 2.1.0, 1.5.3, etc. | App version breakdown |
| `TelemetryDeck.AppInfo.buildNumber` | 123, 456, etc.     | Build tracking        |

### User Preferences

| Dimension                                  | Values           | Use                |
| ------------------------------------------ | ---------------- | ------------------ |
| `TelemetryDeck.UserPreference.region`      | US, DE, GB, etc. | Country breakdown  |
| `TelemetryDeck.UserPreference.language`    | en, de, fr, etc. | Language breakdown |
| `TelemetryDeck.UserPreference.colorScheme` | dark, light      | Dark mode usage    |

### Context

| Dimension                          | Values            | Use                      |
| ---------------------------------- | ----------------- | ------------------------ |
| `TelemetryDeck.Calendar.dayOfWeek` | 1-7 (Mon-Sun)     | Weekday analysis         |
| `TelemetryDeck.Calendar.hourOfDay` | 1-24              | Time-of-day patterns     |
| `type`                             | Your signal types | Event filtering/grouping |

### Web Analytics

| Dimension     | Values                  | Use               |
| ------------- | ----------------------- | ----------------- |
| `isBot`       | True, False             | Bot detection     |
| `browserName` | Chrome, Safari, etc.    | Browser breakdown |
| `path`        | /about, /products, etc. | Page breakdown    |

---

## 7.6 Registered Lookups

| Lookup Name       | Purpose         | Example                    |
| ----------------- | --------------- | -------------------------- |
| `appleModelNames` | Device names    | iPhone14,2 → iPhone 13 Pro |
| `deviceType`      | Device category | iPhone14,2 → Phone         |
| `processorFamily` | CPU family      | Value → Family name        |
| `processorType`   | CPU type        | Value → Type name          |

**Usage:**

```json
{
  "dimension": {
    "type": "extraction",
    "dimension": "TelemetryDeck.Device.modelName",
    "outputName": "Device",
    "extractionFn": {
      "type": "registeredLookup",
      "lookup": "appleModelNames",
      "retainMissingValue": true
    }
  }
}
```

---

## 7.7 Common Patterns Cheat Sheet

### Daily Active Users

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [{ "type": "userCount", "name": "Users" }],
  "baseFilters": "thisApp",
  "appID": "YOUR-APP-ID"
}
```

### iOS Users Per Day

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [{ "type": "userCount", "name": "iOS Users" }],
  "filter": {
    "type": "selector",
    "dimension": "TelemetryDeck.Device.platform",
    "value": "iOS"
  },
  "baseFilters": "thisApp",
  "appID": "YOUR-APP-ID"
}
```

### Monthly Revenue

```json
{
  "queryType": "timeseries",
  "granularity": "month",
  "aggregations": [
    { "type": "doubleSum", "name": "Revenue", "fieldName": "floatValue" }
  ],
  "filter": {
    "type": "selector",
    "dimension": "type",
    "value": "Purchase.completed"
  },
  "baseFilters": "thisApp",
  "appID": "YOUR-APP-ID",
  "valueFormatter": {
    "options": {
      "style": "currency",
      "currency": "USD",
      "minimumFractionDigits": 2
    },
    "locale": "en-US"
  }
}
```

### Top 10 Platforms

```json
{
  "queryType": "topN",
  "granularity": "all",
  "threshold": 10,
  "dimension": {
    "type": "default",
    "dimension": "TelemetryDeck.Device.platform",
    "outputName": "Platform"
  },
  "metric": { "type": "numeric", "metric": "count" },
  "aggregations": [{ "type": "userCount", "name": "count" }],
  "baseFilters": "thisApp",
  "appID": "YOUR-APP-ID"
}
```

### Onboarding Funnel

```json
{
  "queryType": "funnel",
  "granularity": "all",
  "steps": [
    {
      "name": "Launched",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "App.launched"
      }
    },
    {
      "name": "Tutorial Started",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Tutorial.started"
      }
    },
    {
      "name": "Tutorial Completed",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "Tutorial.completed"
      }
    }
  ],
  "baseFilters": "thisApp",
  "appID": "YOUR-APP-ID"
}
```

### Monthly Retention

```json
{
  "queryType": "retention",
  "granularity": "month",
  "relativeIntervals": [
    {
      "beginningDate": {
        "component": "month",
        "offset": -6,
        "position": "beginning"
      },
      "endDate": { "component": "month", "offset": 0, "position": "end" }
    }
  ],
  "baseFilters": "thisApp",
  "appID": "YOUR-APP-ID"
}
```

### New User Percentage

```json
{
  "queryType": "timeseries",
  "granularity": "day",
  "aggregations": [
    { "type": "thetaSketch", "name": "_Total", "fieldName": "clientUser" },
    {
      "type": "filtered",
      "name": "_New_Filtered",
      "filter": {
        "type": "selector",
        "dimension": "type",
        "value": "TelemetryDeck.Acquisition.newInstallDetected"
      },
      "aggregator": {
        "type": "thetaSketch",
        "name": "_New",
        "fieldName": "clientUser"
      }
    }
  ],
  "postAggregations": [
    {
      "type": "arithmetic",
      "name": "New User %",
      "fn": "/",
      "fields": [
        { "type": "finalizingFieldAccess", "fieldName": "_New" },
        { "type": "finalizingFieldAccess", "fieldName": "_Total" }
      ]
    }
  ],
  "baseFilters": "thisApp",
  "appID": "YOUR-APP-ID",
  "valueFormatter": {
    "options": { "style": "percent", "minimumFractionDigits": 1 }
  }
}
```

---

## 7.8 Field Quick Lookup

### Required vs Optional by Query Type

**Timeseries:**

- Required: `queryType`, `granularity`, `aggregations`
- Optional: `filter`, `relativeIntervals`, `postAggregations`, `valueFormatter`
- For API: `baseFilters`, `appID`

**TopN:**

- Required: `queryType`, `granularity`, `threshold`, `dimension`, `metric`, `aggregations`
- Optional: `filter`
- For API: `baseFilters`, `appID`

**Funnel:**

- Required: `queryType`, `granularity`, `steps` (array of 2+ steps)
- Optional: `filter`, `relativeIntervals`
- For API: `baseFilters`, `appID`

**Retention:**

- Required: `queryType`, `granularity`, `relativeIntervals`
- Optional: None
- For API: `baseFilters`, `appID`

### Common Field Values

**dataSource:**

- `"telemetry-signals"` - Default
- `"com.telemetrydeck.all"` - Optimized for default parameters
- `"your-namespace.cns"` - Custom namespace (best performance)

**baseFilters:**

- `"thisApp"` - Filter to one app (requires appID)
- `"thisOrganization"` - All apps in organization

**granularity:**

- `"hour"`, `"day"`, `"week"`, `"month"`, `"quarter"`, `"year"`, `"all"`

---

**End of Part 7: Quick Reference**

---

# Final Notes

## File Purpose

This file contains everything needed to understand and use TQL (TelemetryDeck Query Language):

✅ **7 query types** with 30+ complete examples
✅ **All aggregators, filters, and components** with syntax
✅ **All 86 default parameters** documented
✅ **Dashboard to API conversion** guide
✅ **Troubleshooting** for common issues
✅ **Advanced techniques** (lookups, performance, special calculations)
✅ **Quick reference** tables for rapid lookup

## How to Use This Guide

1. **First time?** Read Part 1 (Introduction)
2. **Building a query?** Check Part 2.1 decision guide → go to specific query type
3. **Looking up syntax?** Jump to Part 3 (Components) or Part 7 (Quick Reference)
4. **Understanding data?** See Part 4 (all 86 parameters)
5. **Query not working?** Check Part 5 (Troubleshooting)
6. **Advanced features?** Browse Part 6

## For AI Chatbots

This file is optimized for AI assistance with TQL queries. When helping users:

- **Ask about their goal** first → Use Part 2.1 to pick query type
- **Start simple** → 90% of queries are Level 1 (see complexity guidance)
- **Reference examples** → Part 2 has 30+ complete working queries
- **Check parameters** → Part 4 lists all 86 default parameters available
- **Debug issues** → Part 5.3 covers common problems and solutions

## Version Information

**TQL-Guideline.md Version:** 1.0
**Created:** 2025-11-07
**Based On:** TQLDocumentation.md, TQL-RAG-Complete.md, BuiltInCharts.md, JSONSchema-AI-Complete.md

**Coverage:** Complete reference for all TQL features, 86 default parameters, 30+ examples

---

**End of TQL Complete Reference & Guide**
