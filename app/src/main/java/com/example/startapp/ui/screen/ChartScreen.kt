package com.example.startapp.ui.screen

import android.graphics.Color
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import com.example.startapp.domain.model.TimeFrame
import com.example.startapp.ui.viewmodel.ChartViewModel
import com.github.mikephil.charting.charts.LineChart
import com.github.mikephil.charting.components.YAxis
import com.github.mikephil.charting.data.Entry
import com.github.mikephil.charting.data.LineData
import com.github.mikephil.charting.data.LineDataSet
import com.github.mikephil.charting.formatter.ValueFormatter
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale
import kotlin.math.max
import kotlin.math.min

@Composable
fun ChartScreen(viewModel: ChartViewModel) {
    val chartStats by viewModel.chartStats.collectAsState()
    val timeFrame by viewModel.timeFrame.collectAsState()

    Column(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp)
            .verticalScroll(rememberScrollState())
    ) {
        Text(
            text = "Performance Chart",
            style = MaterialTheme.typography.titleMedium,
            modifier = Modifier.padding(bottom = 16.dp)
        )

        // TimeFrame Selector
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(bottom = 16.dp),
            horizontalArrangement = Arrangement.SpaceEvenly
        ) {
            TimeFrame.values().forEach { frame ->
                TimeFrameChip(
                    text = frame.label,
                    isSelected = timeFrame == frame,
                    onClick = { viewModel.setTimeFrame(frame) }
                )
            }
        }

        val stats = chartStats
        
        if (stats != null) {
            val ratio = if (stats.totalIncome != 0.0) (1 - (stats.totalExpenses / stats.totalIncome)) else 0.0

            Text(
                text = "Avg Saving Surplus Ratio: %.2f%%".format(ratio * 100),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Total Expenses: %.2f €".format(stats.totalExpenses),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Total Income: %.2f €".format(stats.totalIncome),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Average Surplus: %.2f €".format(stats.avgSurplus),
                modifier = Modifier.padding(vertical = 4.dp)
            )
            Text(
                text = "Surplus Std Dev: %.2f".format(stats.surplusStdDev),
                modifier = Modifier.padding(vertical = 8.dp)
            )

            AndroidView(
                factory = { context ->
                    LineChart(context).apply {
                        setNoDataText("No data available")
                        description.isEnabled = false

                        // X-axis styling
                        xAxis.textColor = Color.YELLOW
                        xAxis.gridColor = Color.YELLOW
                        xAxis.valueFormatter = object : ValueFormatter() {
                            private val format = SimpleDateFormat("dd/MM", Locale.getDefault())
                            override fun getFormattedValue(value: Float): String {
                                return format.format(Date(value.toLong()))
                            }
                        }

                        // Left Y-axis styling (Surplus)
                        axisLeft.textColor = Color.YELLOW
                        axisLeft.gridColor = Color.YELLOW

                        // Right Y-axis styling (Income/Expense)
                        axisRight.isEnabled = true
                        axisRight.textColor = Color.WHITE
                        axisRight.gridColor = Color.WHITE
                        axisRight.setDrawGridLines(false) 
                        axisRight.axisMinimum = 0f 

                        // Legend styling
                        legend.textColor = Color.YELLOW
                    }
                },
                update = { chart ->
                    if (stats.points.isNotEmpty()) {
                        val surplusEntries = stats.points.map { Entry(it.date.toFloat(), it.surplus.toFloat()) }
                        val expenseEntries = stats.points.map { Entry(it.date.toFloat(), it.expense.toFloat()) }
                        val incomeEntries = stats.points.map { Entry(it.date.toFloat(), it.income.toFloat()) }
                        
                        // Calculate range for Left Axis (Surplus)
                        val minSurplus = stats.points.minOf { it.surplus }.toFloat()
                        val maxSurplus = stats.points.maxOf { it.surplus }.toFloat()
                        
                        // Ensure 0 is included
                        val axisMin = min(0f, minSurplus)
                        val axisMax = max(0f, maxSurplus)
                        
                        // Add some padding (10%)
                        val range = axisMax - axisMin
                        val padding = if (range == 0f) 10f else range * 0.1f
                        
                        chart.axisLeft.axisMinimum = axisMin - padding
                        chart.axisLeft.axisMaximum = axisMax + padding
                        
                        val surplusSet = LineDataSet(surplusEntries, "Surplus").apply {
                            color = Color.YELLOW
                            valueTextColor = Color.YELLOW
                            setCircleColor(Color.YELLOW)
                            lineWidth = 2f
                            axisDependency = YAxis.AxisDependency.LEFT
                            setDrawValues(false)
                        }

                        val expenseSet = LineDataSet(expenseEntries, "Expenses").apply {
                            color = Color.RED
                            valueTextColor = Color.RED
                            setCircleColor(Color.RED)
                            lineWidth = 2f
                            axisDependency = YAxis.AxisDependency.RIGHT
                            setDrawValues(false)
                        }

                        val incomeSet = LineDataSet(incomeEntries, "Income").apply {
                            color = Color.GREEN
                            valueTextColor = Color.GREEN
                            setCircleColor(Color.GREEN)
                            lineWidth = 2f
                            axisDependency = YAxis.AxisDependency.RIGHT
                            setDrawValues(false)
                        }

                        chart.data = LineData(surplusSet, expenseSet, incomeSet)
                        chart.invalidate()
                    } else {
                        chart.clear()
                        chart.invalidate()
                    }
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
            )
        }
    }
}

@Composable
fun TimeFrameChip(
    text: String,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .clip(RoundedCornerShape(16.dp))
            .background(if (isSelected) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.surfaceVariant)
            .clickable(onClick = onClick)
            .padding(horizontal = 12.dp, vertical = 8.dp),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = text,
            color = if (isSelected) MaterialTheme.colorScheme.onPrimary else MaterialTheme.colorScheme.onSurfaceVariant,
            style = MaterialTheme.typography.bodyMedium
        )
    }
}
