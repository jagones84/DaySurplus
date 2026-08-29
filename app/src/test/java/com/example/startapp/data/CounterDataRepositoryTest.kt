package com.example.startapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import com.example.startapp.data.backup.ImportMode
import com.example.startapp.data.model.AppBackup
import com.example.startapp.domain.model.CategoryType
import com.example.startapp.domain.model.DailySnapshot
import com.example.startapp.domain.model.Transaction
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File
import java.util.Calendar

class CounterDataRepositoryTest {

    @Test
    fun recordTransaction_updatesTotalAndAppendsAtomically() = runBlocking {
        val repository = buildRepository()
        repository.updateTotalAmount(100.0)

        repository.recordTransaction(
            transaction = Transaction(
                amount = -10.0,
                date = System.currentTimeMillis(),
                description = "coffee",
                category = "Food"
            ),
            totalDelta = -10.0
        )

        assertEquals(90.0, repository.totalAmount.first(), 0.0001)
        assertEquals(1, repository.transactions.first().size)
        assertEquals("coffee", repository.transactions.first().single().description)
    }

    @Test
    fun transactionsFlow_returnsEmptyListOnCorruptedJson() = runBlocking {
        val repository = buildRepository()

        repository.dataStoreForTest.edit { preferences ->
            preferences[stringPreferencesKey("transactions")] = "{not-valid-json"
        }

        assertEquals(emptyList<Transaction>(), repository.transactions.first())
    }

    @Test
    fun snapshotsFlow_returnsEmptyListOnCorruptedJson() = runBlocking {
        val repository = buildRepository()

        repository.dataStoreForTest.edit { preferences ->
            preferences[stringPreferencesKey("daily_snapshots")] = "[{broken"
        }

        assertEquals(emptyList<DailySnapshot>(), repository.dailySnapshots.first())
    }

    @Test
    fun importBackup_mergeMode_unionsDataAndKeepsCurrentSettings() = runBlocking {
        val repository = buildRepository()
        repository.updateTotalAmount(100.0)
        repository.updateDailyIncrease(5.0)
        repository.addCustomCategory(CategoryType.EXPENSE, "Foo")
        repository.addTransaction(
            Transaction(id = "A", amount = -10.0, date = jan10At(12), description = "old", category = "Food")
        )
        repository.addDailySnapshot(DailySnapshot(date = jan10At(12), amount = 90.0))

        val backup = AppBackup(
            schemaVersion = 1,
            createdAtEpochMs = 1L,
            totalAmount = 999.0,
            dailyIncrease = 42.0,
            daysToDisplay = 7,
            maxHistoryDays = 900,
            transactions = listOf(
                Transaction(id = "A", amount = -99.0, date = jan10At(12), description = "backup", category = "Food"),
                Transaction(id = "B", amount = -5.0, date = jan11At(9), description = "new", category = "Leisure")
            ),
            dailySnapshots = listOf(
                DailySnapshot(date = jan10At(18), amount = 500.0),
                DailySnapshot(date = jan11At(12), amount = 200.0)
            ),
            expenseCustomCategories = listOf("Foo", "Bar"),
            incomeCustomCategories = listOf("Bonus")
        )

        repository.importBackup(backup, ImportMode.MERGE)

        assertEquals(100.0, repository.totalAmount.first(), 0.0001)
        assertEquals(5.0, repository.dailyIncrease.first(), 0.0001)

        val mergedTransactions = repository.transactions.first().sortedBy { it.id }
        assertEquals(listOf("A", "B"), mergedTransactions.map { it.id })
        assertEquals(-10.0, mergedTransactions.first { it.id == "A" }.amount, 0.0001)

        val mergedSnapshots = repository.dailySnapshots.first().sortedBy { it.date }
        assertEquals(listOf(500.0, 200.0), mergedSnapshots.map { it.amount })

        val expenseCustom = repository.expenseCustomCategories.first()
        assertTrue(expenseCustom.contains("Foo"))
        assertTrue(expenseCustom.contains("Bar"))
        assertTrue(repository.incomeCustomCategories.first().contains("Bonus"))
    }

    @Test
    fun importBackup_replaceMode_replacesEverything() = runBlocking {
        val repository = buildRepository()
        repository.updateTotalAmount(100.0)
        repository.addTransaction(
            Transaction(id = "A", amount = -10.0, date = jan10At(12), description = "old", category = "Food")
        )

        val backup = AppBackup(
            schemaVersion = 1,
            createdAtEpochMs = 1L,
            totalAmount = 55.0,
            dailyIncrease = 3.0,
            daysToDisplay = 30,
            maxHistoryDays = 900,
            transactions = listOf(
                Transaction(id = "B", amount = -5.0, date = jan11At(9), description = "only", category = "Food")
            ),
            dailySnapshots = emptyList(),
            expenseCustomCategories = emptyList(),
            incomeCustomCategories = emptyList()
        )

        repository.importBackup(backup, ImportMode.REPLACE)

        assertEquals(55.0, repository.totalAmount.first(), 0.0001)
        assertEquals(listOf("B"), repository.transactions.first().map { it.id })
    }

    @Test
    fun renameCustomCategory_normalizesRejectsConflictsAndUpdatesList() = runBlocking {
        val repository = buildRepository()
        repository.addCustomCategory(CategoryType.EXPENSE, "My Cat")

        val renamed = repository.renameCustomCategory(CategoryType.EXPENSE, "My Cat", "best   cat")

        assertEquals("Best Cat", renamed)
        assertFalse(repository.expenseCustomCategories.first().contains("My Cat"))
        assertTrue(repository.expenseCustomCategories.first().contains("Best Cat"))
    }

    @Test
    fun renameCustomCategory_rejectsBuiltInConflict() = runBlocking {
        val repository = buildRepository()
        repository.addCustomCategory(CategoryType.EXPENSE, "My Cat")

        val renamed = repository.renameCustomCategory(CategoryType.EXPENSE, "My Cat", "Food")

        assertNull(renamed)
        assertTrue(repository.expenseCustomCategories.first().contains("My Cat"))
    }

    @Test
    fun deleteCustomCategory_removesFromListOnly() = runBlocking {
        val repository = buildRepository()
        repository.addCustomCategory(CategoryType.INCOME, "Side Job")
        repository.addTransaction(
            Transaction(amount = 10.0, date = jan10At(12), description = "gig", category = "Side Job")
        )

        repository.deleteCustomCategory(CategoryType.INCOME, "Side Job")

        assertFalse(repository.incomeCustomCategories.first().contains("Side Job"))
        val history = repository.transactions.first().single()
        assertEquals("Side Job", history.category)
    }

    private fun buildRepository(): CounterDataRepository {
        val tempFile = File.createTempFile("counter-repo-test", ".preferences_pb").apply { deleteOnExit() }
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(produceFile = { tempFile })
        return CounterDataRepository(dataStore)
    }

    private fun jan10At(hour: Int): Long = calendarAt(2026, Calendar.JANUARY, 10, hour)

    private fun jan11At(hour: Int): Long = calendarAt(2026, Calendar.JANUARY, 11, hour)

    private fun calendarAt(year: Int, month: Int, day: Int, hour: Int): Long {
        return Calendar.getInstance().apply {
            set(year, month, day, hour, 0, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
