package com.example.startapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.startapp.data.CounterDataRepository
import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.CategorySlice
import com.example.startapp.domain.model.ChartPoint
import com.example.startapp.domain.model.ChartStats
import com.example.startapp.domain.model.RankedMetric
import com.example.startapp.domain.model.TimeFrame
import com.example.startapp.utils.calculateStdDev
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

internal data class PeriodTotals(
    val totalExpenses: Double,
    val totalIncome: Double
)

internal data class FilteredAnalyticsData(
    val snapshots: List<DailySnapshot>,
    val transactions: List<Transaction>,
    val coveredDays: Int
)

private const val AVERAGE_DAYS_PER_MONTH = 30.4375

internal fun calculatePeriodTotals(
    snapshots: List<DailySnapshot>,
    transactions: List<Transaction>
): PeriodTotals {
    val sortedSnapshots = snapshots.sortedBy { it.date }
    val totalExpenses = transactions
        .filter { it.amount < 0 }
        .sumOf { -it.amount }

    val totalIncome = if (sortedSnapshots.size >= 2) {
        val delta = sortedSnapshots.last().amount - sortedSnapshots.first().amount
        delta + totalExpenses
    } else {
        totalExpenses
    }

    return PeriodTotals(
        totalExpenses = totalExpenses,
        totalIncome = totalIncome
    )
}

internal fun calculateSavingRatioToDate(
    snapshots: List<DailySnapshot>,
    transactions: List<Transaction>
): Double {
    val sortedSnapshots = snapshots.sortedBy { it.date }
    if (sortedSnapshots.isEmpty()) {
        return 0.0
    }

    val totalExpenses = transactions
        .filter { it.amount < 0 }
        .sumOf { -it.amount }

    val netSurplusRaw = if (sortedSnapshots.size >= 2) {
        sortedSnapshots.last().amount - sortedSnapshots.first().amount
    } else {
        sortedSnapshots.last().amount
    }

    val totalIncome = totalExpenses + netSurplusRaw
    val saved = netSurplusRaw.coerceAtLeast(0.0)
    return if (totalIncome > 0.0) saved / totalIncome else 0.0
}

internal fun calculateMonthlyAverage(total: Double, coveredDays: Int): Double {
    return total / maxOf(coveredDays, 1) * AVERAGE_DAYS_PER_MONTH
}

internal fun filterAnalyticsData(
    snapshots: List<DailySnapshot>,
    transactions: List<Transaction>,
    daysToDisplay: Int,
    now: Long = System.currentTimeMillis()
): FilteredAnalyticsData {
    val coveredDays = maxOf(daysToDisplay, 1)
    val minTimestamp = now - (coveredDays.toLong() * 24 * 60 * 60 * 1000)
    return FilteredAnalyticsData(
        snapshots = snapshots.filter { it.date >= minTimestamp }.sortedBy { it.date },
        transactions = transactions.filter { it.date >= minTimestamp },
        coveredDays = coveredDays
    )
}

internal fun buildExpenseCategorySlices(
    transactions: List<Transaction>,
    coveredDays: Int
): List<CategorySlice> {
    val totals = transactions
        .filter { it.amount < 0 }
        .groupBy { it.category.ifBlank { "Other" } }
        .mapValues { (_, items) -> items.sumOf { -it.amount } }

    val totalExpenses = totals.values.sum()
    return totals.entries
        .sortedByDescending { it.value }
        .map { (category, total) ->
            CategorySlice(
                category = category,
                total = total,
                percentage = if (totalExpenses > 0.0) total / totalExpenses else 0.0,
                monthlyAverage = calculateMonthlyAverage(total, coveredDays)
            )
        }
}

