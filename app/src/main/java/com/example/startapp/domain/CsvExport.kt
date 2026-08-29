package com.example.startapp.domain

import com.example.startapp.domain.model.Transaction
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import java.util.TimeZone

private const val CSV_HEADER = "id,date_iso,amount,description,category"

fun transactionsToCsv(transactions: List<Transaction>): String {
    val isoFormat = SimpleDateFormat("yyyy-MM-dd'T'HH:mm:ss'Z'", Locale.US).apply {
        timeZone = TimeZone.getTimeZone("UTC")
    }
    val lines = transactions.map { transaction ->
        listOf(
            transaction.id,
            isoFormat.format(Date(transaction.date)),
            transaction.amount.toString(),
            escapeCsvField(transaction.description),
            escapeCsvField(transaction.category)
        ).joinToString(",")
    }
    return (listOf(CSV_HEADER) + lines).joinToString("\n")
}

private fun escapeCsvField(value: String): String {
    val needsQuoting = value.contains(',') || value.contains('"') ||
        value.contains('\n') || value.contains('\r')
    return if (needsQuoting) {
        "\"" + value.replace("\"", "\"\"") + "\""
    } else {
        value
    }
}
