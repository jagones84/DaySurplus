# Edit Transactions, Search/Filters, Local Backup/Restore, Better Analytics — Implementation Plan

> **For agentic workers:** REQUIRED SUB-SKILL: Use superpowers:subagent-driven-development (recommended) or superpowers:executing-plans to implement this plan task-by-task. Steps use checkbox (`- [ ]`) syntax for tracking.

**Goal:** Add transaction editing, history search/filters, local JSON backup/restore (single file, everything included), and improved analytics without changing the “only extra expenses” philosophy.

**Architecture:** Keep DataStore as the source of truth. Add repository primitives to update transactions and read/write full app state as a versioned JSON blob. UI stays in existing screens (Counter + Chart) with minimal extra navigation. All behavior changes are test-driven with unit tests.

**Tech Stack:** Kotlin, Jetpack Compose, Android DataStore Preferences, Gson, JUnit4, Gradle

---

## File Map

### New
- Create: `app/src/main/java/com/example/startapp/data/model/AppBackup.kt`
- Create: `app/src/main/java/com/example/startapp/data/backup/BackupCodec.kt`
- Create: `app/src/main/java/com/example/startapp/data/backup/BackupValidator.kt`
- Create: `app/src/main/java/com/example/startapp/data/backup/BackupRepository.kt`

### Modify
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

### Tests
- Create: `app/src/test/java/com/example/startapp/data/backup/BackupCodecTest.kt`
- Create: `app/src/test/java/com/example/startapp/data/backup/BackupValidatorTest.kt`
- Modify: `app/src/test/java/com/example/startapp/data/TransactionMigrationTest.kt`
- Create: `app/src/test/java/com/example/startapp/data/TransactionUpdateTest.kt`

---

## Data Definitions

### Backup File (single JSON)

`AppBackup` contains everything needed to restore state:
- schemaVersion (int)
- createdAtEpochMs (long)
- totalAmount (double)
- dailyIncrease (double)
- daysToDisplay (int)
- maxHistoryDays (int)
- transactions (list)
- dailySnapshots (list)
- expenseCustomCategories (list)
- incomeCustomCategories (list)

---

### Task 1: Repository — Update Existing Transaction (Edit) (TDD)

**Files:**
- Create: `app/src/test/java/com/example/startapp/data/TransactionUpdateTest.kt`
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`

- [ ] **Step 1: Write failing test for updating amount/description/category by transaction id**

Create test:

```kotlin
package com.example.startapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.startapp.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.io.File
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking

class TransactionUpdateTest {

    @Test
    fun updateTransaction_updatesTransactionAndAdjustsTotalAmount() = runBlocking {
        val repository = buildRepository()

        repository.updateTotalAmount(100.0)
        val original = Transaction(amount = -10.0, date = 1L, description = "old", category = "Food")
        repository.addTransaction(original)

        repository.updateTransaction(
            id = original.id,
            newAmount = -25.0,
            newDescription = "new",
            newCategory = "Leisure"
        )

        val stored = repository.transactions.first()
        assertEquals(1, stored.size)
        assertEquals(-25.0, stored.single().amount, 0.0001)
        assertEquals("new", stored.single().description)
        assertEquals("Leisure", stored.single().category)

        val total = repository.totalAmount.first()
        assertEquals(85.0, total, 0.0001)
    }

    private fun buildRepository(): CounterDataRepository {
        val tempFile = File.createTempFile("transaction-update-test", ".preferences_pb").apply { deleteOnExit() }
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(produceFile = { tempFile })
        return CounterDataRepository(dataStore)
    }
}
```

- [ ] **Step 2: Run the test to confirm it fails**

Run:

```bash
./gradlew testDebugUnitTest --tests "com.example.startapp.data.TransactionUpdateTest"
```

Expected: FAIL due to missing `updateTransaction`.

- [ ] **Step 3: Implement `updateTransaction(...)` in repository**

Implement a new suspended function:
- loads mutable list of transactions
- finds transaction by id
- computes delta in amount
- updates transaction fields
- writes list back
- adjusts `total_amount` by `-delta` (same logic used in delete, but applied to the delta between old/new amounts)

- [ ] **Step 4: Expose `updateTransaction(...)` from viewmodel**

Add:

```kotlin
fun updateTransaction(id: String, newAmount: Double, newDescription: String, newCategory: String)
```

- [ ] **Step 5: Run the test to confirm it passes**

Run:

```bash
./gradlew testDebugUnitTest --tests "com.example.startapp.data.TransactionUpdateTest"
```

Expected: PASS.

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/startapp/data/CounterDataRepository.kt app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt app/src/test/java/com/example/startapp/data/TransactionUpdateTest.kt
git commit -m "feat: allow editing transactions in repository and viewmodel"
```

---

### Task 2: UI — Edit Transaction Dialog (TDD-lite)

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`

- [ ] **Step 1: Add an Edit icon button to each TransactionRow**

Add an edit button next to delete.

- [ ] **Step 2: Add a dialog that edits amount/description/category**

Rules:
- if amount is edited, keep sign consistent with the original transaction type (income >= 0, expense < 0)
- category dropdown uses the correct catalog (Income vs Expense) + custom categories

- [ ] **Step 3: Wire dialog confirm to `viewModel.updateTransaction(...)`**

- [ ] **Step 4: Run compile check**

```bash
./gradlew assembleDebug
```

Expected: PASS.

---

### Task 3: History — Search + Filters (TDD-lite)

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`

- [ ] **Step 1: Add search text field above History**

- [ ] **Step 2: Add scope filter (All / Income / Expense)**

- [ ] **Step 3: Add category filter (All + dropdown)**

- [ ] **Step 4: Apply filters before grouping**

Expected: grouped income/expense reflect filters.

