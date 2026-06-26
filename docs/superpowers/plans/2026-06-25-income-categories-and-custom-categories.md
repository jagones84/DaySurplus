# Income Categories And Custom Categories Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add categorized and collapsible income history, persistent custom categories for both incomes and expenses, an income pie chart with monthly averages, and GitHub-style repository hygiene without losing existing user data.

**Architecture:** Extend the current DataStore-backed repository instead of replacing it. Keep the existing expense inference rules, add a small category catalog layer for built-in and custom labels, and evolve `ChartStats` so expense and income pie-chart data are computed from one coherent aggregation pipeline, including the synthetic `Daily Surplus` contribution.

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore Preferences, Gson JSON persistence, MPAndroidChart, JUnit4, Gradle

---

## File Map

- Modify: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`
- Create: `app/src/main/java/com/example/startapp/domain/model/CategoryCatalog.kt`
- Modify: `app/src/main/java/com/example/startapp/domain/model/GroupedTransactions.kt`
- Modify: `app/src/main/java/com/example/startapp/domain/model/ChartData.kt`
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
- Create: `app/src/test/java/com/example/startapp/domain/CategoryCatalogTest.kt`
- Modify: `app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt`
- Modify: `app/src/test/java/com/example/startapp/domain/GroupedTransactionsTest.kt`
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`
- Create: `README.md`
- Modify: `.gitignore`

### Task 1: Build Category Catalog Foundations

**Files:**
- Create: `app/src/main/java/com/example/startapp/domain/model/CategoryCatalog.kt`
- Modify: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`
- Test: `app/src/test/java/com/example/startapp/domain/CategoryCatalogTest.kt`

- [ ] **Step 1: Write the failing catalog test**

```kotlin
package com.example.startapp.domain

import com.example.startapp.domain.model.CategoryCatalog
import com.example.startapp.domain.model.CategoryType
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Test

class CategoryCatalogTest {

    @Test
    fun incomeCatalog_containsBuiltInIncomeCategoriesAndNewCategoryEntry() {
        val incomeOptions = CategoryCatalog.dropdownOptions(CategoryType.INCOME, emptyList())

        assertTrue(incomeOptions.contains("Daily Surplus"))
        assertTrue(incomeOptions.contains("Salary"))
        assertTrue(incomeOptions.contains("Other Income"))
        assertEquals("+ New Category", incomeOptions.last())
    }

    @Test
    fun normalizeCustomCategoryName_trimsAndTitleCasesText() {
        assertEquals("Side Hustle", CategoryCatalog.normalizeCustomCategoryName("  side   hustle  "))
    }

    @Test
    fun canAddCustomCategory_blocksDuplicatesAgainstBuiltInAndCustomLabels() {
        val existing = listOf("Family Support")

        assertFalse(CategoryCatalog.canAddCustomCategory(CategoryType.INCOME, "salary", existing))
        assertFalse(CategoryCatalog.canAddCustomCategory(CategoryType.INCOME, " family support ", existing))
        assertTrue(CategoryCatalog.canAddCustomCategory(CategoryType.INCOME, "Dividends", existing))
    }
}
```

- [ ] **Step 2: Run the test to verify it fails**

Run:

```bash
.\gradlew testDebugUnitTest --tests "com.example.startapp.domain.CategoryCatalogTest"
```

Expected: FAIL because `CategoryCatalog` and `CategoryType` do not exist yet.

- [ ] **Step 3: Write the minimal category catalog implementation**

```kotlin
package com.example.startapp.domain.model

enum class CategoryType {
    EXPENSE,
    INCOME
}

object CategoryCatalog {
    const val NEW_CATEGORY_OPTION = "+ New Category"

    private val builtInExpenseCategories = listOf(
        "Food", "Transport", "Health", "Shopping", "Gaming",
        "Leisure", "Travel", "Digital", "Education", "Taxes",
        "Gifts", "Pets", "Other"
    )

