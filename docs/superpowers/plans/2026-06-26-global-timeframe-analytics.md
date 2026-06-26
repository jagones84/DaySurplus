# Global Timeframe Analytics Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Make every metric and chart on the analytics screen use only the active `daysToDisplay` period, without changing or deleting persisted app data.

**Architecture:** Keep the fix localized to `ChartViewModel` by centralizing filtered analytics inputs there and deriving all downstream stats from the same filtered snapshots and transactions. Update the analytics label in `ChartScreen` so the UI no longer implies all-time behavior, then lock the behavior down with targeted unit tests in `ChartStatsTest`.

**Tech Stack:** Kotlin, Android ViewModel, Kotlin Flow, Jetpack Compose, JUnit4, Gradle

---

### Task 1: Lock The Timeframe Behavior With Failing Tests

**Files:**
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`
- Test: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`

- [ ] **Step 1: Write the failing tests**

```kotlin
@Test
fun savingRatioToDate_usesOnlyProvidedFilteredPeriod() {
    val now = 1_700_000_000_000L
    val filteredSnapshots = listOf(
        DailySnapshot(date = now - 2_000L, amount = 100.0),
        DailySnapshot(date = now - 1_000L, amount = 130.0)
    )
    val filteredTransactions = listOf(
        Transaction(
            amount = -10.0,
            date = now - 1_500L,
            description = "farmacia",
            category = ExpenseCategory.HEALTH.label
        )
    )

    val savingRatio = calculateSavingRatioToDate(
        snapshots = filteredSnapshots,
        transactions = filteredTransactions
    )

    assertEquals(30.0 / 40.0, savingRatio, 0.0001)
}

@Test
fun savingRatioToDate_returnsZeroWhenPeriodHasNoSnapshots() {
    val savingRatio = calculateSavingRatioToDate(
        snapshots = emptyList(),
        transactions = listOf(
            Transaction(
                amount = -25.0,
                date = 1_700_000_000_000L,
                description = "spesa",
                category = ExpenseCategory.FOOD.label
            )
        )
    )

    assertEquals(0.0, savingRatio, 0.0)
}
```

- [ ] **Step 2: Run the targeted test class to verify the new expectation fails or exposes the wrong current behavior**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"`

Expected: either FAIL on the new assertions or PASS only after the implementation in Task 2 is complete.

- [ ] **Step 3: Keep the existing synthetic-income and monthly-average tests intact**

```kotlin
@Test
fun incomeCategorySlicesIncludeSyntheticDailySurplus() {
    val now = 1_700_000_000_000L
    val slices = buildIncomeCategorySlices(
        transactions = listOf(
            Transaction(amount = 1000.0, date = now, description = "stipendio", category = "Salary"),
            Transaction(amount = 100.0, date = now - 1_000L, description = "rimborso", category = "Refund"),
        ),
        coveredDays = 30,
        dailyIncrease = 20.0
    )

    assertTrue(slices.any { it.category == "Daily Surplus" && it.total == 600.0 })
    assertTrue(slices.any { it.category == "Salary" && it.total == 1000.0 })
}
```

- [ ] **Step 4: Re-run the targeted test class after editing the file**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"`

Expected: FAIL only on the new timeframe assertions if the implementation has not yet been updated.

- [ ] **Step 5: Commit the pure-test checkpoint**

```bash
git add app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt
git commit -m "test: cover selected-period analytics ratio"
```

### Task 2: Centralize Filtered Analytics In ChartViewModel

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Test: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`

- [ ] **Step 1: Extract a small helper so filtering is explicit and reusable inside `ChartViewModel.kt`**

```kotlin
private data class FilteredAnalyticsData(
    val snapshots: List<DailySnapshot>,
    val transactions: List<Transaction>,
    val coveredDays: Int
)

