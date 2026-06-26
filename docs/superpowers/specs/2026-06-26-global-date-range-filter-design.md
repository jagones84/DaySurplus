# DaySurp Global Date Range Filter Design

## Goal

Introduce a single global date range filter selected in Home through `Da` and `A` pickers.

That range becomes the only visible dataset for the app UI:

- history list
- grouped transactions
- chart totals
- saving ratio
- category pie charts
- ranked analytics

This change must be read-only with respect to stored business data. It filters what the user sees, but it must not delete or rewrite historical transactions or snapshots.

## Problem Summary

The current branch partially improves timeframe coherence on the chart screen, but the product model is still wrong at the foundation level.

Today there are two different concepts mixed together:

- a global data window driven by `daysToDisplay`
- a chart grouping selector `Daily / Weekly / Monthly / Yearly`

Those are not the same thing.

The visible result is confusing:

- the chart screen suggests a timeframe selector exists
- the selector only changes chart aggregation granularity
- the base dataset still comes from a separate hidden rule
- users cannot define a real analysis range such as `from 01/06 to 30/06`

The requested product behavior is stricter:

- the user chooses `start date` and `end date` in Home
- that date range becomes the base dataset for the entire app
- every downstream number and visualization must behave as if only the filtered records exist
- stored data must remain intact

## Scope

This design covers:

- a global `Da/A` filter in Home
- persistence of the selected date range
- shared filtering logic for transactions and snapshots
- history list and grouped history coherence
- chart and analytics coherence
- distinction between dataset range and chart grouping
- safe empty states when the range has no data
- regression tests for end-to-end filter behavior

This design does not cover:

- backup schema changes
- transaction migration
- retention policy changes
- destructive delete or reset flows
- new analytics unrelated to the selected range

## Product Rules

### Single source of truth

The app shall expose one global visible dataset determined by:

- `filterStartDate`
- `filterEndDate`

Every screen that currently derives values from full transactions or snapshots shall instead derive them from the filtered subset inside that range.

### Inclusive range semantics

The selected range shall be inclusive:

- records on the `Da` date are included
- records on the `A` date are included

Implementation should normalize boundaries to full local-day coverage:

- `Da` uses start-of-day
- `A` uses end-of-day

### No data loss

This feature must not:

- delete transactions
- delete daily snapshots
- rewrite old records only to support filtering
- require app reset
- require reinstall

Filtering is display logic only.

## UX Design

### Home filter card

Home becomes the place where the user defines the global analysis window.

Add a visible filter card above the history and analytics-driving content, containing:

- `Da` field
- `A` field
- `Reset filtro` action

The date controls should open date pickers rather than rely on free-form text entry.

### Default behavior

Recommended default:

- `A = today`
- `Da = today - 30 days`

This preserves the current practical behavior of showing a recent window while making it explicit and understandable.

The range shall persist across app restarts.

### Validation behavior

If the user selects an invalid range where `Da > A`, the app shall correct it safely instead of letting the state drift into nonsense.

Recommended behavior:

- when changing `Da`, if it lands after `A`, set `A = Da`
- when changing `A`, if it lands before `Da`, set `Da = A`

This avoids modal error loops and keeps the UI predictable.

### Reset behavior

`Reset filtro` shall restore the default range:

- `A = today`
- `Da = today - 30 days`

It shall not switch to unlimited all-history mode.

## Architecture Design

### Separate dataset range from chart grouping

Two concepts must remain explicitly separate:

1. `Dataset range`
   - defined by `Da/A`
   - decides which records exist for the UI

2. `Chart grouping`
   - defined by `Daily / Weekly / Monthly / Yearly`
   - decides how already-filtered chart points are aggregated

Example:

- user selects `01/06 -> 30/06`
- all lists, totals, pies, and rankings use only June data
- line chart may show June as daily, weekly, monthly, or yearly grouping
- grouping never expands the dataset outside June

### Persistence

Store the global range in app preferences alongside other UI state.

Recommended persisted values:

- `filterStartDateEpochMs`
- `filterEndDateEpochMs`

They represent normalized local-day boundaries for inclusive filtering.

### Shared filter layer

Centralize range filtering so all consumers use the same rule.

Introduce shared helper logic that:

- filters transactions by the active `Da/A` range
- filters snapshots by the active `Da/A` range
- returns normalized filtered collections ready for downstream use

This helper should be reused by:

- `CounterViewModel`
- `ChartViewModel`

The goal is to prevent one screen from applying a slightly different interpretation of the same range.

## Screen Behavior

### Home / history

The history list and grouped category sections shall use only transactions inside the selected range.

Text search and additional history filters continue to work, but only after the global date filter has already reduced the dataset.

Filtering order becomes:

1. global `Da/A` range
2. type filter
3. category filter
4. text search

### Chart screen

The chart screen shall use only snapshots and transactions inside the selected range.

The following values must all derive from the same filtered dataset:

- total expenses
- total income
- average surplus
- surplus standard deviation
- saving ratio
- expense category totals and percentages
- income category totals and percentages
- top expense descriptions
- top expense days
- line chart points

`Daily / Weekly / Monthly / Yearly` remains available only as chart aggregation mode.

### Saving ratio

The saving ratio shown on the analytics screen shall be computed only from records inside the selected global range.

It must not silently fall back to full-history values.

If the selected range does not have enough information for a meaningful ratio, return `0.0`.

### Empty states

If the selected range has no matching data:

- history shows no transactions for the selected period
- grouped lists are empty
- chart shows no points
- summaries return zero values
- pies and ranked analytics show empty states

The app must remain stable and coherent without fake fallback numbers.

## Migration Strategy

This is a non-destructive state evolution.

For existing installs:

- if the new persisted range is absent, initialize it to the default window
- do not transform historical transactions or snapshots

This keeps rollout simple and safe.

## Likely File Impact

Primary files likely touched:

- `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
- `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

Likely new support code:

- a small date-range model or helper file for normalized boundaries and defaults

## Testing Strategy

Use focused tests that prove the new source-of-truth behavior.

Recommended coverage:

- repository test for storing and reading the global date range
- viewmodel test confirming the default range initializes correctly
- history filtering test proving only in-range transactions are shown
- grouped transaction test proving category grouping only sees in-range records
- chart stats test proving all analytics are scoped to `Da/A`
- chart grouping test proving `Daily / Weekly / Monthly / Yearly` changes only aggregation, not dataset membership
- empty-range test proving safe zero and empty states
- invalid-range correction test proving `Da/A` remains valid after updates

Avoid low-value UI snapshot tests if pure logic tests already guarantee correctness.

## Recommendation

Implement this as a global app-level filter state, selected in Home and consumed everywhere.

This is the most professional model because it:

- matches how users think about an analysis window
- fixes the current conceptual mismatch between range and grouping
- keeps data safe
- makes future analytics features automatically consistent when they reuse the same filtered dataset
