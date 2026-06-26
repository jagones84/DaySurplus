package com.example.startapp.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.PreferenceDataStoreFactory
import androidx.datastore.preferences.core.Preferences
import com.example.startapp.domain.createNormalizedDateRange
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertTrue
import org.junit.Test
import java.io.File

class DateRangeFilterRepositoryTest {

    @Test
    fun dateRange_defaultsToRecentWindowWhenUnset() = runBlocking {
        val repository = buildRepository()

        val range = repository.dateRangeFilter.first()

        assertTrue(range.endEpochMs >= range.startEpochMs)
    }

    @Test
    fun updateDateRange_persistsNormalizedBounds() = runBlocking {
        val repository = buildRepository()
        val expected = createNormalizedDateRange(
            startEpochMs = 1_718_265_600_000L,
            endEpochMs = 1_718_006_400_000L
        )

        repository.updateDateRangeFilter(
            startEpochMs = 1_718_265_600_000L,
            endEpochMs = 1_718_006_400_000L
        )

        val range = repository.dateRangeFilter.first()

        assertEquals(expected.startEpochMs, range.startEpochMs)
        assertEquals(expected.endEpochMs, range.endEpochMs)
    }

    private fun buildRepository(): CounterDataRepository {
        val tempFile = File.createTempFile("date-range-filter-test", ".preferences_pb").apply { deleteOnExit() }
        val dataStore: DataStore<Preferences> = PreferenceDataStoreFactory.create(produceFile = { tempFile })
        return CounterDataRepository(dataStore)
    }
}