    private val builtInIncomeCategories = listOf(
        "Daily Surplus", "Salary", "Family", "Refund",
        "Sales", "Bonus", "Other Income"
    )

    fun builtInCategories(type: CategoryType): List<String> {
        return when (type) {
            CategoryType.EXPENSE -> builtInExpenseCategories
            CategoryType.INCOME -> builtInIncomeCategories
        }
    }

    fun dropdownOptions(type: CategoryType, custom: List<String>): List<String> {
        return builtInCategories(type) + custom.sorted() + NEW_CATEGORY_OPTION
    }

    fun normalizeCustomCategoryName(raw: String): String {
        return raw
            .trim()
            .replace("\\s+".toRegex(), " ")
            .split(" ")
            .filter { it.isNotBlank() }
            .joinToString(" ") { part ->
                part.lowercase().replaceFirstChar { it.titlecase() }
            }
    }

    fun canAddCustomCategory(type: CategoryType, raw: String, existing: List<String>): Boolean {
        val normalized = normalizeCustomCategoryName(raw)
        if (normalized.isBlank()) return false

        val occupied = (builtInCategories(type) + existing)
            .map { normalizeCustomCategoryName(it) }
            .toSet()

        return normalized !in occupied
    }
}
```

- [ ] **Step 4: Extend expense inference to reuse the built-in catalog without changing current mappings**

```kotlin
val expenseCategories: List<ExpenseCategory> = listOf(
    FOOD, TRANSPORT, HEALTH, SHOPPING, GAMING,
    LEISURE, TRAVEL, DIGITAL, EDUCATION, TAXES,
    GIFTS, PETS, OTHER
)

fun builtInExpenseLabels(): List<String> {
    return CategoryCatalog.builtInCategories(CategoryType.EXPENSE)
}
```

- [ ] **Step 5: Run the catalog test and the existing expense mapping tests**

Run:

```bash
.\gradlew testDebugUnitTest --tests "com.example.startapp.domain.CategoryCatalogTest" --tests "com.example.startapp.ExpenseCategoryTest"
```

Expected: PASS for the new catalog tests and no regression in expense keyword mapping.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/CategoryCatalog.kt app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt app/src/test/java/com/example/startapp/domain/CategoryCatalogTest.kt app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt
git commit -m "feat: add category catalog foundations"
```

### Task 2: Persist Custom Categories And Migrate Legacy Income Categories

**Files:**
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Modify: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`
- Modify: `app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt`

- [ ] **Step 1: Write failing migration and custom-category persistence tests**

```kotlin
@Test
fun normalizeTransactionCategory_mapsPositiveLegacyDescriptionsToIncomeCategories() {
    assertEquals("Salary", normalizeTransactionCategory(1500.0, "stipendio marzo", null))
    assertEquals("Refund", normalizeTransactionCategory(80.0, "rimborso assicurazione", null))
    assertEquals("Other Income", normalizeTransactionCategory(20.0, "entrata strana", null))
}

@Test
fun addCustomCategory_persistsSeparatedExpenseAndIncomeCatalogs() = runTest {
    val repository = buildRepository()

    repository.addCustomCategory(CategoryType.EXPENSE, "Casa Vacanze")
    repository.addCustomCategory(CategoryType.INCOME, "Dividends")

    assertEquals(listOf("Casa Vacanze"), repository.expenseCustomCategories.first())
    assertEquals(listOf("Dividends"), repository.incomeCustomCategories.first())
}
```

- [ ] **Step 2: Run the repository-focused tests to verify they fail**

Run:

```bash
.\gradlew testDebugUnitTest --tests "com.example.startapp.data.TransactionMigrationTest"
```

Expected: FAIL because income inference and custom-category persistence do not exist yet.

- [ ] **Step 3: Add income inference helpers next to the existing expense rules**

```kotlin
fun inferIncomeCategory(description: String): String {
    val normalized = normalizeDescription(description)

    return when {
        listOf("salary", "stipendio", "ral").any(normalized::contains) -> "Salary"
        listOf("genitori", "family", "mamma", "papa").any(normalized::contains) -> "Family"
        listOf("refund", "rimborso", "rimb").any(normalized::contains) -> "Refund"
        listOf("vendita", "sold", "venduto", "ram venduta").any(normalized::contains) -> "Sales"
        listOf("bonus", "premio").any(normalized::contains) -> "Bonus"
        else -> "Other Income"
    }
}
```

- [ ] **Step 4: Persist custom categories in DataStore and normalize legacy positive transactions**

```kotlin
private val expenseCustomCategoriesKey = stringPreferencesKey("expense_custom_categories")
private val incomeCustomCategoriesKey = stringPreferencesKey("income_custom_categories")

