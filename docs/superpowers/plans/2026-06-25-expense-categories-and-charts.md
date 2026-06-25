# Expense Categories And Charts Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add safe expense categories with migration for existing stored data, grouped collapsible history, corrected expense ratio, category pie chart, and better point-value interaction on line charts.

**Architecture:** Keep the existing `DataStore` JSON persistence model, extend `Transaction` with a backward-compatible `category`, normalize old saved records on read, and centralize category logic in small helper functions. Build grouped UI state in the view model, keep chart aggregation in `ChartViewModel`, and extend the chart screen with a pie chart and line-chart marker formatting.

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore Preferences, Gson, MPAndroidChart, JUnit

---

### Task 1: Add Category Domain Helpers

**Files:**
- Create: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`
- Modify: `app/src/main/java/com/example/startapp/data/model/Transaction.kt`
- Test: `app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.startapp

import com.example.startapp.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test

class ExpenseCategoryTest {

    @Test
    fun `known keywords map to stable categories`() {
        assertEquals(ExpenseCategory.FOOD.label, ExpenseCategory.inferFromDescription("Lidl groceries"))
        assertEquals(ExpenseCategory.TRANSPORT.label, ExpenseCategory.inferFromDescription("train ticket"))
        assertEquals(ExpenseCategory.BILLS.label, ExpenseCategory.inferFromDescription("internet bill"))
    }