internal fun buildIncomeCategorySlices(
    transactions: List<Transaction>,
    coveredDays: Int,
    dailyIncrease: Double
): List<CategorySlice> {
    val totals = transactions
        .filter { it.amount >= 0 }
        .groupBy { it.category.ifBlank { "Other Income" } }
        .mapValues { (_, items) -> items.sumOf { it.amount } }
        .toMutableMap()

    val dailySurplusTotal = (dailyIncrease * coveredDays).takeIf { it > 0.0 } ?: 0.0
    if (dailySurplusTotal > 0.0) {
        totals["Daily Surplus"] = (totals["Daily Surplus"] ?: 0.0) + dailySurplusTotal
    }

    val totalIncome = totals.values.sum()
    return totals.entries
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

private fun normalizeMetricLabel(raw: String): String {
    val normalized = raw
        .trim()
        .lowercase()
        .replace("\\s+".toRegex(), " ")
    return normalized.ifBlank { "(no description)" }
}

internal fun buildTopExpenseDescriptions(transactions: List<Transaction>): List<RankedMetric> {
    val totals = transactions
        .filter { it.amount < 0 }
        .groupBy { normalizeMetricLabel(it.description) }
        .mapValues { (_, items) -> items.sumOf { -it.amount } }

    return totals.entries
        .sortedByDescending { it.value }
        .take(5)
        .map { (label, total) -> RankedMetric(label = label, total = total) }
}

internal fun buildTopExpenseDays(transactions: List<Transaction>): List<RankedMetric> {
    val format = SimpleDateFormat("yyyy-MM-dd", Locale.getDefault())
    val totals = transactions
        .filter { it.amount < 0 }
        .groupBy { format.format(Date(it.date)) }
        .mapValues { (_, items) -> items.sumOf { -it.amount } }

    return totals.entries
        .sortedByDescending { it.value }
        .take(5)
        .map { (label, total) -> RankedMetric(label = label, total = total) }
}

internal fun calculateChartStatsForPeriod(
    snapshots: List<DailySnapshot>,
    transactions: List<Transaction>,
    daysToDisplay: Int,
    timeFrame: TimeFrame,
    dailyIncrease: Double,
    now: Long = System.currentTimeMillis()
): ChartStats {
    val filtered = filterAnalyticsData(
        snapshots = snapshots,
        transactions = transactions,
        daysToDisplay = daysToDisplay,
        now = now
    )
    val relevantSnapshots = filtered.snapshots
    val relevantTransactions = filtered.transactions

    if (relevantSnapshots.isEmpty()) {
        return ChartStats(
            points = emptyList(),
            totalExpenses = 0.0,
            totalIncome = 0.0,
            avgSurplus = 0.0,
            surplusStdDev = 0.0,
            savingsRatio = 0.0,
            categoryExpenses = emptyList(),
            categoryIncome = emptyList(),
            topExpenseDescriptions = emptyList(),
            topExpenseDays = emptyList()
        )
    }

    val aggregatedSnapshots = if (timeFrame == TimeFrame.Day) {
        relevantSnapshots
    } else {
        val grouped = relevantSnapshots.groupBy { getPeriodKey(it.date, timeFrame) }
        grouped.map { (_, list) ->
            list.maxByOrNull { it.date }!!
        }.sortedBy { it.date }
    }

    val points = mutableListOf<ChartPoint>()
    val surplusValues = aggregatedSnapshots.map { it.amount }

    if (aggregatedSnapshots.size >= 2) {
        for (i in 1 until aggregatedSnapshots.size) {
            val prev = aggregatedSnapshots[i - 1]
            val curr = aggregatedSnapshots[i]
            val periodTxns = relevantTransactions.filter { it.date > prev.date && it.date <= curr.date }
            val exp = periodTxns.filter { it.amount < 0 }.sumOf { -it.amount }
            val delta = curr.amount - prev.amount
            val inc = delta + exp
            points.add(ChartPoint(curr.date, curr.amount, exp, inc))
        }
    } else {
        points.add(ChartPoint(aggregatedSnapshots[0].date, aggregatedSnapshots[0].amount, 0.0, 0.0))
    }

    val periodTotals = calculatePeriodTotals(
        snapshots = relevantSnapshots,
        transactions = relevantTransactions
    )
    val categoryExpenses = buildExpenseCategorySlices(relevantTransactions, filtered.coveredDays)
    val categoryIncome = buildIncomeCategorySlices(relevantTransactions, filtered.coveredDays, dailyIncrease)
    val topExpenseDescriptions = buildTopExpenseDescriptions(relevantTransactions)
    val topExpenseDays = buildTopExpenseDays(relevantTransactions)
    val savingRatio = calculateSavingRatioToDate(
        snapshots = relevantSnapshots,
        transactions = relevantTransactions
    )

    return ChartStats(
        points = points,
        totalExpenses = periodTotals.totalExpenses,
        totalIncome = periodTotals.totalIncome,
        avgSurplus = if (surplusValues.isNotEmpty()) surplusValues.average() else 0.0,
        surplusStdDev = calculateStdDev(surplusValues),
        savingsRatio = savingRatio,
        categoryExpenses = categoryExpenses,
        categoryIncome = categoryIncome,
        topExpenseDescriptions = topExpenseDescriptions,
        topExpenseDays = topExpenseDays
    )
}

private fun getPeriodKey(date: Long, timeFrame: TimeFrame): String {
    val pattern = when (timeFrame) {
        TimeFrame.Day -> "yyyyMMdd"
        TimeFrame.Week -> "yyyyww"
        TimeFrame.Month -> "yyyyMM"
        TimeFrame.Year -> "yyyy"
    }
    return SimpleDateFormat(pattern, Locale.getDefault()).format(Date(date))
}

class ChartViewModel(private val repository: CounterDataRepository) : ViewModel() {

    private val _timeFrame = MutableStateFlow(TimeFrame.Day)
    val timeFrame = _timeFrame.asStateFlow()

    private val _chartStats = MutableStateFlow<ChartStats?>(null)
    val chartStats = _chartStats.asStateFlow()

    // We still need to know daysToDisplay to filter the initial dataset, 
    // or we can show all available history up to max 900 days.
    // The user said "never forget that ponit sup to teh max periodo box number expressed by user".
    // ChartScreen currently uses 'daysToDisplay'. Let's keep observing it.
    
    init {
        viewModelScope.launch {
            combine(
                repository.dailySnapshots,
                repository.transactions,
                repository.daysToDisplay,
                repository.dailyIncrease,
                _timeFrame
            ) { snapshots, transactions, daysToDisplay, dailyIncrease, selectedTimeFrame ->
                calculateChartData(snapshots, transactions, daysToDisplay, selectedTimeFrame, dailyIncrease)
            }.collect { stats ->
                _chartStats.value = stats
            }
        }
    }

    fun setTimeFrame(frame: TimeFrame) {
        _timeFrame.value = frame
    }

    private fun calculateChartData(
        snapshots: List<DailySnapshot>,
        transactions: List<Transaction>,
        daysToDisplay: Int,
        timeFrame: TimeFrame,
        dailyIncrease: Double
    ): ChartStats {
        // All analytics shown on the chart screen must derive from the same filtered period.
        return calculateChartStatsForPeriod(
            snapshots = snapshots,
            transactions = transactions,
            daysToDisplay = daysToDisplay,
            timeFrame = timeFrame,
            dailyIncrease = dailyIncrease
        )
    }
}

class ChartViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(ChartViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return ChartViewModel(CounterDataRepository(application)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
