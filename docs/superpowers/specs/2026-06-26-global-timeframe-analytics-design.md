# DaySurp Global Timeframe Analytics Design

## Goal

Make the analytics screen fully coherent with the active visible period so that every displayed value and chart is calculated only from the selected timeframe.

## Problem Summary

The app already exposes a global time window through `daysToDisplay`, but analytics are only partially aligned with that filter.

Today this creates a misleading experience:

- some charts are based on filtered data
- some summary values still reflect all-time data
- the UI text includes wording such as `To Date`, which implies cumulative behavior even when the user expects period-scoped analytics

The requested product behavior is stricter:

- changing the active period must recalculate all analytics as if the analysis were performed only inside that timeframe

## Scope

This design covers:

- analytics filtering on the chart screen
- summary metrics shown above charts
- line chart data points
- expense and income pie charts
- ranked analytics such as top expense descriptions and top expense days
- wording updates needed to reflect period-scoped behavior
- targeted regression tests for timeframe coherence

This design does not cover:

- backup format changes
- transaction migration
- changes to the history retention policy
- destructive reinstall or data reset flows

## Current State

- `daysToDisplay` is stored in `CounterDataRepository` and already acts as the user-visible period control
- `ChartViewModel` filters snapshots and transactions into `relevantSnapshots` and `relevantTransactions`
- several derived analytics already use those filtered lists
- at least one key summary metric, the saving ratio, is still calculated from unfiltered full-history inputs
- some labels still suggest cumulative semantics rather than selected-period semantics

## Desired Behavior

When the user changes the visible period:

1. all analytics on the chart screen must update
2. every computed metric must be derived only from data inside the active period
3. no value on that screen may silently mix filtered and all-time data
4. if the filtered period has insufficient data, the screen must show safe zero or empty states rather than fallback to all-time values

In practice this means the selected timeframe becomes the single source of truth for analytics aggregation.

## Data And Aggregation Design

### Single filtered dataset

`ChartViewModel` shall build one filtered analytics dataset based on:

- active `daysToDisplay`
- full stored snapshots
- full stored transactions
- selected visual grouping (`Day`, `Week`, `Month`, `Year`)

The filtering boundary remains:

- include only records whose timestamp is within the last `daysToDisplay` days from the current time

This filtered dataset is then used as the input for every downstream chart and metric on the analytics screen.

### Metrics that must become timeframe-scoped

The following values shall be computed only from the filtered dataset:

- total expenses
- total income
- average surplus
- surplus standard deviation
- saving ratio
- expense category totals and shares
- income category totals and shares
- top expense descriptions
- top expense days

### Saving ratio definition

The saving ratio shown on the analytics screen shall be calculated only from filtered data.

It shall no longer consume full-history snapshots or transactions when a timeframe is active.

If the filtered period does not provide enough information to compute a meaningful ratio, the app shall return `0.0` rather than reuse all-time values.

## UI Design

### Summary labels

Any label that implies cumulative semantics shall be updated to match the new behavior.

Recommended change:

- replace `Saving Ratio (Surplus/Total Income To Date)` with wording that clearly refers to the selected period

Examples of acceptable wording:

- `Saving Ratio (Selected Period)`
- `Saving Ratio In Selected Period`

The exact phrasing can remain concise, but it must not imply all-time aggregation.

### Empty and sparse states

If the selected period has:

- no snapshots
- no transactions
- only one snapshot

the screen shall remain stable and show zero or empty analytics where needed.

It must not crash, fabricate values, or fall back to unrelated historical totals.

## Data Safety

This change is read-only with respect to persisted business data.

It shall not:

- delete transactions
- delete snapshots
- change backup schema
- require clearing app storage
- require reinstalling the app

The implementation only changes how existing stored data is filtered and aggregated for display.

## Testing Strategy

Use focused regression coverage rather than broad UI-heavy tests.

Recommended checks:

- a chart-viewmodel test where changing `daysToDisplay` changes totals, ratio, and ranked analytics together
- a saving-ratio test proving the selected period does not read full-history inputs
- an empty-period test confirming safe zero or empty outputs
- a sparse-data test confirming one-snapshot periods do not crash and remain coherent

## Implementation Notes

Likely touched files:

- `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
- `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`

## Recommendation

Implement the fix by centralizing period filtering in `ChartViewModel` and ensuring every analytics output is derived from the same filtered inputs.

This is the safest approach because it:

- preserves all stored data
- avoids repository/schema churn
- makes future analytics features less likely to drift out of sync with the selected period
