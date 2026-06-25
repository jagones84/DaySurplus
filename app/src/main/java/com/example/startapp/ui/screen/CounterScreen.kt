package com.example.startapp.ui.screen

import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.ExposedDropdownMenuBox
import androidx.compose.material3.ExposedDropdownMenuDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.ExpenseCategory
import com.example.startapp.domain.model.ExpenseGroup
import com.example.startapp.domain.model.buildGroupedTransactionState
import com.example.startapp.ui.viewmodel.CounterViewModel
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(viewModel: CounterViewModel) {

    val totalAmount by viewModel.totalAmount.collectAsState()
    val dailyIncrease by viewModel.dailyIncrease.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val daysToDisplay by viewModel.daysToDisplay.collectAsState()

    var manualAmount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dailyIncreaseAmount by remember { mutableStateOf(dailyIncrease.toString()) }
    var showResetDialog by remember { mutableStateOf(false) }
    var daysInput by remember { mutableStateOf(daysToDisplay.toString()) }
    var selectedExpenseCategory by remember { mutableStateOf(ExpenseCategory.FOOD.label) }
    var categoryMenuExpanded by remember { mutableStateOf(false) }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }

    LaunchedEffect(daysToDisplay) {
        daysInput = daysToDisplay.toString()
    }

    LaunchedEffect(dailyIncrease) {
        dailyIncreaseAmount = if (dailyIncrease > 0) dailyIncrease.toString() else ""
    }

    val filteredTransactions = remember(transactions, daysToDisplay) {
        val daysAgo = System.currentTimeMillis() - (daysToDisplay.toLong() * 24 * 60 * 60 * 1000)
        transactions.filter { it.date >= daysAgo }.reversed()
    }
    val groupedFilteredTransactions = remember(filteredTransactions) {
        buildGroupedTransactionState(filteredTransactions)
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text("Reset Data") },
            text = { Text("Are you sure you want to reset all data? This action cannot be undone.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reset()
                        showResetDialog = false
                    }
                ) {
                    Text("Reset")
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text("Cancel")
                }
            }
        )
    }

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    Text(
                        text = "Total Surplus",
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = String.format("%.2f €", totalAmount),
                        style = MaterialTheme.typography.headlineLarge,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = manualAmount,
                        onValueChange = { manualAmount = it },
                        label = { Text("Add or Subtract Amount") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text("Description") },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = categoryMenuExpanded,
                        onExpandedChange = { categoryMenuExpanded = !categoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedExpenseCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text("Expense Category") },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = categoryMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = categoryMenuExpanded,
                            onDismissRequest = { categoryMenuExpanded = false }
                        ) {
                            ExpenseCategory.expenseCategories.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category.label) },
                                    onClick = {
                                        selectedExpenseCategory = category.label
                                        categoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(16.dp))

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceEvenly
                    ) {
                        Button(onClick = {
                            viewModel.addAmount(
                                amount = manualAmount.toDoubleOrNull() ?: 0.0,
                                description = description,
                                category = ExpenseCategory.INCOME.label
                            )
                            manualAmount = ""
                            description = ""
                        }) {
                            Text("Add")
                        }
                        Button(onClick = {
                            viewModel.subtractAmount(
                                amount = manualAmount.toDoubleOrNull() ?: 0.0,
                                description = description,
                                category = selectedExpenseCategory
                            )
                            manualAmount = ""
                            description = ""
                        }) {
                            Text("Subtract")
                        }
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
            ) {
                Column(modifier = Modifier.padding(16.dp)) {
                    OutlinedTextField(
                        value = dailyIncreaseAmount,
                        onValueChange = {
                            dailyIncreaseAmount = it
                            viewModel.updateDailyIncrease(it.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text("Day Surplus") },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = "(Monthly Incomes - Monthly Expenses) * 12 / 365",
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Text(
                    text = "History",
                    style = MaterialTheme.typography.titleMedium,
                    textAlign = TextAlign.Start
                )

                OutlinedTextField(
                    value = daysInput,
                    onValueChange = {
                        daysInput = it
                        val days = it.toIntOrNull()
                        if (days != null) {
                            viewModel.updateDaysToDisplay(days)
                        }
                    },
                    label = { Text("Days") },
                    keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                    modifier = Modifier.width(100.dp)
                )
            }
        }

        if (groupedFilteredTransactions.incomeTransactions.isNotEmpty()) {
            item {
                Text(
                    text = "Income",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }

            items(items = groupedFilteredTransactions.incomeTransactions, key = { it.id }) { transaction ->
                TransactionRow(
                    transaction = transaction,
                    dateFormat = dateFormat,
                    onDelete = viewModel::deleteTransaction
                )
            }
        }

        if (groupedFilteredTransactions.expenseGroups.isNotEmpty()) {
            item {
                Text(
                    text = "Expenses By Category",
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(items = groupedFilteredTransactions.expenseGroups, key = { it.category }) { group ->
            val isExpanded = expandedCategories[group.category] ?: true
            CategoryGroupCard(
                group = group,
                isExpanded = isExpanded,
                onToggle = {
                    expandedCategories[group.category] = !isExpanded
                },
                dateFormat = dateFormat,
                onDelete = viewModel::deleteTransaction
            )
        }

        if (filteredTransactions.isEmpty()) {
            item {
                Text(
                    text = "No transactions in the selected period.",
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        item {
            Spacer(modifier = Modifier.height(16.dp))
            Button(
                onClick = { showResetDialog = true },
                modifier = Modifier.fillMaxWidth()
            ) {
                Text("Reset All Data")
            }
            Spacer(modifier = Modifier.height(16.dp))
            LegalNotice()
        }
    }
}

@Composable
private fun CategoryGroupCard(
    group: ExpenseGroup,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    dateFormat: SimpleDateFormat,
    onDelete: (Transaction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .animateContentSize(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .clickable(onClick = onToggle),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = group.category,
                        style = MaterialTheme.typography.titleSmall
                    )
                    Text(
                        text = "${group.transactions.size} expenses - ${String.format("%.2f €", group.total)}",
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = if (isExpanded) "[-] Collapse" else "[+] Expand",
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.primary
                )
            }

            if (isExpanded) {
                Spacer(modifier = Modifier.height(8.dp))
                Column(modifier = Modifier.padding(start = 12.dp)) {
                    group.transactions.forEach { transaction ->
                        TransactionRow(
                            transaction = transaction,
                            dateFormat = dateFormat,
                            onDelete = onDelete
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun TransactionRow(
    transaction: Transaction,
    dateFormat: SimpleDateFormat,
    onDelete: (Transaction) -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(vertical = 4.dp),
        elevation = CardDefaults.cardElevation(defaultElevation = 2.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically,
            horizontalArrangement = Arrangement.SpaceBetween
        ) {
            Column(modifier = Modifier.weight(1f)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween
                ) {
                    Text(
                        text = dateFormat.format(Date(transaction.date)),
                        style = MaterialTheme.typography.bodySmall
                    )
                    Text(
                        text = String.format("%.2f €", transaction.amount),
                        style = MaterialTheme.typography.bodyMedium,
                        color = if (transaction.amount >= 0) {
                            MaterialTheme.colorScheme.primary
                        } else {
                            MaterialTheme.colorScheme.error
                        },
                        modifier = Modifier.padding(end = 8.dp)
                    )
                }
                if (transaction.description.isNotEmpty()) {
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = transaction.description,
                        style = MaterialTheme.typography.bodyMedium
                    )
                }
                if (transaction.amount >= 0 && transaction.category.isNotBlank()) {
                    Spacer(modifier = Modifier.height(2.dp))
                    Text(
                        text = transaction.category,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            }
            IconButton(onClick = { onDelete(transaction) }) {
                Icon(
                    imageVector = Icons.Default.Delete,
                    contentDescription = "Delete transaction"
                )
            }
        }
    }
}

@Composable
fun LegalNotice() {
    Column(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Text(
            text = "Copyright © 2025 Giovanni Mauceri. All Rights Reserved.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = "Unauthorized copying, modification, distribution, or public performance of this software is strictly prohibited.",
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