private fun filterAnalyticsData(
    snapshots: List<DailySnapshot>,
    transactions: List<Transaction>,
    daysToDisplay: Int,
    now: Long = System.currentTimeMillis()
): FilteredAnalyticsData {
    val coveredDays = maxOf(daysToDisplay, 1)
    val minTimestamp = now - (coveredDays.toLong() * 24 * 60 * 60 * 1000)
    return FilteredAnalyticsData(
        snapshots = snapshots.filter { it.date >= minTimestamp }.sortedBy { it.date },
        transactions = transactions.filter { it.date >= minTimestamp },
        coveredDays = coveredDays
    )
}
```

- [ ] **Step 2: Update `calculateChartData()` so every downstream value uses the filtered dataset**

```kotlin
val filtered = filterAnalyticsData(
    snapshots = snapshots,
    transactions = transactions,
    daysToDisplay = daysToDisplay
)
val relevantSnapshots = filtered.snapshots
val relevantTransactions = filtered.transactions

if (relevantSnapshots.isEmpty()) {
    return ChartStats(
        points = emptyList(),
        totalExpenses = 0.0,
        totalIncome = 0.0,
        avgSurplus = 0.0,
        surplusStdDev = 0.0,
        savingsRatio = 0.0,
        categoryExpenses = emptyList(),
        categoryIncome = emptyList(),
        topExpenseDescriptions = emptyList(),
        topExpenseDays = emptyList()
    )
}

val periodTotals = calculatePeriodTotals(
    snapshots = relevantSnapshots,
    transactions = relevantTransactions
)
val categoryExpenses = buildExpenseCategorySlices(relevantTransactions, filtered.coveredDays)
val categoryIncome = buildIncomeCategorySlices(relevantTransactions, filtered.coveredDays, dailyIncrease)
val topExpenseDescriptions = buildTopExpenseDescriptions(relevantTransactions)
val topExpenseDays = buildTopExpenseDays(relevantTransactions)
val savingRatio = calculateSavingRatioToDate(
    snapshots = relevantSnapshots,
    transactions = relevantTransactions
)
```

- [ ] **Step 3: Remove dead locals and misleading comments from `calculateChartData()`**

```kotlin
// Delete these locals because they are never used after the refactor:
// var totalExp = 0.0
// var totalInc = 0.0

// Replace the old comments with one concise note:
// All analytics shown on the chart screen must derive from the same filtered period.
```

- [ ] **Step 4: Run the focused unit tests to confirm the ViewModel helpers now honor the selected period**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"`

Expected: PASS

- [ ] **Step 5: Commit the behavior change**

```bash
git add app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt
git commit -m "fix: scope analytics to selected period"
```

### Task 3: Align The Analytics Label And Final Verification

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
- Modify: `docs/superpowers/specs/2026-06-26-global-timeframe-analytics-design.md`
- Modify: `docs/superpowers/plans/2026-06-26-global-timeframe-analytics.md`
- Test: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`

- [ ] **Step 1: Update the saving-ratio label so it no longer says `To Date`**

```kotlin
Text(
    text = "Saving Ratio (Selected Period): %.2f%%".format(stats.savingsRatio * 100),
    modifier = Modifier.padding(vertical = 4.dp)
)
```

- [ ] **Step 2: Run the unit test class plus the module unit-test task**

Run: `./gradlew :app:testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"`

Expected: PASS

Run: `./gradlew :app:testDebugUnitTest`

Expected: PASS

- [ ] **Step 3: Run diagnostics on the touched Kotlin files and fix any straightforward issues**

Run:

```bash
./gradlew :app:compileDebugKotlin
```

Expected: BUILD SUCCESSFUL

- [ ] **Step 4: Inspect the branch state before publishing**

Run:

```bash
git status --short
git log --oneline --decorate -n 5
```

Expected: only the intended files are modified or committed on the feature branch.

- [ ] **Step 5: Commit the UI wording update and push the branch**

```bash
git add app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt docs/superpowers/specs/2026-06-26-global-timeframe-analytics-design.md docs/superpowers/plans/2026-06-26-global-timeframe-analytics.md
git commit -m "docs: record global timeframe analytics plan"
git push -u origin fix/global-timeframe-analytics
```

## Self-Review

- Spec coverage: the plan covers centralized timeframe filtering, selected-period saving ratio, UI wording, safe zero states, and targeted regression testing.
- Placeholder scan: there are no `TODO`, `TBD`, or "test the above" style placeholders; every step includes exact files, commands, and code.
- Type consistency: the plan keeps existing production names such as `ChartStats`, `calculateSavingRatioToDate()`, `ChartScreen`, `daysToDisplay`, and `DailySnapshot`.