val expenseCustomCategories: Flow<List<String>> = context.dataStore.data.map { preferences ->
    gson.fromJson(preferences[expenseCustomCategoriesKey] ?: "[]", object : TypeToken<List<String>>() {}.type) ?: emptyList()
}

val incomeCustomCategories: Flow<List<String>> = context.dataStore.data.map { preferences ->
    gson.fromJson(preferences[incomeCustomCategoriesKey] ?: "[]", object : TypeToken<List<String>>() {}.type) ?: emptyList()
}

suspend fun addCustomCategory(type: CategoryType, rawName: String): String? {
    val key = if (type == CategoryType.EXPENSE) expenseCustomCategoriesKey else incomeCustomCategoriesKey
    val normalized = CategoryCatalog.normalizeCustomCategoryName(rawName)

    if (!CategoryCatalog.canAddCustomCategory(type, normalized, currentCustomCategories(type))) {
        return null
    }

    context.dataStore.edit { preferences ->
        val current = gson.fromJson(preferences[key] ?: "[]", object : TypeToken<List<String>>() {}.type) ?: emptyList()
        preferences[key] = gson.toJson((current + normalized).distinct().sorted())
    }

    return normalized
}

internal fun normalizeTransactionCategory(amount: Double, description: String, category: String?): String {
    if (amount >= 0) {
        return category?.takeIf { it.isNotBlank() && it != ExpenseCategory.INCOME.label }
            ?: ExpenseCategory.inferIncomeCategory(description)
    }

    // keep existing expense behavior here
}
```

- [ ] **Step 5: Run migration tests and existing category tests**

Run:

```bash
.\gradlew testDebugUnitTest --tests "com.example.startapp.data.TransactionMigrationTest" --tests "com.example.startapp.domain.CategoryCatalogTest" --tests "com.example.startapp.ExpenseCategoryTest"
```

Expected: PASS with legacy positive transactions migrating to the correct income categories and custom catalogs stored separately.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/startapp/data/CounterDataRepository.kt app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt
git commit -m "feat: persist custom categories and migrate income labels"
```

### Task 3: Make Income And Expense History Fully Grouped, Collapsible, And Category-Aware

**Files:**
- Modify: `app/src/main/java/com/example/startapp/domain/model/GroupedTransactions.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- Modify: `app/src/test/java/com/example/startapp/domain/GroupedTransactionsTest.kt`

- [ ] **Step 1: Write failing grouping tests for income sections**

```kotlin
@Test
fun buildGroupedTransactionState_groupsIncomeByCategoryAndKeepsTotalsPositive() {
    val transactions = listOf(
        Transaction(amount = 2000.0, date = 1L, description = "stipendio", category = "Salary"),
        Transaction(amount = 300.0, date = 2L, description = "vendita ram", category = "Sales"),
        Transaction(amount = 50.0, date = 3L, description = "mamma", category = "Family"),
        Transaction(amount = -20.0, date = 4L, description = "panino", category = "Food")
    )

    val grouped = buildGroupedTransactionState(transactions)

    assertEquals(3, grouped.incomeGroups.size)
    assertEquals("Salary", grouped.incomeGroups.first().category)
    assertEquals(2000.0, grouped.incomeGroups.first().total, 0.001)
}
```

- [ ] **Step 2: Run the grouping tests to verify they fail**

Run:

```bash
.\gradlew testDebugUnitTest --tests "com.example.startapp.domain.GroupedTransactionsTest"
```

Expected: FAIL because income groups do not exist in `GroupedTransactionState`.

- [ ] **Step 3: Update grouped-domain structures to support both expense groups and income groups**

```kotlin
data class TransactionGroup(
    val category: String,
    val total: Double,
    val transactions: List<Transaction>
)