- [ ] **Step 5: Run compile check**

```bash
./gradlew assembleDebug
```

---

### Task 4: Backup — Data Model + Codec (TDD)

**Files:**
- Create: `app/src/main/java/com/example/startapp/data/model/AppBackup.kt`
- Create: `app/src/main/java/com/example/startapp/data/backup/BackupCodec.kt`
- Create: `app/src/test/java/com/example/startapp/data/backup/BackupCodecTest.kt`

- [ ] **Step 1: Write failing encode/decode test**

```kotlin
package com.example.startapp.data.backup

import com.example.startapp.data.model.AppBackup
import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test

class BackupCodecTest {
    @Test
    fun codec_roundTrip_preservesAllFields() {
        val backup = AppBackup(
            schemaVersion = 1,
            createdAtEpochMs = 123L,
            totalAmount = 10.0,
            dailyIncrease = 20.0,
            daysToDisplay = 30,
            maxHistoryDays = 900,
            transactions = listOf(Transaction(amount = -1.0, date = 1L, description = "x", category = "Food")),
            dailySnapshots = listOf(DailySnapshot(date = 1L, amount = 0.0)),
            expenseCustomCategories = listOf("Casa Vacanze"),
            incomeCustomCategories = listOf("Dividends")
        )

        val json = BackupCodec.encode(backup)
        val decoded = BackupCodec.decode(json)

        assertEquals(backup, decoded)
    }
}
```

- [ ] **Step 2: Run test to confirm it fails**

```bash
./gradlew testDebugUnitTest --tests "com.example.startapp.data.backup.BackupCodecTest"
```

- [ ] **Step 3: Implement `AppBackup` + `BackupCodec` using Gson**

- [ ] **Step 4: Run test to confirm it passes**

```bash
./gradlew testDebugUnitTest --tests "com.example.startapp.data.backup.BackupCodecTest"
```

- [ ] **Step 5: Commit**

```bash
git add app/src/main/java/com/example/startapp/data/model/AppBackup.kt app/src/main/java/com/example/startapp/data/backup/BackupCodec.kt app/src/test/java/com/example/startapp/data/backup/BackupCodecTest.kt
git commit -m "feat: add backup model and json codec"
```

---

### Task 5: Backup — Validation + Repository Integration (TDD)

**Files:**
- Create: `app/src/main/java/com/example/startapp/data/backup/BackupValidator.kt`
- Create: `app/src/main/java/com/example/startapp/data/backup/BackupRepository.kt`
- Create: `app/src/test/java/com/example/startapp/data/backup/BackupValidatorTest.kt`
- Modify: `app/src/main/java/com/example/startapp/data/CounterDataRepository.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt`

- [ ] **Step 1: Write failing validator test**

Test cases:
- rejects schemaVersion != 1
- rejects null/empty transactions list when JSON present but malformed

- [ ] **Step 2: Implement validator**

- [ ] **Step 3: Add repository functions**

Add:
- `suspend fun exportBackup(): AppBackup`
- `suspend fun importBackup(backup: AppBackup)`

Implementation notes:
- export reads current DataStore values (including lists) and returns AppBackup
- import overwrites DataStore keys for all fields
- after import, call `normalizeStoredTransactions()` once to ensure taxonomy stays clean

- [ ] **Step 4: Expose in CounterViewModel**

Add:
- `suspend fun exportBackupJson(): String`
- `suspend fun importBackupJson(json: String): Boolean` (returns false on validation failure)

- [ ] **Step 5: Run unit tests**

```bash
./gradlew testDebugUnitTest --tests "com.example.startapp.data.backup.BackupValidatorTest"
```

- [ ] **Step 6: Commit**

```bash
git add app/src/main/java/com/example/startapp/data/CounterDataRepository.kt app/src/main/java/com/example/startapp/data/backup app/src/main/java/com/example/startapp/ui/viewmodel/CounterViewModel.kt app/src/test/java/com/example/startapp/data/backup/BackupValidatorTest.kt
git commit -m "feat: export/import full backup via datastore"
```

---

### Task 6: UI — Export/Import Local File (SAF)

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/screen/CounterScreen.kt`

- [ ] **Step 1: Add two buttons: Export Backup / Import Backup**

- [ ] **Step 2: Use Activity Result APIs**

Export:
- `ActivityResultContracts.CreateDocument("application/json")`
- write JSON to uri

Import:
- `ActivityResultContracts.OpenDocument()`
- read JSON from uri
- call `viewModel.importBackupJson(json)`
- show a simple success/failure dialog

- [ ] **Step 3: Run build**

```bash
./gradlew assembleDebug
```

---

### Task 7: Analytics — Improve Chart Screen

**Files:**
- Modify: `app/src/main/java/com/example/startapp/ui/viewmodel/ChartViewModel.kt`
- Modify: `app/src/main/java/com/example/startapp/ui/screen/ChartScreen.kt`

- [ ] **Step 1: Add “Top 5 descriptions” for expenses**

Implementation:
- group expense transactions by normalized description (trim/lower/condense spaces)
- sum absolute values
- sort desc, take 5

- [ ] **Step 2: Add “Top 5 expense days”**

Implementation:
- group expense transactions by date (yyyyMMdd)
- sum absolute values
- sort desc, take 5

- [ ] **Step 3: Render the new blocks under the charts**

- [ ] **Step 4: Run tests + build**

```bash
./gradlew testDebugUnitTest assembleDebug
```

---

## Self-Review

- Coverage: edit tx, search/filters, backup/restore single file, analytics improvements are covered.
- Placeholders: no TBD/TODO; UI steps are “TDD-lite” but include concrete compile checks.
- Consistency: backup schemaVersion is fixed at 1 across model/validator.