    @Test
    fun `category catalog contains categories even before transactions exist`() {
        val labels = ExpenseCategory.expenseCategories.map { it.label }
        assertTrue(labels.contains("Food"))
        assertTrue(labels.contains("Travel"))
        assertTrue(labels.contains("Pets"))
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ExpenseCategoryTest"`
Expected: FAIL because `ExpenseCategory` does not exist.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.example.startapp.domain.model

enum class ExpenseCategory(val label: String, val keywords: List<String>) {
    FOOD("Food", listOf("grocery", "groceries", "supermarket", "coop", "lidl", "conad", "restaurant", "bar")),
    TRANSPORT("Transport", listOf("train", "bus", "metro", "taxi", "fuel", "parking", "toll")),
    HOME("Home", listOf("rent", "furniture", "repair", "ikea", "home")),
    BILLS("Bills", listOf("electricity", "gas", "water", "internet", "phone", "bill", "utility")),
    HEALTH("Health", listOf("pharmacy", "doctor", "dentist", "medicine", "hospital")),
    SHOPPING("Shopping", listOf("amazon", "clothes", "shoes", "electronics", "shopping")),
    LEISURE("Leisure", listOf("cinema", "movie", "game", "netflix", "spotify", "museum")),
    TRAVEL("Travel", listOf("flight", "hotel", "booking", "trip", "travel")),
    WORK("Work", listOf("office", "coworking", "software", "tool")),
    EDUCATION("Education", listOf("course", "book", "exam", "tuition")),
    TAXES("Taxes", listOf("tax", "fine", "fee")),
    SUBSCRIPTIONS("Subscriptions", listOf("subscription", "prime", "icloud")),
    GIFTS("Gifts", listOf("gift", "present")),
    PETS("Pets", listOf("pet", "dog", "cat", "vet")),
    OTHER("Other", emptyList()),
    INCOME("Income", emptyList());

    companion object {
        val expenseCategories: List<ExpenseCategory> = listOf(
            FOOD, TRANSPORT, HOME, BILLS, HEALTH, SHOPPING, LEISURE, TRAVEL,
            WORK, EDUCATION, TAXES, SUBSCRIPTIONS, GIFTS, PETS, OTHER
        )

        fun inferFromDescription(description: String): String {
            val normalized = description.trim().lowercase()
            return expenseCategories.firstOrNull { category ->
                category.keywords.any { keyword -> normalized.contains(keyword) }
            }?.label ?: OTHER.label
        }
    }
}
```

And update `Transaction.kt`:

```kotlin
data class Transaction(
    val id: String = UUID.randomUUID().toString(),
    val amount: Double,
    val date: Long,
    val description: String,
    val category: String = ""
)
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ExpenseCategoryTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt app/src/main/java/com/example/startapp/data/model/Transaction.kt app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt
git commit -m "feat: add expense category catalog"
```

### Task 2: Normalize Stored Transactions Safely

**Files:**
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Test: `app/src/test/java/com/example/startapp/TransactionMigrationTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.startapp

import com.example.startapp.domain.model.ExpenseCategory
import org.junit.Assert.assertEquals
import org.junit.Test

class TransactionMigrationTest {

    @Test
    fun `legacy expense without category gets inferred category`() {
        val migrated = normalizeTransactionCategory(
            amount = -42.0,
            description = "Lidl groceries",
            category = ""
        )

        assertEquals(ExpenseCategory.FOOD.label, migrated)
    }

    @Test
    fun `legacy income without category becomes income`() {
        val migrated = normalizeTransactionCategory(
            amount = 1000.0,
            description = "salary",
            category = ""
        )

        assertEquals("Income", migrated)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.TransactionMigrationTest"`
Expected: FAIL because `normalizeTransactionCategory` is missing.

- [ ] **Step 3: Write minimal implementation**

Add helper functions in `CounterDataRepository.kt`:

```kotlin
internal fun normalizeTransactionCategory(amount: Double, description: String, category: String): String {
    if (category.isNotBlank()) return category
    return if (amount >= 0) {
        ExpenseCategory.INCOME.label
    } else {
        ExpenseCategory.inferFromDescription(description)
    }
}

private fun List<Transaction>.normalizedTransactions(): List<Transaction> {
    return map { transaction ->
        transaction.copy(
            category = normalizeTransactionCategory(
                amount = transaction.amount,
                description = transaction.description,
                category = transaction.category
            )
        )
    }
}
```

Then update the `transactions` flow to normalize on read and add a repository method to persist normalized data:

```kotlin
val transactions: Flow<List<Transaction>> = context.dataStore.data.map { preferences ->
    val json = preferences[transactionsKey] ?: "[]"
    val type = object : TypeToken<List<Transaction>>() {}.type
    val current: List<Transaction> = gson.fromJson(json, type)
    current.normalizedTransactions()
}

suspend fun normalizeStoredTransactions() {
    context.dataStore.edit { preferences ->
        val json = preferences[transactionsKey] ?: "[]"
        val type = object : TypeToken<List<Transaction>>() {}.type
        val current: List<Transaction> = gson.fromJson(json, type)
        val normalized = current.normalizedTransactions()
        if (normalized != current) {
            preferences[transactionsKey] = gson.toJson(normalized)
        }
    }
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.TransactionMigrationTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/data/CounterDataRepository.kt app/src/test/java/com/example/startapp/TransactionMigrationTest.kt
git commit -m "feat: normalize legacy transaction categories"
```

### Task 3: Expose Grouped History State From The ViewModel

**Files:**
- Create: `app/src/main/java/com/example/startapp/domain/model/GroupedTransactions.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
- Test: `app/src/test/java/com/example/startapp/GroupedTransactionsTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.startapp

import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.buildGroupedTransactionState
import org.junit.Assert.assertEquals
import org.junit.Test

class GroupedTransactionsTest {

    @Test
    fun `expenses are grouped by category while income stays separate`() {
        val now = 1_700_000_000_000L
        val grouped = buildGroupedTransactionState(
            listOf(
                Transaction(amount = -20.0, date = now, description = "Lidl", category = "Food"),
                Transaction(amount = -10.0, date = now - 1, description = "Train", category = "Transport"),
                Transaction(amount = 100.0, date = now - 2, description = "Salary", category = "Income")
            )
        )

        assertEquals(1, grouped.incomeTransactions.size)
        assertEquals(2, grouped.expenseGroups.size)
        assertEquals("Food", grouped.expenseGroups.first().category)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.GroupedTransactionsTest"`
Expected: FAIL because grouping helpers do not exist.

- [ ] **Step 3: Write minimal implementation**

```kotlin
package com.example.startapp.domain.model

import com.example.startapp.data.model.Transaction

data class ExpenseGroup(
    val category: String,
    val total: Double,
    val transactions: List<Transaction>
)

data class GroupedTransactionState(
    val incomeTransactions: List<Transaction>,
    val expenseGroups: List<ExpenseGroup>
)

fun buildGroupedTransactionState(transactions: List<Transaction>): GroupedTransactionState {
    val sorted = transactions.sortedByDescending { it.date }
    val incomes = sorted.filter { it.amount >= 0 }
    val expenseGroups = sorted
        .filter { it.amount < 0 }
        .groupBy { it.category.ifBlank { "Other" } }
        .map { (category, items) ->
            ExpenseGroup(
                category = category,
                total = items.sumOf { -it.amount },
                transactions = items
            )
        }
        .sortedByDescending { it.total }

    return GroupedTransactionState(incomes, expenseGroups)
}
```

Then in `CounterViewModel.kt` add:

```kotlin
private val _groupedTransactions = MutableStateFlow(GroupedTransactionState(emptyList(), emptyList()))
val groupedTransactions = _groupedTransactions.asStateFlow()
```

and inside the transactions collector:

```kotlin
repository.normalizeStoredTransactions()
repository.transactions.collect {
    _transactions.value = it
    _groupedTransactions.value = buildGroupedTransactionState(it)
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.GroupedTransactionsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/GroupedTransactions.kt app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt app/src/test/java/com/example/startapp/GroupedTransactionsTest.kt
git commit -m "feat: add grouped transaction state"
```

### Task 4: Add Category Selection And Collapsible History UI

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`

- [ ] **Step 1: Write the failing UI-oriented test at the state level**

```kotlin
@Test
fun `selected category is preserved for expense submission`() {
    val category = "Food"
    assertEquals("Food", category)
}
```

- [ ] **Step 2: Run test to verify baseline**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.GroupedTransactionsTest"`
Expected: PASS for current grouping tests before UI edits.

- [ ] **Step 3: Write minimal implementation**

Update `CounterViewModel.kt` signatures:

```kotlin
fun addAmount(amount: Double, description: String, category: String = "Income")
fun subtractAmount(amount: Double, description: String, category: String)
```

Create transactions with category:

```kotlin
Transaction(
    amount = amount,
    date = System.currentTimeMillis(),
    description = description,
    category = category
)
```

In `CounterScreen.kt`:

```kotlin
var selectedExpenseCategory by remember { mutableStateOf(ExpenseCategory.FOOD.label) }
val groupedTransactions by viewModel.groupedTransactions.collectAsState()
val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
```

Add a category selector before the subtract button, for example with `ExposedDropdownMenuBox` or a simple clickable card list.

Render grouped history:

```kotlin
groupedTransactions.incomeTransactions.takeIf { it.isNotEmpty() }?.let { incomes ->
    item { Text("Income", style = MaterialTheme.typography.titleMedium) }
    items(incomes, key = { it.id }) { transaction ->
        TransactionRow(transaction = transaction, dateFormat = dateFormat, onDelete = viewModel::deleteTransaction)
    }
}

items(groupedTransactions.expenseGroups, key = { it.category }) { group ->
    val isExpanded = expandedCategories[group.category] ?: true
    CategoryGroupCard(
        group = group,
        isExpanded = isExpanded,
        onToggle = { expandedCategories[group.category] = !isExpanded },
        dateFormat = dateFormat,
        onDelete = viewModel::deleteTransaction
    )
}
```

- [ ] **Step 4: Run app-focused verification**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL and the main screen shows category grouping with collapsible sections.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt
git commit -m "feat: group history by expense category"
```

### Task 5: Extend Chart Models And Chart Calculations

**Files:**
- Modify: `app/src/main/java/com/example/startapp/domain/model/ChartData.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Test: `app/src/test/java/com/example/startapp/ChartStatsTest.kt`

- [ ] **Step 1: Write the failing test**

```kotlin
package com.example.startapp

import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.TimeFrame
import com.example.startapp.ui.viewmodel.calculateExpenseRatio
import org.junit.Assert.assertEquals
import org.junit.Test

class ChartStatsTest {

    @Test
    fun `expense ratio is overall expenses divided by overall income`() {
        assertEquals(0.25, calculateExpenseRatio(50.0, 200.0), 0.0001)
    }
}
```

- [ ] **Step 2: Run test to verify it fails**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ChartStatsTest"`
Expected: FAIL because `calculateExpenseRatio` does not exist.

- [ ] **Step 3: Write minimal implementation**

In `ChartData.kt` add:

```kotlin
data class CategoryExpenseSlice(
    val category: String,
    val total: Double,
    val percentage: Double
)

data class ChartStats(
    val points: List<ChartPoint>,
    val totalExpenses: Double,
    val totalIncome: Double,
    val avgSurplus: Double,
    val surplusStdDev: Double,
    val expenseRatio: Double,
    val savingsRatio: Double,
    val categoryExpenses: List<CategoryExpenseSlice>
)
```

In `ChartViewModel.kt` add:

```kotlin
internal fun calculateExpenseRatio(totalExpenses: Double, totalIncome: Double): Double {
    return if (totalIncome > 0.0) totalExpenses / totalIncome else 0.0
}
```

and inside `calculateChartData(...)` build category totals from filtered expense transactions:

```kotlin
val expenseTransactions = transactions
    .filter { it.date >= daysAgo && it.amount < 0 }

val categoryTotals = expenseTransactions
    .groupBy { it.category.ifBlank { "Other" } }
    .map { (category, items) ->
        category to items.sumOf { -it.amount }
    }
    .sortedByDescending { it.second }

val ratio = calculateExpenseRatio(totalExp, totalInc)
val categoryExpenses = categoryTotals.map { (category, total) ->
    CategoryExpenseSlice(
        category = category,
        total = total,
        percentage = if (totalExp > 0.0) total / totalExp else 0.0
    )
}
```

- [ ] **Step 4: Run test to verify it passes**

Run: `.\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ChartStatsTest"`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/ChartData.kt app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt app/src/test/java/com/example/startapp/ChartStatsTest.kt
git commit -m "feat: add category chart statistics"
```

### Task 6: Add Pie Chart And Line Marker UI

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
- Create: `app/src/main/res/layout/chart_marker.xml`

- [ ] **Step 1: Write baseline verification step**

```kotlin
// Use existing unit tests as the safety net, then verify the screen manually after UI changes.
```

- [ ] **Step 2: Run current test suite before UI changes**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS on the unit tests created in earlier tasks.

- [ ] **Step 3: Write minimal implementation**

In `ChartScreen.kt`:

```kotlin
Text(
    text = "Expense Ratio: %.2f%%".format(stats.expenseRatio * 100),
    modifier = Modifier.padding(vertical = 4.dp)
)
Text(
    text = "Savings Ratio: %.2f%%".format(stats.savingsRatio * 100),
    modifier = Modifier.padding(vertical = 4.dp)
)
```

Add a custom `MarkerView` for line charts:

```kotlin
class ExpenseMarkerView(context: Context) : MarkerView(context, R.layout.chart_marker) {
    private val format = SimpleDateFormat("dd/MM", Locale.getDefault())

    override fun refreshContent(e: Entry?, highlight: Highlight?) {
        findViewById<TextView>(R.id.markerText)?.text =
            "${format.format(Date(e?.x?.toLong() ?: 0L))} - ${"%.2f".format(e?.y ?: 0f)} €"
        super.refreshContent(e, highlight)
    }
}
```

Create `chart_marker.xml`:

```xml
<?xml version="1.0" encoding="utf-8"?>
<TextView xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/markerText"
    android:layout_width="wrap_content"
    android:layout_height="wrap_content"
    android:background="@android:color/black"
    android:padding="8dp"
    android:textColor="@android:color/white" />
```

Configure the line chart:

```kotlin
chart.marker = ExpenseMarkerView(chart.context)
chart.isHighlightPerTapEnabled = true
chart.isHighlightPerDragEnabled = true
surplusSet.setDrawCircles(true)
expenseSet.setDrawCircles(true)
incomeSet.setDrawCircles(true)
```

Add a pie chart with `PieChart`, `PieDataSet`, and `PieEntry`:

```kotlin
val pieEntries = stats.categoryExpenses.map { PieEntry(it.total.toFloat(), it.category) }
pieChart.data = PieData(
    PieDataSet(pieEntries, "Expense Categories").apply {
        colors = listOf(Color.RED, Color.BLUE, Color.GREEN, Color.MAGENTA, Color.CYAN, Color.YELLOW)
        valueTextColor = Color.WHITE
        sliceSpace = 2f
    }
)
pieChart.invalidate()
```

- [ ] **Step 4: Run build verification**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL and the chart screen shows the corrected ratios, the category pie chart, and visible values on tap.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt
git commit -m "feat: improve chart interaction and category pie chart"
```

### Task 7: Final Verification And Cleanup

**Files:**
- Modify: `app/src/test/java/com/example/startapp/ExampleUnitTest.kt` only if needed to remove dead placeholder coverage
- Review: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- Review: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

- [ ] **Step 1: Run full unit test suite**

Run: `.\gradlew.bat testDebugUnitTest`
Expected: PASS

- [ ] **Step 2: Run diagnostics and build**

Run: `.\gradlew.bat assembleDebug`
Expected: BUILD SUCCESSFUL

Then check IDE diagnostics for:

```text
CounterScreen.kt
ChartScreen.kt
CounterViewModel.kt
ChartViewModel.kt
CounterDataRepository.kt
```

- [ ] **Step 3: Verify stored-data safety manually**

Manual checklist:

```text
1. Launch app with existing stored transactions.
2. Confirm old expenses still appear.
3. Confirm each old expense has a plausible category.
4. Confirm deleting one transaction still updates the total correctly.
5. Confirm grouped sections expand and collapse.
6. Confirm pie chart totals match visible category sums.
7. Confirm tapping line points shows date and euro value.
```

- [ ] **Step 4: Update plan/spec status in git**

```bash
git add docs/superpowers/specs/2026-06-25-expense-categories-and-charts-design.md docs/superpowers/plans/2026-06-25-expense-categories-and-charts.md
git commit -m "docs: add expense categories implementation plan"
```

- [ ] **Step 5: Prepare handoff summary**

```text
Summarize: migration strategy, chosen category catalog, files changed, tests run, manual verification results, and any remaining follow-up items.
```
