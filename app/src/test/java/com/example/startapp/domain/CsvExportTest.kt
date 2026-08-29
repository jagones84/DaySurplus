package com.example.startapp.domain

import com.example.startapp.domain.model.Transaction
import org.junit.Assert.assertEquals
import org.junit.Test
import java.util.Calendar
import java.util.TimeZone

class CsvExportTest {

    @Test
    fun transactionsToCsv_emitsHeaderOnlyWhenEmpty() {
        assertEquals("id,date_iso,amount,description,category", transactionsToCsv(emptyList()))
    }

    @Test
    fun transactionsToCsv_writesOneRowPerTransactionInUtcIsoFormat() {
        val csv = transactionsToCsv(
            listOf(
                Transaction(id = "abc", amount = -12.5, date = utcAt(2026, 8, 5, 12, 30), description = "lunch", category = "Food")
            )
        )

        val lines = csv.lines()
        assertEquals(2, lines.size)
        assertEquals("id,date_iso,amount,description,category", lines[0])
        assertEquals("abc,2026-08-05T12:30:00Z,-12.5,lunch,Food", lines[1])
    }

    @Test
    fun transactionsToCsv_escapesCommasQuotesAndNewlines() {
        val csv = transactionsToCsv(
            listOf(
                Transaction(
                    id = "id1",
                    amount = 3.0,
                    date = utcAt(2026, 1, 2, 0, 0),
                    description = "weird, \"desc\"\nline",
                    category = "Other"
                )
            )
        )

        val dataLine = csv.removePrefix("id,date_iso,amount,description,category\n")
        val expected = "id1,2026-01-02T00:00:00Z,3.0,\"weird, \"\"desc\"\"\nline\",Other"
        assertEquals(expected, dataLine)
    }

    private fun utcAt(year: Int, month: Int, day: Int, hour: Int, minute: Int): Long {
        return Calendar.getInstance(TimeZone.getTimeZone("UTC")).apply {
            set(year, month - 1, day, hour, minute, 0)
            set(Calendar.MILLISECOND, 0)
        }.timeInMillis
    }
}