data class GroupedTransactionState(
    val incomeGroups: List<TransactionGroup>,
    val expenseGroups: List<TransactionGroup>
)

fun buildGroupedTransactionState(transactions: List<Transaction>): GroupedTransactionState {
    val sorted = transactions.sortedByDescending { it.date }

    fun buildGroups(source: List<Transaction>, fallback: String, absolute: Boolean): List<TransactionGroup> {
        return source
            .groupBy { it.category.ifBlank { fallback } }
            .map { (category, items) ->
                TransactionGroup(
                    category = category,
                    total = if (absolute) items.sumOf { kotlin.math.abs(it.amount) } else items.sumOf { it.amount },
                    transactions = items
                )
            }
            .sortedByDescending { it.total }
    }

    return GroupedTransactionState(
        incomeGroups = buildGroups(sorted.filter { it.amount >= 0 }, "Other Income", absolute = false),
        expenseGroups = buildGroups(sorted.filter { it.amount < 0 }, "Other", absolute = true)
    )
}
```

- [ ] **Step 4: Add dynamic category dropdowns and `+ New Category` flow to the entry UI**

```kotlin
var selectedMode by remember { mutableStateOf(CategoryType.EXPENSE) }
var selectedIncomeCategory by remember { mutableStateOf("Salary") }
var showNewCategoryDialog by remember { mutableStateOf(false) }
var newCategoryName by remember { mutableStateOf("") }

val expenseCustomCategories by viewModel.expenseCustomCategories.collectAsState()
val incomeCustomCategories by viewModel.incomeCustomCategories.collectAsState()
val categoryOptions = when (selectedMode) {
    CategoryType.EXPENSE -> CategoryCatalog.dropdownOptions(CategoryType.EXPENSE, expenseCustomCategories)
    CategoryType.INCOME -> CategoryCatalog.dropdownOptions(CategoryType.INCOME, incomeCustomCategories)
}

if (selectedOption == CategoryCatalog.NEW_CATEGORY_OPTION) {
    showNewCategoryDialog = true
} else {
    selectedCategory = selectedOption
}
```

- [ ] **Step 5: Render grouped income sections with the same collapse interaction as expenses**

```kotlin
if (groupedFilteredTransactions.incomeGroups.isNotEmpty()) {
    item {
        Text(
            text = "Income By Category",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.fillMaxWidth()
        )
    }
}

items(items = groupedFilteredTransactions.incomeGroups, key = { "income-${it.category}" }) { group ->
    val expanded = expandedCategories["income-${group.category}"] ?: true
    CategoryGroupCard(
        title = group.category,
        subtitle = "${group.transactions.size} incomes - ${String.format("%.2f €", group.total)}",
        transactions = group.transactions,
        isExpanded = expanded,
        onToggle = { expandedCategories["income-${group.category}"] = !expanded },
        dateFormat = dateFormat,
        onDelete = viewModel::deleteTransaction
    )
}
```

- [ ] **Step 6: Expose custom category flows and add-category actions from the view model**

```kotlin
val expenseCustomCategories = repository.expenseCustomCategories.stateIn(...)
val incomeCustomCategories = repository.incomeCustomCategories.stateIn(...)

fun addCustomCategory(type: CategoryType, name: String, onCreated: (String) -> Unit) {
    viewModelScope.launch {
        repository.addCustomCategory(type, name)?.let(onCreated)
    }
}
```

- [ ] **Step 7: Run grouped-history tests and one full unit test pass**

Run:

```bash
.\gradlew testDebugUnitTest --tests "com.example.startapp.domain.GroupedTransactionsTest" --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"
```

Expected: PASS, with grouped income history available and no regression in chart stats.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/GroupedTransactions.kt app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt app/src/test/java/com/example/startapp/domain/GroupedTransactionsTest.kt
git commit -m "feat: add grouped income history and custom category entry"
```

