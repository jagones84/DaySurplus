# Saving Ratio Redefinition Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Remove `Expense Ratio` and redefine `Saving Ratio` as `(net surplus to date) / (total income to date)` and display only this ratio in the chart UI.

**Architecture:** Keep chart points and category slices computed for the selected period, but compute the saving ratio using full-history snapshots/transactions so it’s stable across timeframes. Use a net-surplus definition that stays mathematically bounded (`netSurplus = lastSnapshot - firstSnapshot`).

**Tech Stack:** Kotlin, Jetpack Compose, JUnit4, Gradle

---

## File Map

- Modify: `app/src/main/java/com/example/startapp/domain/model/ChartData.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`

---

### Task 1: Update Tests (RED)

**Files:**
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`

- [ ] **Step 1: Replace expense-ratio tests with saving-ratio-to-date tests**

Update the test file to:
- remove references to `calculateExpenseRatio` / `calculateSavingsRatio`
- assert that `calculateSavingRatioToDate(...)` returns the expected value for a baseline snapshot at `0.0`.

- [ ] **Step 2: Run the test to confirm it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"
```

Expected: FAIL due to missing `calculateSavingRatioToDate` and/or mismatched `ChartStats` shape.

---

### Task 2: Implement New Ratio Model (GREEN)

**Files:**
- Modify: `app/src/main/java/com/example/startapp/domain/model/ChartData.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`

- [ ] **Step 1: Remove `expenseRatio` from `ChartStats`**

Update `ChartStats` to contain only `savingsRatio` (now meaning “saving ratio to date”).

- [ ] **Step 2: Add `calculateSavingRatioToDate(...)`**

Implement:
- `netSurplusToDate = lastSnapshot.amount - firstSnapshot.amount` (or `lastSnapshot.amount` when only one snapshot exists)
- `totalIncomeToDate = netSurplusToDate + totalExpensesToDate`
- `savingRatioToDate = netSurplusToDate / totalIncomeToDate` (safe-divide)

- [ ] **Step 3: Update `ChartViewModel` to compute `savingsRatio` from full history**

Keep period filtering for chart points and category slices, but compute `savingsRatio` using the full `snapshots` + `transactions` lists (unfiltered).

- [ ] **Step 4: Run the test to confirm it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"
```

Expected: PASS.

---

### Task 3: Update Chart UI (GREEN)

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

- [ ] **Step 1: Remove the `Expense Ratio` line from the UI**

Delete the “Expense Ratio” text row.

- [ ] **Step 2: Keep only Saving Ratio label and ensure it matches the new semantics**

Text should remain “Saving Ratio …” and display `stats.savingsRatio * 100`.

- [ ] **Step 3: Run a compile/build check**

Run:

```bash
./gradlew assembleDebug
```

Expected: PASS.

