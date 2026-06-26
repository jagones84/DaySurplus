# Global Date Range Filter Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the hidden `daysToDisplay`-driven dataset window with a persisted global `Da/A` date range selected in Home and used everywhere as the base dataset for history and analytics.

**Architecture:** Persist a global inclusive date range in `CounterDataRepository`, expose it through `CounterViewModel`, and make both Home/history and chart analytics consume the same filtered subset. Keep `Daily / Weekly / Monthly / Yearly` as chart-only aggregation over data already filtered by the global range.

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore Preferences, JUnit4, coroutines/Flow

---

## File Map

- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
  - Persist `filterStartDateEpochMs` and `filterEndDateEpochMs`
  - Expose range flows and update/reset methods
- Create: `app/src/main/java/com/example/startapp/domain/model/DateRangeFilter.kt`
  - Hold normalized inclusive range data and defaults
- Create: `app/src/main/java/com/example/startapp/domain/DateRangeFilterUtils.kt`
  - Normalize day boundaries and filter snapshots/transactions consistently
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
  - Expose global range state and update methods
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
  - Replace `daysToDisplay` filtering with shared `Da/A` filtering
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
  - Replace `Days` field with `Da`, `A`, and `Reset filtro`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
  - Clarify that chart timeframe chips only aggregate already-filtered data
- Create: `app/src/test/java/com/example/startapp/data/DateRangeFilterRepositoryTest.kt`
- Create: `app/src/test/java/com/example/startapp/domain/DateRangeFilterUtilsTest.kt`
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`
- Modify: `app/src/test/java/com/example/startapp/domain/GroupedTransactionsTest.kt`

### Task 1: Add the Global Date Range Model And Shared Filter Utilities

**Files:**
- Create: `app/src/main/java/com/example/startapp/domain/model/DateRangeFilter.kt`
- Create: `app/src/main/java/com/example/startapp/domain/DateRangeFilterUtils.kt`
- Test: `app/src/test/java/com/example/startapp/domain/DateRangeFilterUtilsTest.kt`

- [ ] **Step 1: Write the failing utility tests**

```kotlin
package com.example.startapp.domain