### Task 4: Add Income Pie Chart And Monthly Averages To Chart Stats

**Files:**
- Modify: `app/src/main/java/com/example/startapp/domain/model/ChartData.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`

- [ ] **Step 1: Write the failing chart aggregation tests**

```kotlin
@Test
fun calculateChartData_buildsIncomeSlicesIncludingDailySurplus() {
    val stats = viewModel.calculateChartDataForTest(
        snapshots = sampleSnapshots(),
        transactions = listOf(
            Transaction(amount = 1000.0, date = day(1), description = "stipendio", category = "Salary"),
            Transaction(amount = 100.0, date = day(2), description = "rimborso", category = "Refund"),
            Transaction(amount = -50.0, date = day(3), description = "spesa", category = "Food")
        ),
        daysToDisplay = 30,
        timeFrame = TimeFrame.Month,
        dailyIncrease = 20.0
    )

    assertTrue(stats.categoryIncome.any { it.category == "Daily Surplus" })
    assertTrue(stats.categoryIncome.any { it.category == "Salary" })
}

@Test
fun categorySlice_monthlyAverageIsNormalizedByCoveredDays() {
    assertEquals(304.375, calculateMonthlyAverage(total = 300.0, coveredDays = 30), 0.001)
}
```

- [ ] **Step 2: Run the chart tests to verify they fail**

Run:

```bash
.\gradlew testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"
```

Expected: FAIL because `categoryIncome` and `monthlyAverage` do not exist yet.

- [ ] **Step 3: Expand `ChartStats` to carry generic category slices for both domains**

```kotlin
data class CategorySlice(
    val category: String,
    val total: Double,
    val percentage: Double,
    val monthlyAverage: Double
)

data class ChartStats(
    val points: List<ChartPoint>,
    val totalExpenses: Double,
    val totalIncome: Double,
    val avgSurplus: Double,
    val surplusStdDev: Double,
    val expenseRatio: Double,
    val savingsRatio: Double,
    val categoryExpenses: List<CategorySlice>,
    val categoryIncome: List<CategorySlice>
)
```

- [ ] **Step 4: Add a reusable monthly-average helper and daily-surplus slice aggregation**

```kotlin
internal fun calculateMonthlyAverage(total: Double, coveredDays: Int): Double {
    return total / maxOf(coveredDays, 1) * 30.4375
}

private fun buildIncomeSlices(
    transactions: List<Transaction>,
    coveredDays: Int,
    dailyIncrease: Double
): List<CategorySlice> {
    val groupedIncome = transactions
        .filter { it.amount >= 0 }
        .groupBy { it.category.ifBlank { "Other Income" } }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toMutableMap()

    val dailySurplusTotal = dailyIncrease * coveredDays
    if (dailySurplusTotal > 0.0) {
        groupedIncome["Daily Surplus"] = (groupedIncome["Daily Surplus"] ?: 0.0) + dailySurplusTotal
    }

    val totalIncome = groupedIncome.values.sum()

    return groupedIncome.entries
        .sortedByDescending { it.value }
        .map { (category, total) ->
            CategorySlice(
                category = category,
                total = total,
                percentage = if (totalIncome > 0.0) total / totalIncome else 0.0,
                monthlyAverage = calculateMonthlyAverage(total, coveredDays)
            )
        }
}
```

- [ ] **Step 5: Thread `dailyIncrease` into chart calculation and render both pie charts**

```kotlin
combine(
    repository.dailySnapshots,
    repository.transactions,
    repository.daysToDisplay,
    repository.dailyIncrease,
    _timeFrame
) { snapshots, transactions, daysToDisplay, dailyIncrease, selectedTimeFrame ->
    calculateChartData(snapshots, transactions, daysToDisplay, selectedTimeFrame, dailyIncrease)
}
```

