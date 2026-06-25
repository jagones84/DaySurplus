# Category Taxonomy Cleanup Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Replace the current mixed category system with a cleaned macro-taxonomy, migrate legacy records safely, minimize `Other`, and align chart ratio naming/calculation with the user's intended meaning.

**Architecture:** Keep the existing DataStore-backed transaction pipeline, but rebuild category inference around a smaller final taxonomy with priority overrides, heuristics, and explicit legacy-category collapse rules. Reuse the repository normalization path to rewrite only transaction categories, then make UI grouping, selector options, and chart labels consume the new taxonomy consistently.

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore Preferences, Gson, JUnit, Gradle, adb

---

### Task 1: Lock The Final Taxonomy In Tests

**Files:**
- Modify: `app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt`
- Modify: `app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt`

- [ ] **Step 1: Add failing taxonomy-catalog and collapse tests**

```kotlin
@Test
fun activeExpenseCatalogMatchesFinalTaxonomy() {
    assertEquals(
        listOf(
            "Food",
            "Transport",
            "Health",
            "Shopping",
            "Gaming",
            "Leisure",
            "Travel",
            "Digital",
            "Education",
            "Taxes",
            "Gifts",
            "Pets",
            "Other"
        ),
        ExpenseCategory.expenseCategories.map { it.label }
    )
}
```

and

```kotlin
@Test
fun legacyNamedCategoriesCollapseIntoNewTaxonomy() {
    assertEquals(
        ExpenseCategory.SHOPPING.label,
        normalizeTransactionCategory(-25.0, "lampada", "Home")
    )
    assertEquals(
        ExpenseCategory.DIGITAL.label,
        normalizeTransactionCategory(-10.0, "trae", "Work")
    )
    assertEquals(
        ExpenseCategory.DIGITAL.label,
        normalizeTransactionCategory(-10.0, "patreon", "Subscriptions")
    )
}
```

- [ ] **Step 2: Run tests to verify they fail**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ExpenseCategoryTest" --tests "com.example.startapp.data.TransactionMigrationTest" --no-daemon`
Expected: FAIL because `Home`, `Work`, and `Subscriptions` are still present and not collapsed.

- [ ] **Step 3: Implement the minimal taxonomy rename/removal**

```kotlin
enum class ExpenseCategory(
    val label: String,
    val keywords: List<String>
) {
    FOOD("Food", ...),
    TRANSPORT("Transport", ...),
    HEALTH("Health", ...),
    SHOPPING("Shopping", ...),
    GAMING("Gaming", ...),
    LEISURE("Leisure", ...),
    TRAVEL("Travel", ...),
    DIGITAL("Digital", ...),
    EDUCATION("Education", ...),
    TAXES("Taxes", ...),
    GIFTS("Gifts", ...),
    PETS("Pets", ...),
    OTHER("Other", emptyList()),
    INCOME("Income", emptyList());
}
```

and ensure:

```kotlin
val expenseCategories: List<ExpenseCategory> = listOf(
    FOOD,
    TRANSPORT,
    HEALTH,
    SHOPPING,
    GAMING,
    LEISURE,
    TRAVEL,
    DIGITAL,
    EDUCATION,
    TAXES,
    GIFTS,
    PETS,
    OTHER
)
```

- [ ] **Step 4: Run tests to verify they pass**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ExpenseCategoryTest" --tests "com.example.startapp.data.TransactionMigrationTest" --no-daemon`
Expected: PASS for taxonomy catalog and legacy collapse cases.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt
git commit -m "feat: replace legacy expense taxonomy"
```

### Task 2: Encode Real-World Macro-Categorization Rules

**Files:**
- Modify: `app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt`
- Modify: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`

- [ ] **Step 1: Add failing real-data examples for the corrected categories**

```kotlin
@Test
fun realDescriptionsMapToFoodShoppingLeisureAndDigital() {
    assertEquals(ExpenseCategory.FOOD.label, ExpenseCategory.inferFromDescription("cena"))
    assertEquals(ExpenseCategory.FOOD.label, ExpenseCategory.inferFromDescription("prosecco"))
    assertEquals(ExpenseCategory.LEISURE.label, ExpenseCategory.inferFromDescription("noleggio snow"))
    assertEquals(ExpenseCategory.SHOPPING.label, ExpenseCategory.inferFromDescription("antenne wireless"))
    assertEquals(ExpenseCategory.DIGITAL.label, ExpenseCategory.inferFromDescription("patreon"))
    assertEquals(ExpenseCategory.DIGITAL.label, ExpenseCategory.inferFromDescription("apple tv"))
    assertEquals(ExpenseCategory.DIGITAL.label, ExpenseCategory.inferFromDescription("trae"))
}
```

- [ ] **Step 2: Run the focused test to verify it fails**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ExpenseCategoryTest.realDescriptionsMapToFoodShoppingLeisureAndDigital" --no-daemon`
Expected: FAIL because current rules still send some entries to `Other`, `Home`, `Work`, or `Subscriptions`.