import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.DateRangeFilter
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class DateRangeFilterUtilsTest {

    @Test
    fun transactionFiltering_isInclusiveOfStartAndEndDays() {
        val june10Start = 1_718_006_400_000L
        val june12End = 1_718_265_599_999L
        val filter = DateRangeFilter(startEpochMs = june10Start, endEpochMs = june12End)
        val transactions = listOf(
            Transaction(amount = -1.0, date = june10Start, description = "start", category = "Food"),
            Transaction(amount = -2.0, date = 1_718_092_800_000L, description = "middle", category = "Food"),
            Transaction(amount = -3.0, date = june12End, description = "end", category = "Food"),
            Transaction(amount = -4.0, date = june12End + 1, description = "out", category = "Food")
        )

        val filtered = filterTransactionsByDateRange(transactions, filter)

        assertEquals(listOf("start", "middle", "end"), filtered.map { it.description })
    }

    @Test
    fun snapshotFiltering_returnsOnlyInRangeSnapshots() {
        val filter = DateRangeFilter(
            startEpochMs = 1_718_006_400_000L,
            endEpochMs = 1_718_179_199_999L
        )
        val snapshots = listOf(
            DailySnapshot(date = 1_717_920_000_000L, amount = 10.0),
            DailySnapshot(date = 1_718_006_400_000L, amount = 20.0),
            DailySnapshot(date = 1_718_092_800_000L, amount = 30.0),
            DailySnapshot(date = 1_718_265_600_000L, amount = 40.0)
        )

        val filtered = filterSnapshotsByDateRange(snapshots, filter)

        assertEquals(listOf(20.0, 30.0), filtered.map { it.amount })
    }

    @Test
    fun normalizedRange_swapsInvalidBoundaries() {
        val normalized = createNormalizedDateRange(
            startEpochMs = 1_718_265_600_000L,
            endEpochMs = 1_718_006_400_000L
        )

        assertTrue(normalized.startEpochMs <= normalized.endEpochMs)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.example.startapp.domain.DateRangeFilterUtilsTest"`

Expected: FAIL because `DateRangeFilter`, `filterTransactionsByDateRange`, `filterSnapshotsByDateRange`, or `createNormalizedDateRange` do not exist yet.

- [ ] **Step 3: Write the minimal model and utilities**

```kotlin
package com.example.startapp.domain.model

data class DateRangeFilter(
    val startEpochMs: Long,
    val endEpochMs: Long
)
```

```kotlin
package com.example.startapp.domain

import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.DateRangeFilter
import java.util.Calendar

fun createNormalizedDateRange(startEpochMs: Long, endEpochMs: Long): DateRangeFilter {
    val lower = minOf(startEpochMs, endEpochMs)
    val upper = maxOf(startEpochMs, endEpochMs)
    return DateRangeFilter(
        startEpochMs = startOfDay(lower),
        endEpochMs = endOfDay(upper)
    )
}

fun defaultDateRange(now: Long = System.currentTimeMillis()): DateRangeFilter {
    val end = endOfDay(now)
    val calendar = Calendar.getInstance().apply { timeInMillis = end }
    calendar.add(Calendar.DAY_OF_YEAR, -30)
    return DateRangeFilter(
        startEpochMs = startOfDay(calendar.timeInMillis),
        endEpochMs = end
    )
}

fun filterTransactionsByDateRange(
    transactions: List<Transaction>,
    range: DateRangeFilter
): List<Transaction> = transactions.filter { it.date in range.startEpochMs..range.endEpochMs }

fun filterSnapshotsByDateRange(
    snapshots: List<DailySnapshot>,
    range: DateRangeFilter
): List<DailySnapshot> = snapshots.filter { it.date in range.startEpochMs..range.endEpochMs }.sortedBy { it.date }

private fun startOfDay(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 0)
        set(Calendar.MINUTE, 0)
        set(Calendar.SECOND, 0)
        set(Calendar.MILLISECOND, 0)
    }.timeInMillis

private fun endOfDay(epochMs: Long): Long =
    Calendar.getInstance().apply {
        timeInMillis = epochMs
        set(Calendar.HOUR_OF_DAY, 23)
        set(Calendar.MINUTE, 59)
        set(Calendar.SECOND, 59)
        set(Calendar.MILLISECOND, 999)
    }.timeInMillis
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.example.startapp.domain.DateRangeFilterUtilsTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/DateRangeFilter.kt app/src/main/java/com/example/startapp/domain/DateRangeFilterUtils.kt app/src/test/java/com/example/startapp/domain/DateRangeFilterUtilsTest.kt
git commit -m "feat: add shared date range filter utilities"
```

### Task 2: Persist the Global Date Range in DataStore

**Files:**
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Test: `app/src/test/java/com/example/startapp/data/DateRangeFilterRepositoryTest.kt`

- [ ] **Step 1: Write the failing repository tests**

```kotlin
package com.example.startapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.startapp.domain.defaultDateRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File

class DateRangeFilterRepositoryTest {

    @Test
    fun dateRange_defaultsToRecentWindowWhenUnset() = runBlocking {
        val repository = buildRepository()

        val range = repository.dateRangeFilter.first()

        assertEquals(true, range.endEpochMs >= range.startEpochMs)
    }

    @Test
    fun updateDateRange_persistsNormalizedBounds() = runBlocking {
        val repository = buildRepository()
        repository.updateDateRangeFilter(
            startEpochMs = 1_718_265_600_000L,
            endEpochMs = 1_718_006_400_000L
        )

        val range = repository.dateRangeFilter.first()

        assertEquals(1_718_006_400_000L, range.startEpochMs)
        assertEquals(1_718_351_999_999L, range.endEpochMs)
    }

    private fun buildRepository(): CounterDataRepository {
        val tempFile = File.createTempFile("date-range-filter-test", ".preferences_pb").apply { deleteOnExit() }
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(produceFile = { tempFile })
        return CounterDataRepository(dataStore)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.example.startapp.data.DateRangeFilterRepositoryTest"`

Expected: FAIL because `dateRangeFilter` or `updateDateRangeFilter` does not exist yet.

- [ ] **Step 3: Implement repository persistence**

```kotlin
private val filterStartDateKey = longPreferencesKey("filter_start_date")
private val filterEndDateKey = longPreferencesKey("filter_end_date")

val dateRangeFilter: Flow<DateRangeFilter> = dataStore.data.map { preferences ->
    val default = defaultDateRange()
    val start = preferences[filterStartDateKey] ?: default.startEpochMs
    val end = preferences[filterEndDateKey] ?: default.endEpochMs
    createNormalizedDateRange(start, end)
}

suspend fun updateDateRangeFilter(startEpochMs: Long, endEpochMs: Long) {
    val normalized = createNormalizedDateRange(startEpochMs, endEpochMs)
    dataStore.edit {
        it[filterStartDateKey] = normalized.startEpochMs
        it[filterEndDateKey] = normalized.endEpochMs
    }
}

suspend fun resetDateRangeFilter(now: Long = System.currentTimeMillis()) {
    val default = defaultDateRange(now)
    dataStore.edit {
        it[filterStartDateKey] = default.startEpochMs
        it[filterEndDateKey] = default.endEpochMs
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `./gradlew testDebugUnitTest --tests "com.example.startapp.data.DateRangeFilterRepositoryTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/data/CounterDataRepository.kt app/src/test/java/com/example/startapp/data/DateRangeFilterRepositoryTest.kt
git commit -m "feat: persist global date range filter"
```

### Task 3: Expose the Range Through CounterViewModel And Use It For History

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
- Modify: `app/src/test/java/com/example/startapp/domain/GroupedTransactionsTest.kt`

- [ ] **Step 1: Write the failing grouped-history test**

```kotlin
@Test
fun groupedTransactions_canBeBuiltFromDateFilteredSubset() {
    val june10 = 1_718_006_400_000L
    val june20 = 1_718_092_800_000L
    val july10 = 1_720_684_800_000L

    val filtered = filterTransactionsByDateRange(
        listOf(
            Transaction(amount = -20.0, date = june10, description = "pizza", category = "Food"),
            Transaction(amount = 100.0, date = june20, description = "salary", category = "Salary"),
            Transaction(amount = -50.0, date = july10, description = "old", category = "Shopping")
        ),
        DateRangeFilter(startEpochMs = june10, endEpochMs = june20 + 86_399_999L)
    )

    val grouped = buildGroupedTransactionState(filtered)

    assertEquals(listOf("Salary"), grouped.incomeGroups.map { it.category })
    assertEquals(listOf("Food"), grouped.expenseGroups.map { it.category })
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.example.startapp.domain.GroupedTransactionsTest"`

Expected: FAIL until the new filter utilities are wired into test imports and compile path.

- [ ] **Step 3: Implement CounterViewModel range state**

```kotlin
private val _dateRangeFilter = MutableStateFlow(defaultDateRange())
val dateRangeFilter = _dateRangeFilter.asStateFlow()

init {
    viewModelScope.launch {
        repository.dateRangeFilter.collect { _dateRangeFilter.value = it }
    }
}

fun updateDateRangeFilter(startEpochMs: Long, endEpochMs: Long) {
    viewModelScope.launch {
        repository.updateDateRangeFilter(startEpochMs, endEpochMs)
    }
}

fun resetDateRangeFilter() {
    viewModelScope.launch {
        repository.resetDateRangeFilter()
    }
}
```

- [ ] **Step 4: Update Home/history filtering to use the range**

```kotlin
val filteredTransactions = filterTransactionsByDateRange(transactions, dateRangeFilter.value)
_groupedTransactions.value = buildGroupedTransactionState(filteredTransactions)
```

Keep the search/type/category filters downstream in `CounterScreen`; only replace the initial dataset source from `transactions within daysToDisplay` to `transactions within dateRangeFilter`.

- [ ] **Step 5: Run tests to verify they pass**

Run: `./gradlew testDebugUnitTest --tests "com.example.startapp.domain.GroupedTransactionsTest"`

Expected: PASS

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt app/src/test/java/com/example/startapp/domain/GroupedTransactionsTest.kt
git commit -m "feat: expose global date range through counter viewmodel"
```

### Task 4: Replace the Home `Days` Input with `Da/A` Date Pickers

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`

- [ ] **Step 1: Write the failing screen-focused compile target**

Run: `./gradlew compileDebugKotlin`

Expected: it currently passes, but this step establishes the baseline before replacing the `Days` field.

- [ ] **Step 2: Replace the `Days` field UI with `Da`, `A`, and `Reset filtro`**

```kotlin
val dateRangeFilter by viewModel.dateRangeFilter.collectAsState()
val dateFormatter = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }

OutlinedButton(onClick = { showStartDatePicker = true }) {
    Text("Da: ${dateFormatter.format(Date(dateRangeFilter.startEpochMs))}")
}

OutlinedButton(onClick = { showEndDatePicker = true }) {
    Text("A: ${dateFormatter.format(Date(dateRangeFilter.endEpochMs))}")
}

TextButton(onClick = { viewModel.resetDateRangeFilter() }) {
    Text("Reset filtro")
}
```

Use `DatePickerDialog` or Material date pickers already available in the project setup. When the user picks a start date, call:

```kotlin
viewModel.updateDateRangeFilter(
    startEpochMs = pickedStartMillis,
    endEpochMs = dateRangeFilter.endEpochMs
)
```

When the user picks an end date:

```kotlin
viewModel.updateDateRangeFilter(
    startEpochMs = dateRangeFilter.startEpochMs,
    endEpochMs = pickedEndMillis
)
```

- [ ] **Step 3: Update the local history dataset source**

```kotlin
val periodTransactions = remember(transactions, dateRangeFilter) {
    filterTransactionsByDateRange(transactions, dateRangeFilter).reversed()
}
```

Remove the old `daysInput` UI state and all uses of `viewModel.updateDaysToDisplay(days)`.

- [ ] **Step 4: Run compile to verify it passes**

Run: `./gradlew compileDebugKotlin`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt
git commit -m "feat: add home date range filter controls"
```

### Task 5: Scope Chart Analytics to the Persisted Global Range

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`

- [ ] **Step 1: Write the failing chart test**

```kotlin
@Test
fun calculateChartStatsForPeriod_usesExplicitDateRangeInsteadOfDaysToDisplay() {
    val start = 1_718_006_400_000L
    val end = 1_718_179_199_999L

    val stats = calculateChartStatsForPeriod(
        snapshots = listOf(
            DailySnapshot(date = 1_717_920_000_000L, amount = 10.0),
            DailySnapshot(date = start, amount = 100.0),
            DailySnapshot(date = 1_718_092_800_000L, amount = 120.0)
        ),
        transactions = listOf(
            Transaction(amount = -50.0, date = 1_717_920_000_000L, description = "old", category = "Other"),
            Transaction(amount = -10.0, date = 1_718_092_800_000L, description = "farmacia", category = "Health")
        ),
        range = DateRangeFilter(startEpochMs = start, endEpochMs = end),
        timeFrame = TimeFrame.Day,
        dailyIncrease = 0.0
    )

    assertEquals(10.0, stats.totalExpenses, 0.0001)
    assertEquals(30.0 / 40.0, stats.savingsRatio, 0.0001)
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `./gradlew testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"`

Expected: FAIL because `calculateChartStatsForPeriod` still accepts `daysToDisplay`.

- [ ] **Step 3: Refactor ChartViewModel to use `DateRangeFilter`**

```kotlin
internal fun calculateChartStatsForPeriod(
    snapshots: List<DailySnapshot>,
    transactions: List<Transaction>,
    range: DateRangeFilter,
    timeFrame: TimeFrame,
    dailyIncrease: Double
): ChartStats {
    val relevantSnapshots = filterSnapshotsByDateRange(snapshots, range)
    val relevantTransactions = filterTransactionsByDateRange(transactions, range)
    val coveredDays = calculateCoveredDays(range)
    // keep the existing aggregation and analytics logic from here
}
```

In `init`, combine:

```kotlin
combine(
    repository.dailySnapshots,
    repository.transactions,
    repository.dateRangeFilter,
    repository.dailyIncrease,
    _timeFrame
) { snapshots, transactions, range, dailyIncrease, selectedTimeFrame ->
    calculateChartData(snapshots, transactions, range, selectedTimeFrame, dailyIncrease)
}
```

- [ ] **Step 4: Run the chart tests**

Run: `./gradlew testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"`

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt
git commit -m "feat: scope chart analytics to global date range"
```

### Task 6: Clarify Chart UI And Remove Remaining `daysToDisplay` Product Usage

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
- Modify: `app/src/main/java/com/example/startapp/data/model/AppBackup.kt`
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`

- [ ] **Step 1: Write the failing compile target**

Run: `./gradlew compileDebugKotlin`

Expected: it may fail after Task 5 until any residual `daysToDisplay` references are removed or kept as compatibility-only state.

- [ ] **Step 2: Update chart wording so chips clearly mean aggregation**

```kotlin
Text(
    text = "Chart Aggregation",
    style = MaterialTheme.typography.titleSmall,
    modifier = Modifier.padding(bottom = 8.dp)
)
```

Keep the chips, but do not label them as period selectors. Preserve text like `Selected Period` for analytics values because the real selected period now comes from Home.

- [ ] **Step 3: Keep backup compatibility sane**

If `daysToDisplay` must remain in `AppBackup` for backward compatibility, stop using it as a product driver but continue serializing it safely:

```kotlin
val daysToDisplay: Int,
```

Do not remove the field from backup schema in this task. Only stop using it for analytics/history filtering.

- [ ] **Step 4: Run app-level checks**

Run:

```bash
./gradlew testDebugUnitTest
./gradlew assembleDebug
```

Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt app/src/main/java/com/example/startapp/data/model/AppBackup.kt app/src/main/java/com/example/startapp/data/CounterDataRepository.kt
git commit -m "refactor: separate global date range from chart aggregation"
```

## Self-Review

- Spec coverage:
  - `Da/A` global filter in Home: covered by Task 4
  - persisted inclusive range: covered by Tasks 1 and 2
  - shared filter source for history and chart: covered by Tasks 3 and 5
  - chart grouping separated from dataset range: covered by Tasks 5 and 6
  - no data deletion: preserved by architecture and Task 6 compatibility note
  - empty states and regression tests: covered by Tasks 1, 3, and 5 plus final full test run
- Placeholder scan:
  - no `TODO`, `TBD`, or “implement later” placeholders remain
- Type consistency:
  - shared type name is `DateRangeFilter`
  - repository flow is `dateRangeFilter`
  - repository update method is `updateDateRangeFilter`
  - reset method is `resetDateRangeFilter`

## Execution Handoff

Plan complete and saved to `docs/superpowers/plans/2026-06-26-global-date-range-filter-implementation.md`. Two execution options:

1. Subagent-Driven (recommended) - I dispatch a fresh subagent per task, review between tasks, fast iteration
2. Inline Execution - Execute tasks in this session using executing-plans, batch execution with checkpoints

Which approach?
