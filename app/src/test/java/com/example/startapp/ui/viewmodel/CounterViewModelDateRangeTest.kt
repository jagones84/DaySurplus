package com.example.startapp.ui.viewmodel

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.startapp.data.CounterDataRepository
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.createNormalizedDateRange
import kotlinx.coroutines.delay
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import java.io.File

class CounterViewModelDateRangeTest {

    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun updateDateRangeFilter_exposesNormalizedRangeAndFiltersGroupedTransactions() = runBlocking {
        val repository = buildRepository()
        val viewModel = CounterViewModel(repository)
        val june10 = 1_718_006_400_000L
        val june20 = 1_718_092_800_000L
        val july10 = 1_720_684_800_000L

        repository.addTransaction(Transaction(amount = -20.0, date = june10, description = "pizza", category = "Food"))
        repository.addTransaction(Transaction(amount = 100.0, date = june20, description = "salary", category = "Salary"))
        repository.addTransaction(Transaction(amount = -50.0, date = july10, description = "old", category = "Shopping"))

        viewModel.updateDateRangeFilter(startEpochMs = june10, endEpochMs = june20)

        val expected = createNormalizedDateRange(june10, june20)
        val range = withTimeout(5_000) {
            while (viewModel.dateRangeFilter.value != expected) {
                delay(10)
            }
            viewModel.dateRangeFilter.value
        }
        val grouped = withTimeout(5_000) {
            while (
                viewModel.groupedTransactions.value.incomeGroups.map { it.category } != listOf("Salary") ||
                viewModel.groupedTransactions.value.expenseGroups.map { it.category } != listOf("Food")
            ) {
                delay(10)
            }
            viewModel.groupedTransactions.value
        }

        assertEquals(expected, range)
        assertEquals(listOf("Salary"), grouped.incomeGroups.map { it.category })
        assertEquals(listOf("Food"), grouped.expenseGroups.map { it.category })
    }

    private fun buildRepository(): CounterDataRepository {
        val tempFile = File.createTempFile("counter-viewmodel-date-range-test", ".preferences_pb").apply { deleteOnExit() }
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(produceFile = { tempFile })
        return CounterDataRepository(dataStore)
    }
}
