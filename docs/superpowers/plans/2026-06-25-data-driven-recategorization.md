# Data-Driven Recategorization Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Read the real stored expense descriptions, derive better macro-categories from them, recategorize legacy transactions safely, improve category collapse UI, and keep pie-chart legend and overall ratio fully coherent.

**Architecture:** Use the existing `DataStore` transaction source as the truth, add explicit description overrides plus improved keyword groups in `ExpenseCategory`, and keep migration write-back limited to category changes only. Compute overall ratio from the full selected-period totals independent of chart aggregation, and render a complete textual legend under the pie chart so no category is truncated.

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore Preferences, Gson, MPAndroidChart, JUnit, adb

---

### Task 1: Inspect Real Stored Descriptions

**Files:**
- Review: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Review: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`

- [ ] **Step 1: Read stored transactions from the connected device**

```bash
adb shell run-as com.example.startapp cat files/../datastore/settings.preferences_pb
```

- [ ] **Step 2: If protobuf output is unreadable, extract via app-level normalization path**

```bash
adb shell run-as com.example.startapp ls
adb shell run-as com.example.startapp ls ../datastore
```

- [ ] **Step 3: Build a manual mapping list from real descriptions**

```text
Examples expected:
- isber -> Health
- clinica / medica / farmacia -> Health
- temu -> Shopping
- serata / uscita / 1 maggio -> Leisure
```

- [ ] **Step 4: Record the recurring terms to turn into explicit overrides**

```text
Output should be a short list of exact description fragments to promote above generic keyword matching.
```

- [ ] **Step 5: Commit inspection notes if written to docs**

```bash
git add docs/superpowers/plans/2026-06-25-data-driven-recategorization.md
git commit -m "docs: add data driven recategorization plan"
```

### Task 2: Add Failing Tests For Data-Driven Overrides

**Files:**
- Modify: `app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt`
- Modify: `app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt`

- [ ] **Step 1: Add failing override tests**

```kotlin
@Test
fun exactRealWorldDescriptionsMapToExpectedMacroCategories() {
    assertEquals(ExpenseCategory.HEALTH.label, ExpenseCategory.inferFromDescription("isber"))
    assertEquals(ExpenseCategory.SHOPPING.label, ExpenseCategory.inferFromDescription("temu"))
    assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("serata"))
    assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("1 maggio"))
}
```

and

```kotlin
@Test
fun legacyOtherEntriesGetPromotedUsingSpecificOverrides() {
    val migrated = normalizeTransactionCategory(
        amount = -30.0,
        description = "temu",
        category = ExpenseCategory.OTHER.label
    )

    assertEquals(ExpenseCategory.SHOPPING.label, migrated)
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ExpenseCategoryTest" --tests "com.example.startapp.data.TransactionMigrationTest"`
Expected: FAIL because the current mapping is too generic.

- [ ] **Step 3: Implement minimal override-aware categorization**

```kotlin
private val explicitDescriptionOverrides = linkedMapOf(
    "isber" to HEALTH,
    "temu" to SHOPPING,
    "serata" to LEISURE,
    "uscita" to LEISURE,
    "1 maggio" to LEISURE
)
```

and check overrides before generic keyword matching.

- [ ] **Step 4: Run tests to verify they pass**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ExpenseCategoryTest" --tests "com.example.startapp.data.TransactionMigrationTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt app/src/main/java/com/example/startapp/data/CounterDataRepository.kt app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt
git commit -m "feat: improve recategorization from real descriptions"
```

### Task 3: Make Category Collapse Visually Explicit

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`

- [ ] **Step 1: Add a small failing state test or baseline verification**

```kotlin
// Use the existing grouping tests as baseline, then verify build after the UI-only change.
```

- [ ] **Step 2: Run baseline build before edits**

Run: `.\gradlew.bat assembleDebug`
Expected: PASS

- [ ] **Step 3: Implement clearer collapse affordance**

```kotlin
Text(
    text = if (isExpanded) "[-] Collapse" else "[+] Expand",
    color = MaterialTheme.colorScheme.primary
)
```

and ensure only the category header toggles the nested list, while child items remain clearly indented:

```kotlin
Column(modifier = Modifier.padding(start = 12.dp)) {
    group.transactions.forEach { ... }
}
```

- [ ] **Step 4: Run build to verify the screen still compiles**

Run: `.\gradlew.bat assembleDebug`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt
git commit -m "feat: improve category collapse affordance"
```

### Task 4: Keep Overall Ratio Stable Across Timeframes

**Files:**
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`

- [ ] **Step 1: Add failing overall-totals test**

```kotlin
@Test
fun overallTotalsStayTheSameAcrossChartTimeframes() {
    val totals = calculateOverallTotals(
        snapshots = snapshots,
        transactions = transactions
    )

    assertEquals(15.0, totals.totalExpenses, 0.0001)
    assertEquals(55.0, totals.totalIncome, 0.0001)
    assertEquals(15.0 / 55.0, totals.expenseRatio, 0.0001)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"`
Expected: FAIL because `calculateOverallTotals` is missing or totals still depend on aggregation.

- [ ] **Step 3: Implement minimal overall totals helper**

```kotlin
internal data class OverallTotals(
    val totalExpenses: Double,
    val totalIncome: Double,
    val expenseRatio: Double
)
```

and compute the ratio from full selected-period snapshots + transactions rather than aggregated chart buckets.

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt
git commit -m "fix: keep overall expense ratio stable across timeframes"
```

### Task 5: Show Full Pie-Chart Legend

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

- [ ] **Step 1: Run baseline build before chart UI changes**

Run: `.\gradlew.bat assembleDebug`
Expected: PASS

- [ ] **Step 2: Disable the cramped built-in legend and add a full text legend below**

```kotlin
legend.isEnabled = false
```

and render:

```kotlin
stats.categoryExpenses.forEach { slice ->
    Text("${slice.category}: %.2f € (%.2f%%)".format(slice.total, slice.percentage * 100))
}
```

- [ ] **Step 3: Keep marker behavior intact**

```kotlin
marker = DaySurpMarkerView(context)
isHighlightPerTapEnabled = true
```

- [ ] **Step 4: Run build verification**

Run: `.\gradlew.bat assembleDebug`
Expected: PASS and full category legend visible below the pie chart.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt
git commit -m "feat: show full category legend under pie chart"
```

### Task 6: Final Device Verification

**Files:**
- Review: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`
- Review: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- Review: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

- [ ] **Step 1: Run full test suite and build**

Run: `.\gradlew.bat testDebugUnitTest assembleDebug`
Expected: PASS

- [ ] **Step 2: Install updated APK and launch app on the connected device**

```bash
adb install -r app/build/outputs/apk/debug/app-debug.apk
adb shell am force-stop com.example.startapp
adb shell am start -n com.example.startapp/.MainActivity
```

- [ ] **Step 3: Manually verify category promotions and UI**

```text
1. Check entries like isber are in Health.
2. Check temu is in Shopping.
3. Check serata/uscita/1 maggio are in Leisure.
4. Expand and collapse at least two category headers.
5. Open the chart page and verify the pie legend is fully readable.
6. Switch Day/Week/Month/Year and confirm the overall ratio stays coherent.
```

- [ ] **Step 4: Check IDE diagnostics**

```text
ExpenseCategory.kt
CounterDataRepository.kt
CounterScreen.kt
ChartViewModel.kt
ChartScreen.kt
```

- [ ] **Step 5: Prepare handoff summary**

```text
Summarize the real-data recategorization improvements, UI collapse improvements, ratio fix, tests run, and device verification results.
```