```kotlin
Text(
    text = "Income Categories",
    style = MaterialTheme.typography.titleMedium,
    modifier = Modifier.padding(top = 20.dp, bottom = 8.dp)
)

CategoryPieChart(
    title = "Income",
    slices = stats.categoryIncome
)

stats.categoryIncome.forEach { slice ->
    Text(
        text = "${slice.category}: %.2f € | %.2f%% | %.2f €/month".format(
            slice.total,
            slice.percentage * 100,
            slice.monthlyAverage
        )
    )
}
```

- [ ] **Step 6: Keep the ratio labels semantically correct while adding the new chart data**

```kotlin
Text(text = "Expense Ratio (Expenses/Gains): %.2f%%".format(stats.expenseRatio * 100))
Text(text = "Saving Ratio (Saved/Gains): %.2f%%".format(stats.savingsRatio * 100))
```

- [ ] **Step 7: Run chart tests and a full build**

Run:

```bash
.\gradlew testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest"
.\gradlew assembleDebug
```

Expected: PASS, with both pie charts backed by coherent stats and the build succeeding.

- [ ] **Step 8: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/ChartData.kt app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt
git commit -m "feat: add income pie chart and monthly averages"
```

### Task 5: Clean Up Repository Hygiene And Final Regression Checks

**Files:**
- Create: `README.md`
- Modify: `.gitignore`
- Test: whole project via Gradle and IDE diagnostics

- [ ] **Step 1: Write the missing repository files with concise but complete content**

`README.md`

```md
# DaySurp

DaySurp is an Android app for tracking personal surplus, expenses, incomes, grouped category history, and financial charts.

## Requirements

- Android Studio
- JDK 17
- Android SDK matching the project configuration

## Run

Use `.\gradlew assembleDebug`, then open the project in Android Studio and run the `app` configuration on a device or emulator.

## Features

- daily surplus tracking
- categorized expenses and incomes
- collapsible grouped history
- expense and income pie charts
- saving ratio and expense ratio analytics

## Data

Data is stored locally on-device using DataStore preferences and JSON serialization. Existing transactions are migrated in place when category rules evolve.
```

```gitignore
*.iml
.gradle/
.kotlin/
/build/
**/build/
/captures/
.externalNativeBuild/
.cxx/
/local.properties
local.properties
.DS_Store
.idea/caches/
.idea/libraries/
.idea/modules.xml
.idea/workspace.xml
.idea/navEditor.xml
.idea/assetWizardSettings.xml
.idea/vcs.xml
*.log
*.tmp
*_dump.json
```

- [ ] **Step 2: Verify whether `.env.template` is actually needed**

Rule:

```text
If no runtime environment variables are read anywhere in the codebase, do not create `.env.template`.
```

Run:

```bash
Select-String -Path .\app\src\main\java\**\*.kt -Pattern "System.getenv|BuildConfig\." -SimpleMatch
```

Expected: No environment-variable dependency requiring an `.env.template` file.

- [ ] **Step 3: Run full tests, build, and diagnostics**

Run:

```bash
.\gradlew testDebugUnitTest assembleDebug
```

Then check diagnostics for the edited files and fix anything new before handoff.

Expected: PASS with no new IDE diagnostics in modified files.

- [ ] **Step 4: Commit**

```bash
git add README.md .gitignore
git commit -m "docs: add repository hygiene and usage guide"
```

## Self-Review

- Spec coverage: income taxonomy, custom categories, grouped income history, income pie chart, monthly averages, synthetic `Daily Surplus`, and repo hygiene each map to a task above. No uncovered requirement remains.
- Placeholder scan: no `TODO`, `TBD`, or deferred implementation markers remain in the plan.
- Type consistency: `CategoryType`, `CategoryCatalog`, `CategorySlice`, `GroupedTransactionState.incomeGroups`, and `calculateMonthlyAverage()` are named consistently across tasks.