- [ ] **Step 3: Implement override and heuristic cleanup**

```kotlin
private val prioritizedRules: List<CategoryRule> = listOf(
    CategoryRule(HEALTH, listOf("isber", "medica", "medico", "visita", "clinica", "ospedale", "farmacia", "farmaco", "siringhe", "integratori", "omega3", "vitamina")),
    CategoryRule(SHOPPING, listOf("temu", "amazon", "qmazon", "amz", "antenne wireless", "caricacell", "cavo usb hdmi")),
    CategoryRule(GAMING, listOf("gaming", "game", "god of war", "switch", "gdr", "steam")),
    CategoryRule(GIFTS, listOf("regalo")),
    CategoryRule(DIGITAL, listOf("patreon", "apple tv", "gemini", "trae", "openrouter", "google ai", "google cloud", "telegram", "social app", "comfyui", "voxta", "vam model")),
    CategoryRule(TRAVEL, listOf("viaggio", "cervinia", "gita", "bergamo", "lainate")),
    CategoryRule(TRANSPORT, listOf("taxi", "area c", "parcheggio")),
    CategoryRule(LEISURE, listOf("serata", "uscita", "capodanno", "cinema", "hard rock", "bowling", "roknroll", "weekend", "noleggio snow")),
    CategoryRule(FOOD, listOf("cena", "prosecco", "deliveroo", "hamburger", "kebab", "poke", "piadina", "mc donald", "giappo", "pranzo", "fortimel"))
)
```

and move generic purchase terms that were in `Home` into `Shopping`, not into a separate category.

- [ ] **Step 4: Run the focused test again**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ExpenseCategoryTest.realDescriptionsMapToFoodShoppingLeisureAndDigital" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt app/src/test/java/com/example/startapp/ExpenseCategoryTest.kt
git commit -m "feat: classify real world descriptions into final taxonomy"
```

### Task 3: Reclassify Stored Legacy Transactions Safely

**Files:**
- Modify: `app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt`
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`

- [ ] **Step 1: Add failing migration tests for legacy categories and residual Other**

```kotlin
@Test
fun legacyCategoriesAndOldOtherEntriesArePromotedToFinalBuckets() {
    assertEquals(
        ExpenseCategory.FOOD.label,
        normalizeTransactionCategory(-12.0, "cena", ExpenseCategory.OTHER.label)
    )
    assertEquals(
        ExpenseCategory.LEISURE.label,
        normalizeTransactionCategory(-20.0, "noleggio snow", ExpenseCategory.OTHER.label)
    )
    assertEquals(
        ExpenseCategory.SHOPPING.label,
        normalizeTransactionCategory(-18.0, "antenne wireless", ExpenseCategory.OTHER.label)
    )
    assertEquals(
        ExpenseCategory.DIGITAL.label,
        normalizeTransactionCategory(-8.0, "patreon", "Subscriptions")
    )
}
```

- [ ] **Step 2: Run the migration test to verify it fails**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests "com.example.startapp.data.TransactionMigrationTest" --no-daemon`
Expected: FAIL because normalization currently only force-reclassifies `Other`.

- [ ] **Step 3: Extend normalization to collapse removed legacy categories**

```kotlin
private val collapsedLegacyCategories = mapOf(
    "Home" to ExpenseCategory.SHOPPING.label,
    "Work" to ExpenseCategory.DIGITAL.label,
    "Subscriptions" to ExpenseCategory.DIGITAL.label
)
```

and update:

```kotlin
internal fun normalizeTransactionCategory(amount: Double, description: String, category: String?): String {
    val collapsed = category?.let { collapsedLegacyCategories[it] }
    if (collapsed != null) {
        return if (amount >= 0) ExpenseCategory.INCOME.label else ExpenseCategory.inferFromDescription(description).takeIf { it != ExpenseCategory.OTHER.label } ?: collapsed
    }

    val shouldReclassifyOther = category == ExpenseCategory.OTHER.label && amount < 0
    if (!category.isNullOrBlank() && !shouldReclassifyOther) {
        return category
    }
    return if (amount >= 0) ExpenseCategory.INCOME.label else ExpenseCategory.inferFromDescription(description)
}
```

- [ ] **Step 4: Run the migration test again**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests "com.example.startapp.data.TransactionMigrationTest" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/data/CounterDataRepository.kt app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt
git commit -m "fix: migrate legacy transactions into cleaned taxonomy"
```

### Task 4: Align UI Category Selector And Group Labels

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- Modify: `app/src/main/java/com/example/startapp/domain/model/GroupedTransactions.kt`

- [ ] **Step 1: Add a small regression assertion for grouped output if a suitable test file exists, otherwise use a build verification**

```kotlin
// Prefer extending the grouped-transactions test set so that "Digital" and "Shopping" appear,
// while "Home", "Work", and "Subscriptions" do not.
```

