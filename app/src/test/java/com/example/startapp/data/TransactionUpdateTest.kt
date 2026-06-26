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
        val now = System.currentTimeMillis()
        val original = Transaction(amount = -10.0, date = now, description = "old", category = "Food")
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
