package com.example.startapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.startapp.data.CounterDataRepository
import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.ChartPoint
import com.example.startapp.domain.model.ChartStats
import com.example.startapp.domain.model.TimeFrame
import com.example.startapp.utils.calculateStdDev
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.launch
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

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
                _timeFrame
            ) { snapshots, transactions, daysToDisplay, selectedTimeFrame ->
                calculateChartData(snapshots, transactions, daysToDisplay, selectedTimeFrame)
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
        timeFrame: TimeFrame
    ): ChartStats {
        // 1. Filter by daysToDisplay (from NOW backwards)
        // User wants "history in the main page shall be a perido coherent with the number of days in the chart sheet"
        // But also "never loose the DAY-series".
        // Here we are calculating for display.
        val daysAgo = System.currentTimeMillis() - (daysToDisplay.toLong() * 24 * 60 * 60 * 1000)
        val relevantSnapshots = snapshots.filter { it.date >= daysAgo }.sortedBy { it.date }

        if (relevantSnapshots.isEmpty()) {
            return ChartStats(emptyList(), 0.0, 0.0, 0.0, 0.0)
        }

        // 2. Aggregate Snapshots based on TimeFrame
        val aggregatedSnapshots = if (timeFrame == TimeFrame.Day) {
            relevantSnapshots
        } else {
            // Group by period and take the LAST snapshot of that period
            val grouped = relevantSnapshots.groupBy { getPeriodKey(it.date, timeFrame) }
            grouped.map { (_, list) -> 
                list.maxByOrNull { it.date }!! 
            }.sortedBy { it.date }
        }

        // 3. Calculate Points (Delta between snapshots + Transactions in between)
        val points = mutableListOf<ChartPoint>()
        var totalExp = 0.0
        var totalInc = 0.0
        val surplusValues = aggregatedSnapshots.map { it.amount }

        if (aggregatedSnapshots.size >= 2) {
            for (i in 1 until aggregatedSnapshots.size) {
                val prev = aggregatedSnapshots[i - 1]
                val curr = aggregatedSnapshots[i]

                // Transactions between prev and curr
                val periodTxns = transactions.filter { it.date > prev.date && it.date <= curr.date }
                val exp = periodTxns.filter { it.amount < 0 }.sumOf { -it.amount }
                
                // Income = Delta Surplus + Expenses
                // Surplus_curr = Surplus_prev + Income - Expenses
                // Income = Surplus_curr - Surplus_prev + Expenses
                val delta = curr.amount - prev.amount
                val inc = delta + exp

                points.add(ChartPoint(curr.date, curr.amount, exp, inc))
                totalExp += exp
                totalInc += inc
            }
        } else if (aggregatedSnapshots.isNotEmpty()) {
            // Single point
            points.add(ChartPoint(aggregatedSnapshots[0].date, aggregatedSnapshots[0].amount, 0.0, 0.0))
        }

        return ChartStats(
            points = points,
            totalExpenses = totalExp,
            totalIncome = totalInc,
            avgSurplus = if (surplusValues.isNotEmpty()) surplusValues.average() else 0.0,
            surplusStdDev = calculateStdDev(surplusValues)
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