- [ ] **Step 2: Run baseline build before UI edits**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat assembleDebug --no-daemon`
Expected: PASS

- [ ] **Step 3: Ensure the selector and grouping use only the final taxonomy**

```kotlin
var selectedExpenseCategory by remember { mutableStateOf(ExpenseCategory.FOOD.label) }
```

and verify the dropdown iterates only:

```kotlin
ExpenseCategory.expenseCategories.forEach { category -> ... }
```

with no legacy categories left in `expenseCategories`.

- [ ] **Step 4: Run build verification**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat assembleDebug --no-daemon`
Expected: PASS and the category selector exposes `Digital` instead of `Work`/`Subscriptions`, with no `Home`.

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt app/src/main/java/com/example/startapp/domain/model/GroupedTransactions.kt
git commit -m "feat: expose cleaned taxonomy in history and input ui"
```

### Task 5: Fix Ratio Semantics In Chart Stats

**Files:**
- Modify: `app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

- [ ] **Step 1: Add failing tests for saving-ratio meaning**

```kotlin
@Test
fun savingRatioRepresentsSavedFractionOfTotalIncome() {
    val now = 1_700_000_000_000L
    val snapshots = listOf(
        DailySnapshot(date = now - 3_000L, amount = 100.0),
        DailySnapshot(date = now - 2_000L, amount = 120.0),
        DailySnapshot(date = now - 1_000L, amount = 140.0)
    )
    val transactions = listOf(
        Transaction(amount = -10.0, date = now - 2_500L, description = "cena", category = ExpenseCategory.FOOD.label),
        Transaction(amount = -5.0, date = now - 1_500L, description = "steam", category = ExpenseCategory.GAMING.label)
    )

    val totals = calculateOverallTotals(snapshots, transactions)

    assertEquals(15.0 / 55.0, totals.expenseRatio, 0.0001)
    assertEquals(1 - (15.0 / 55.0), calculateSavingsRatio(totals.expenseRatio), 0.0001)
}
```

- [ ] **Step 2: Run the chart test to verify it fails**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest" --no-daemon`
Expected: FAIL because the helper `calculateSavingsRatio` does not exist or labels are still semantically mixed.

- [ ] **Step 3: Implement the ratio helper and relabel UI**

```kotlin
internal fun calculateSavingsRatio(expenseRatio: Double): Double {
    return 1 - expenseRatio
}
```

and in chart aggregation:

```kotlin
val expenseRatio = overallTotals.expenseRatio
val savingsRatio = if (overallTotals.totalIncome > 0.0) calculateSavingsRatio(expenseRatio) else 0.0
```

and in the screen:

```kotlin
Text("Expense Ratio (Expenses/Gains): %.2f%%".format(stats.expenseRatio * 100))
Text("Saving Ratio (Saved/Gains): %.2f%%".format(stats.savingsRatio * 100))
```

- [ ] **Step 4: Run the chart test again**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest --tests "com.example.startapp.ui.viewmodel.ChartStatsTest" --no-daemon`
Expected: PASS

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt app/src/test/java/com/example/startapp/ui/viewmodel/ChartStatsTest.kt
git commit -m "fix: align saving ratio with saved fraction semantics"
```

### Task 6: Final Verification On Real Data

**Files:**
- Review: `app/src/main/java/com/example/startapp/domain/model/ExpenseCategory.kt`
- Review: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Review: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

- [ ] **Step 1: Run full unit tests and debug build**

Run: `$env:JAVA_HOME='C:\Program Files\Android\Android Studio\jbr'; $env:Path="$env:JAVA_HOME\bin;$env:Path"; .\gradlew.bat testDebugUnitTest assembleDebug --no-daemon`
Expected: PASS

- [ ] **Step 2: Install and launch the app on the connected device**

```bash
& 'C:\Users\giova\AppData\Local\Android\Sdk\platform-tools\adb.exe' install -r app/build/outputs/apk/debug/app-debug.apk
& 'C:\Users\giova\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell am force-stop com.example.startapp
& 'C:\Users\giova\AppData\Local\Android\Sdk\platform-tools\adb.exe' shell am start -n com.example.startapp/.MainActivity
```

- [ ] **Step 3: Verify post-migration examples from real stored data**

```text
Confirm these examples after app startup:
- cena -> Food
- prosecco -> Food
- noleggio snow -> Leisure
- antenne wireless -> Shopping
- trae / patreon / apple tv -> Digital
- no visible Home, Work, or Subscriptions groups remain
- Other is reduced to only genuinely unclear records
```

- [ ] **Step 4: Check IDE diagnostics on touched files**

```text
ExpenseCategory.kt
CounterDataRepository.kt
CounterScreen.kt
ChartViewModel.kt
ChartScreen.kt
ExpenseCategoryTest.kt
TransactionMigrationTest.kt
ChartStatsTest.kt
```

- [ ] **Step 5: Prepare handoff summary**

```text
Summarize taxonomy cleanup, legacy migration, ratio semantics fix, tests/build/device verification, and any residual ambiguous descriptions still left in Other.
```
