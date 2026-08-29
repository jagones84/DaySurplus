package com.example.startapp.domain

import com.example.startapp.domain.model.DailySnapshot
import com.example.startapp.domain.model.Transaction
import com.example.startapp.domain.model.AnalysisWindowPreset
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

    @Test
    fun presetDateRange_usesRequestedInclusiveDayCount() {
        val now = 1_718_179_200_000L

        val range = presetDateRange(days = 30, now = now)

        assertEquals(30, calculateCoveredDays(range))
    }

    @Test
    fun matchingAnalysisWindowPreset_returnsPresetForStandardWindow() {
        val now = 1_718_179_200_000L
        val range = presetDateRange(days = 30, now = now)

        val preset = matchingAnalysisWindowPreset(range, now)

        assertEquals(AnalysisWindowPreset.DAYS_30, preset)
    }

    @Test
    fun matchingAnalysisWindowPreset_returnsCustomForManualRange() {
        val now = 1_718_179_200_000L
        val range = createNormalizedDateRange(
            startEpochMs = 1_718_006_400_000L,
            endEpochMs = 1_718_179_200_000L
        )

        val preset = matchingAnalysisWindowPreset(range, now)

        assertEquals(AnalysisWindowPreset.CUSTOM, preset)
    }
}
