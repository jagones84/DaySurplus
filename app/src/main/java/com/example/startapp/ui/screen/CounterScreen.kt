package com.example.startapp.ui.screen

import android.app.DatePickerDialog
import android.content.Context
import android.net.Uri
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.animation.animateContentSize
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Delete
import androidx.compose.material.icons.filled.Edit
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
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.mutableStateMapOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.example.startapp.R
import com.example.startapp.data.backup.ImportMode
import com.example.startapp.domain.filterTransactionsByDateRange
import com.example.startapp.domain.matchingAnalysisWindowPreset
import com.example.startapp.domain.model.AnalysisWindowPreset
import com.example.startapp.domain.model.CategoryCatalog
import com.example.startapp.domain.model.CategoryType
import com.example.startapp.domain.model.ExpenseCategory
import com.example.startapp.domain.model.Transaction
import com.example.startapp.domain.model.TransactionGroup
import com.example.startapp.domain.model.buildGroupedTransactionState
import com.example.startapp.ui.viewmodel.CounterViewModel
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import kotlin.math.abs

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CounterScreen(viewModel: CounterViewModel) {

    val context = LocalContext.current
    val coroutineScope = rememberCoroutineScope()

    val totalAmount by viewModel.totalAmount.collectAsState()
    val dailyIncrease by viewModel.dailyIncrease.collectAsState()
    val transactions by viewModel.transactions.collectAsState()
    val dateRangeFilter by viewModel.dateRangeFilter.collectAsState()
    val expenseCustomCategories by viewModel.expenseCustomCategories.collectAsState()
    val incomeCustomCategories by viewModel.incomeCustomCategories.collectAsState()

    var manualAmount by remember { mutableStateOf("") }
    var description by remember { mutableStateOf("") }
    var dailyIncreaseAmount by remember { mutableStateOf("") }
    var showResetDialog by remember { mutableStateOf(false) }
    var selectedIncomeCategory by remember { mutableStateOf("Salary") }
    var selectedExpenseCategory by remember { mutableStateOf(ExpenseCategory.FOOD.label) }
    var incomeCategoryMenuExpanded by remember { mutableStateOf(false) }
    var expenseCategoryMenuExpanded by remember { mutableStateOf(false) }
    var showNewCategoryDialog by remember { mutableStateOf(false) }
    var pendingCategoryType by remember { mutableStateOf<CategoryType?>(null) }
    var newCategoryName by remember { mutableStateOf("") }
    var editingTransaction by remember { mutableStateOf<Transaction?>(null) }
    var editAmount by remember { mutableStateOf("") }
    var editDescription by remember { mutableStateOf("") }
    var editSelectedCategory by remember { mutableStateOf("") }
    var editCategoryMenuExpanded by remember { mutableStateOf(false) }
    var historySearchQuery by remember { mutableStateOf("") }
    var historyScope by remember { mutableStateOf("All") }
    var historyScopeMenuExpanded by remember { mutableStateOf(false) }
    var historyCategoryFilter by remember { mutableStateOf("All") }
    var historyCategoryMenuExpanded by remember { mutableStateOf(false) }
    var backupResultMessage by remember { mutableStateOf<String?>(null) }
    var pendingImportUri by remember { mutableStateOf<Uri?>(null) }
    var renamingCategoryType by remember { mutableStateOf<CategoryType?>(null) }
    var renamingCategoryOldName by remember { mutableStateOf("") }
    var renamingCategoryNewName by remember { mutableStateOf("") }
    var deletingCategoryType by remember { mutableStateOf<CategoryType?>(null) }
    var deletingCategoryName by remember { mutableStateOf("") }

    val exportBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("application/json")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                backupResultMessage = runCatching {
                    withContext(Dispatchers.IO) {
                        val json = viewModel.exportBackupJson()
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("Cannot open output stream")
                        output.use { stream ->
                            stream.write(json.toByteArray(Charsets.UTF_8))
                            stream.flush()
                        }
                    }
                    context.getString(R.string.backup_exported)
                }.getOrElse {
                    context.getString(R.string.backup_export_failed, it.message ?: "unknown")
                }
            }
        }
    }

    val exportCsvLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.CreateDocument("text/csv")
    ) { uri ->
        if (uri != null) {
            coroutineScope.launch {
                backupResultMessage = runCatching {
                    withContext(Dispatchers.IO) {
                        val csv = viewModel.exportCsv()
                        val output = context.contentResolver.openOutputStream(uri)
                            ?: error("Cannot open output stream")
                        output.use { stream ->
                            stream.write(csv.toByteArray(Charsets.UTF_8))
                            stream.flush()
                        }
                    }
                    context.getString(R.string.backup_exported)
                }.getOrElse {
                    context.getString(R.string.backup_export_failed, it.message ?: "unknown")
                }
            }
        }
    }

    val importBackupLauncher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.OpenDocument()
    ) { uri ->
        if (uri != null) {
            pendingImportUri = uri
        }
    }

    fun performImport(uri: Uri, mode: ImportMode) {
        coroutineScope.launch {
            backupResultMessage = runCatching {
                val json = withContext(Dispatchers.IO) {
                    context.contentResolver.openInputStream(uri)
                        ?.bufferedReader()
                        ?.use { it.readText() }
                        .orEmpty()
                }
                val error = viewModel.importBackupJson(json, mode)
                if (error == null) {
                    context.getString(R.string.backup_imported)
                } else {
                    context.getString(R.string.backup_import_failed, error)
                }
            }.getOrElse {
                context.getString(R.string.backup_import_failed, it.message ?: "unknown")
            }
        }
    }

    val dateFormat = remember { SimpleDateFormat("dd/MM/yyyy HH:mm", Locale.getDefault()) }
    val filterDateFormat = remember { SimpleDateFormat("dd/MM/yyyy", Locale.getDefault()) }
    val expandedCategories = remember { mutableStateMapOf<String, Boolean>() }
    val selectedAnalysisWindow = remember(dateRangeFilter) {
        matchingAnalysisWindowPreset(dateRangeFilter)
    }

    LaunchedEffect(Unit) {
        val initial = viewModel.dailyIncrease.first()
        if (initial > 0) {
            dailyIncreaseAmount = initial.toString()
        }
    }

    val parsedManualAmount = manualAmount.toDoubleOrNull()?.takeIf { it > 0.0 }
    val parsedEditAmount = editAmount.toDoubleOrNull()?.takeIf { it > 0.0 }

    val periodTransactions = remember(transactions, dateRangeFilter) {
        filterTransactionsByDateRange(transactions, dateRangeFilter).reversed()
    }
    val historyCategoryOptions = remember(
        historyScope,
        expenseCustomCategories,
        incomeCustomCategories
    ) {
        val expense = CategoryCatalog.builtInCategories(CategoryType.EXPENSE) + expenseCustomCategories
        val income = CategoryCatalog.builtInCategories(CategoryType.INCOME) + incomeCustomCategories
        val scoped = when (historyScope) {
            "Income" -> income
            "Expense" -> expense
            else -> expense + income
        }
        listOf("All") + scoped.distinct().sorted()
    }
    LaunchedEffect(historyCategoryOptions) {
        if (historyCategoryFilter !in historyCategoryOptions) {
            historyCategoryFilter = "All"
        }
    }

    val filteredTransactions = remember(
        periodTransactions,
        historySearchQuery,
        historyScope,
        historyCategoryFilter
    ) {
        val query = historySearchQuery.trim().lowercase()
        periodTransactions
            .asSequence()
            .filter { transaction ->
                when (historyScope) {
                    "Income" -> transaction.amount >= 0
                    "Expense" -> transaction.amount < 0
                    else -> true
                }
            }
            .filter { transaction ->
                if (historyCategoryFilter == "All") {
                    true
                } else {
                    transaction.category == historyCategoryFilter
                }
            }
            .filter { transaction ->
                if (query.isBlank()) {
                    true
                } else {
                    transaction.description.lowercase().contains(query)
                }
            }
            .toList()
    }

    val groupedFilteredTransactions = remember(filteredTransactions) {
        buildGroupedTransactionState(filteredTransactions)
    }

    if (showResetDialog) {
        AlertDialog(
            onDismissRequest = { showResetDialog = false },
            title = { Text(stringResource(R.string.reset_data_title)) },
            text = { Text(stringResource(R.string.reset_data_message)) },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.reset()
                        showResetDialog = false
                    }
                ) {
                    Text(stringResource(R.string.reset))
                }
            },
            dismissButton = {
                TextButton(onClick = { showResetDialog = false }) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (pendingImportUri != null) {
        AlertDialog(
            onDismissRequest = { pendingImportUri = null },
            title = { Text(stringResource(R.string.import_choose_mode_title)) },
            text = { Text(stringResource(R.string.import_choose_mode_message)) },
            confirmButton = {
                Row {
                    TextButton(
                        onClick = {
                            val uri = pendingImportUri
                            pendingImportUri = null
                            if (uri != null) performImport(uri, ImportMode.REPLACE)
                        }
                    ) {
                        Text(stringResource(R.string.import_replace_all))
                    }
                    TextButton(
                        onClick = {
                            val uri = pendingImportUri
                            pendingImportUri = null
                            if (uri != null) performImport(uri, ImportMode.MERGE)
                        }
                    ) {
                        Text(stringResource(R.string.import_merge))
                    }
                    TextButton(onClick = { pendingImportUri = null }) {
                        Text(stringResource(R.string.cancel))
                    }
                }
            }
        )
    }

    if (showNewCategoryDialog && pendingCategoryType != null) {
        AlertDialog(
            onDismissRequest = {
                showNewCategoryDialog = false
                pendingCategoryType = null
                newCategoryName = ""
            },
            title = { Text(stringResource(R.string.new_category)) },
            text = {
                OutlinedTextField(
                    value = newCategoryName,
                    onValueChange = { newCategoryName = it },
                    label = { Text(stringResource(R.string.category_name)) },
                    modifier = Modifier.fillMaxWidth()
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        val type = pendingCategoryType ?: return@TextButton
                        viewModel.addCustomCategory(type, newCategoryName) { created ->
                            when (type) {
                                CategoryType.EXPENSE -> selectedExpenseCategory = created
                                CategoryType.INCOME -> selectedIncomeCategory = created
                            }
                            if (editingTransaction != null && type == pendingCategoryType) {
                                editSelectedCategory = created
                            }
                            showNewCategoryDialog = false
                            pendingCategoryType = null
                            newCategoryName = ""
                        }
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showNewCategoryDialog = false
                        pendingCategoryType = null
                        newCategoryName = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (renamingCategoryType != null) {
        val type = renamingCategoryType ?: return
        AlertDialog(
            onDismissRequest = {
                renamingCategoryType = null
                renamingCategoryOldName = ""
                renamingCategoryNewName = ""
            },
            title = { Text(stringResource(R.string.rename_category_title)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = renamingCategoryNewName,
                        onValueChange = { renamingCategoryNewName = it },
                        label = { Text(stringResource(R.string.category_name)) },
                        modifier = Modifier.fillMaxWidth()
                    )
                    Spacer(modifier = Modifier.height(4.dp))
                    Text(
                        text = stringResource(R.string.category_in_use_note),
                        style = MaterialTheme.typography.bodySmall
                    )
                }
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.renameCustomCategory(type, renamingCategoryOldName, renamingCategoryNewName)
                        renamingCategoryType = null
                        renamingCategoryOldName = ""
                        renamingCategoryNewName = ""
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        renamingCategoryType = null
                        renamingCategoryOldName = ""
                        renamingCategoryNewName = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (deletingCategoryType != null) {
        val type = deletingCategoryType ?: return
        AlertDialog(
            onDismissRequest = {
                deletingCategoryType = null
                deletingCategoryName = ""
            },
            title = { Text(stringResource(R.string.delete_category)) },
            text = { Text("${stringResource(R.string.category_in_use_note)} (${deletingCategoryName})") },
            confirmButton = {
                TextButton(
                    onClick = {
                        viewModel.deleteCustomCategory(type, deletingCategoryName)
                        deletingCategoryType = null
                        deletingCategoryName = ""
                    }
                ) {
                    Text(stringResource(R.string.delete_category))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        deletingCategoryType = null
                        deletingCategoryName = ""
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (editingTransaction != null) {
        val transaction = editingTransaction!!
        val isIncome = transaction.amount >= 0
        val type = if (isIncome) CategoryType.INCOME else CategoryType.EXPENSE
        val custom = if (isIncome) incomeCustomCategories else expenseCustomCategories

        AlertDialog(
            onDismissRequest = {
                editingTransaction = null
                editAmount = ""
                editDescription = ""
                editSelectedCategory = ""
                editCategoryMenuExpanded = false
            },
            title = { Text(stringResource(R.string.edit_transaction)) },
            text = {
                Column {
                    OutlinedTextField(
                        value = editAmount,
                        onValueChange = { editAmount = it },
                        label = { Text(stringResource(R.string.amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (parsedEditAmount == null) {
                        Text(
                            text = stringResource(R.string.invalid_amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = editDescription,
                        onValueChange = { editDescription = it },
                        label = { Text(stringResource(R.string.description_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = editCategoryMenuExpanded,
                        onExpandedChange = { editCategoryMenuExpanded = !editCategoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = editSelectedCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(if (isIncome) stringResource(R.string.income_category) else stringResource(R.string.expense_category)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = editCategoryMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = editCategoryMenuExpanded,
                            onDismissRequest = { editCategoryMenuExpanded = false }
                        ) {
                            CategoryCatalog.dropdownOptions(type, custom).forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (category == CategoryCatalog.NEW_CATEGORY_OPTION) {
                                                stringResource(R.string.new_category)
                                            } else {
                                                category
                                            }
                                        )
                                    },
                                    onClick = {
                                        if (category == CategoryCatalog.NEW_CATEGORY_OPTION) {
                                            pendingCategoryType = type
                                            showNewCategoryDialog = true
                                        } else {
                                            editSelectedCategory = category
                                        }
                                        editCategoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            },
            confirmButton = {
                TextButton(
                    enabled = parsedEditAmount != null,
                    onClick = {
                        val magnitude = parsedEditAmount ?: return@TextButton
                        val newAmount = if (isIncome) magnitude else -magnitude
                        viewModel.updateTransaction(
                            id = transaction.id,
                            newAmount = newAmount,
                            newDescription = editDescription,
                            newCategory = editSelectedCategory
                        )

                        editingTransaction = null
                        editAmount = ""
                        editDescription = ""
                        editSelectedCategory = ""
                        editCategoryMenuExpanded = false
                    }
                ) {
                    Text(stringResource(R.string.save))
                }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        editingTransaction = null
                        editAmount = ""
                        editDescription = ""
                        editSelectedCategory = ""
                        editCategoryMenuExpanded = false
                    }
                ) {
                    Text(stringResource(R.string.cancel))
                }
            }
        )
    }

    if (backupResultMessage != null) {
        AlertDialog(
            onDismissRequest = { backupResultMessage = null },
            title = { Text(stringResource(R.string.backup_title)) },
            text = { Text(backupResultMessage!!) },
            confirmButton = {
                TextButton(onClick = { backupResultMessage = null }) {
                    Text("OK")
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
                        text = stringResource(R.string.total_surplus),
                        style = MaterialTheme.typography.titleMedium,
                        modifier = Modifier.fillMaxWidth(),
                        textAlign = TextAlign.Center
                    )
                    Text(
                        text = stringResource(R.string.amount_euro, totalAmount),
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
                        label = { Text(stringResource(R.string.add_or_subtract_amount)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )
                    if (manualAmount.isNotBlank() && parsedManualAmount == null) {
                        Text(
                            text = stringResource(R.string.invalid_amount),
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.error
                        )
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    OutlinedTextField(
                        value = description,
                        onValueChange = { description = it },
                        label = { Text(stringResource(R.string.description_label)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = incomeCategoryMenuExpanded,
                        onExpandedChange = { incomeCategoryMenuExpanded = !incomeCategoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedIncomeCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.income_category)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = incomeCategoryMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = incomeCategoryMenuExpanded,
                            onDismissRequest = { incomeCategoryMenuExpanded = false }
                        ) {
                            CategoryCatalog.dropdownOptions(CategoryType.INCOME, incomeCustomCategories).forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (category == CategoryCatalog.NEW_CATEGORY_OPTION) {
                                                stringResource(R.string.new_category)
                                            } else {
                                                category
                                            }
                                        )
                                    },
                                    onClick = {
                                        if (category == CategoryCatalog.NEW_CATEGORY_OPTION) {
                                            pendingCategoryType = CategoryType.INCOME
                                            showNewCategoryDialog = true
                                        } else {
                                            selectedIncomeCategory = category
                                        }
                                        incomeCategoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = expenseCategoryMenuExpanded,
                        onExpandedChange = { expenseCategoryMenuExpanded = !expenseCategoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = selectedExpenseCategory,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.expense_category)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = expenseCategoryMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = expenseCategoryMenuExpanded,
                            onDismissRequest = { expenseCategoryMenuExpanded = false }
                        ) {
                            CategoryCatalog.dropdownOptions(CategoryType.EXPENSE, expenseCustomCategories).forEach { category ->
                                DropdownMenuItem(
                                    text = {
                                        Text(
                                            if (category == CategoryCatalog.NEW_CATEGORY_OPTION) {
                                                stringResource(R.string.new_category)
                                            } else {
                                                category
                                            }
                                        )
                                    },
                                    onClick = {
                                        if (category == CategoryCatalog.NEW_CATEGORY_OPTION) {
                                            pendingCategoryType = CategoryType.EXPENSE
                                            showNewCategoryDialog = true
                                        } else {
                                            selectedExpenseCategory = category
                                        }
                                        expenseCategoryMenuExpanded = false
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
                        Button(
                            enabled = parsedManualAmount != null,
                            onClick = {
                                val amount = parsedManualAmount ?: return@Button
                                viewModel.addAmount(
                                    amount = amount,
                                    description = description,
                                    category = selectedIncomeCategory
                                )
                                manualAmount = ""
                                description = ""
                            }
                        ) {
                            Text(stringResource(R.string.add))
                        }
                        Button(
                            enabled = parsedManualAmount != null,
                            onClick = {
                                val amount = parsedManualAmount ?: return@Button
                                viewModel.subtractAmount(
                                    amount = amount,
                                    description = description,
                                    category = selectedExpenseCategory
                                )
                                manualAmount = ""
                                description = ""
                            }
                        ) {
                            Text(stringResource(R.string.subtract))
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
                        onValueChange = { newValue ->
                            dailyIncreaseAmount = newValue
                            viewModel.updateDailyIncrease(newValue.toDoubleOrNull() ?: 0.0)
                        },
                        label = { Text(stringResource(R.string.day_surplus)) },
                        keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Text(
                        text = stringResource(R.string.day_surplus_hint),
                        style = MaterialTheme.typography.bodySmall,
                        modifier = Modifier.padding(start = 16.dp, top = 4.dp)
                    )
                }
            }
        }

        item {
            Card(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    Text(
                        text = stringResource(R.string.history),
                        style = MaterialTheme.typography.titleMedium,
                        textAlign = TextAlign.Start
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.analysis_range),
                        style = MaterialTheme.typography.titleSmall
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.timeframe_window),
                        style = MaterialTheme.typography.bodyMedium
                    )
                    Spacer(modifier = Modifier.height(8.dp))
                    val presetRows = listOf(
                        listOf(
                            AnalysisWindowPreset.DAYS_7,
                            AnalysisWindowPreset.DAYS_30,
                            AnalysisWindowPreset.DAYS_90
                        ),
                        listOf(
                            AnalysisWindowPreset.DAYS_180,
                            AnalysisWindowPreset.DAYS_365,
                            AnalysisWindowPreset.CUSTOM
                        )
                    )
                    presetRows.forEach { presetRow ->
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(bottom = 8.dp),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            presetRow.forEach { preset ->
                                Box(modifier = Modifier.weight(1f)) {
                                    TimeFrameChip(
                                        text = preset.label,
                                        isSelected = selectedAnalysisWindow == preset,
                                        onClick = {
                                            preset.days?.let { days ->
                                                viewModel.applyAnalysisWindow(days)
                                            }
                                        }
                                    )
                                }
                            }
                        }
                    }
                    Spacer(modifier = Modifier.height(4.dp))
                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                showDatePicker(
                                    context = context,
                                    initialEpochMs = dateRangeFilter.startEpochMs
                                ) { selectedDate ->
                                    viewModel.updateDateRangeFilter(
                                        startEpochMs = selectedDate,
                                        endEpochMs = dateRangeFilter.endEpochMs
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                stringResource(
                                    R.string.from_date,
                                    filterDateFormat.format(Date(dateRangeFilter.startEpochMs))
                                )
                            )
                        }
                        Button(
                            onClick = {
                                showDatePicker(
                                    context = context,
                                    initialEpochMs = dateRangeFilter.endEpochMs
                                ) { selectedDate ->
                                    viewModel.updateDateRangeFilter(
                                        startEpochMs = dateRangeFilter.startEpochMs,
                                        endEpochMs = selectedDate
                                    )
                                }
                            },
                            modifier = Modifier.weight(1f)
                        ) {
                            Text(
                                stringResource(
                                    R.string.to_date,
                                    filterDateFormat.format(Date(dateRangeFilter.endEpochMs))
                                )
                            )
                        }
                    }
                    Spacer(modifier = Modifier.height(8.dp))
                    TextButton(
                        onClick = { viewModel.resetDateRangeFilter() },
                        modifier = Modifier.align(Alignment.End)
                    ) {
                        Text(stringResource(R.string.reset_filter))
                    }
                }
            }
        }

        item {
            Card(
                modifier = Modifier.fillMaxWidth(),
                elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
            ) {
                Column(modifier = Modifier.padding(12.dp)) {
                    OutlinedTextField(
                        value = historySearchQuery,
                        onValueChange = { historySearchQuery = it },
                        label = { Text(stringResource(R.string.search)) },
                        modifier = Modifier.fillMaxWidth()
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = historyScopeMenuExpanded,
                        onExpandedChange = { historyScopeMenuExpanded = !historyScopeMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = scopeDisplayLabel(historyScope),
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.scope)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = historyScopeMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = historyScopeMenuExpanded,
                            onDismissRequest = { historyScopeMenuExpanded = false }
                        ) {
                            listOf("All", "Income", "Expense").forEach { scope ->
                                DropdownMenuItem(
                                    text = { Text(scopeDisplayLabel(scope)) },
                                    onClick = {
                                        historyScope = scope
                                        historyScopeMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }

                    Spacer(modifier = Modifier.height(8.dp))

                    ExposedDropdownMenuBox(
                        expanded = historyCategoryMenuExpanded,
                        onExpandedChange = { historyCategoryMenuExpanded = !historyCategoryMenuExpanded }
                    ) {
                        OutlinedTextField(
                            value = historyCategoryFilter,
                            onValueChange = {},
                            readOnly = true,
                            label = { Text(stringResource(R.string.category)) },
                            trailingIcon = {
                                ExposedDropdownMenuDefaults.TrailingIcon(expanded = historyCategoryMenuExpanded)
                            },
                            modifier = Modifier
                                .menuAnchor()
                                .fillMaxWidth()
                        )
                        DropdownMenu(
                            expanded = historyCategoryMenuExpanded,
                            onDismissRequest = { historyCategoryMenuExpanded = false }
                        ) {
                            historyCategoryOptions.forEach { category ->
                                DropdownMenuItem(
                                    text = { Text(category) },
                                    onClick = {
                                        historyCategoryFilter = category
                                        historyCategoryMenuExpanded = false
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        item {
            CustomCategoriesCard(
                expenseCustomCategories = expenseCustomCategories,
                incomeCustomCategories = incomeCustomCategories,
                onRename = { type, name ->
                    renamingCategoryType = type
                    renamingCategoryOldName = name
                    renamingCategoryNewName = name
                },
                onDelete = { type, name ->
                    deletingCategoryType = type
                    deletingCategoryName = name
                }
            )
        }

        item {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        exportBackupLauncher.launch("day-surp-backup.json")
                    }
                ) {
                    Text(stringResource(R.string.export_backup))
                }
            }
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(vertical = 8.dp),
                horizontalArrangement = Arrangement.SpaceEvenly
            ) {
                Button(
                    onClick = {
                        importBackupLauncher.launch(arrayOf("application/json"))
                    }
                ) {
                    Text(stringResource(R.string.import_backup))
                }
                Button(
                    onClick = {
                        exportCsvLauncher.launch("day-surp-transactions.csv")
                    }
                ) {
                    Text(stringResource(R.string.export_csv))
                }
            }
        }

        if (groupedFilteredTransactions.incomeGroups.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.income_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(items = groupedFilteredTransactions.incomeGroups, key = { "income-${it.category}" }) { group ->
            val groupKey = "income-${group.category}"
            val isExpanded = expandedCategories[groupKey] ?: true
            CategoryGroupCard(
                group = group,
                entryText = stringResource(
                    R.string.income_entries_count,
                    group.transactions.size,
                    stringResource(R.string.amount_euro, group.total)
                ),
                isExpanded = isExpanded,
                onToggle = {
                    expandedCategories[groupKey] = !isExpanded
                },
                dateFormat = dateFormat,
                onDelete = viewModel::deleteTransaction,
                onEdit = { transaction ->
                    editingTransaction = transaction
                    editAmount = abs(transaction.amount).toString()
                    editDescription = transaction.description
                    editSelectedCategory = transaction.category.ifBlank {
                        if (transaction.amount >= 0) "Other Income" else ExpenseCategory.OTHER.label
                    }
                }
            )
        }

        if (groupedFilteredTransactions.expenseGroups.isNotEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.expenses_by_category),
                    style = MaterialTheme.typography.titleMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        }

        items(items = groupedFilteredTransactions.expenseGroups, key = { it.category }) { group ->
            val groupKey = "expense-${group.category}"
            val isExpanded = expandedCategories[groupKey] ?: true
            CategoryGroupCard(
                group = group,
                entryText = stringResource(
                    R.string.expenses_count,
                    group.transactions.size,
                    stringResource(R.string.amount_euro, group.total)
                ),
                isExpanded = isExpanded,
                onToggle = {
                    expandedCategories[groupKey] = !isExpanded
                },
                dateFormat = dateFormat,
                onDelete = viewModel::deleteTransaction,
                onEdit = { transaction ->
                    editingTransaction = transaction
                    editAmount = abs(transaction.amount).toString()
                    editDescription = transaction.description
                    editSelectedCategory = transaction.category.ifBlank {
                        if (transaction.amount >= 0) "Other Income" else ExpenseCategory.OTHER.label
                    }
                }
            )
        }

        if (periodTransactions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_transactions_period),
                    style = MaterialTheme.typography.bodyMedium,
                    modifier = Modifier.fillMaxWidth()
                )
            }
        } else if (filteredTransactions.isEmpty()) {
            item {
                Text(
                    text = stringResource(R.string.no_transactions_filters),
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
                Text(stringResource(R.string.reset_all_data))
            }
            Spacer(modifier = Modifier.height(16.dp))
            LegalNotice()
        }
    }
}

@Composable
private fun scopeDisplayLabel(scope: String): String = when (scope) {
    "Income" -> stringResource(R.string.scope_income)
    "Expense" -> stringResource(R.string.scope_expense)
    else -> stringResource(R.string.scope_all)
}

@Composable
private fun CustomCategoriesCard(
    expenseCustomCategories: List<String>,
    incomeCustomCategories: List<String>,
    onRename: (CategoryType, String) -> Unit,
    onDelete: (CategoryType, String) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        elevation = CardDefaults.cardElevation(defaultElevation = 3.dp)
    ) {
        Column(modifier = Modifier.padding(12.dp)) {
            Text(
                text = stringResource(R.string.custom_categories),
                style = MaterialTheme.typography.titleMedium
            )
            Spacer(modifier = Modifier.height(8.dp))

            if (expenseCustomCategories.isEmpty() && incomeCustomCategories.isEmpty()) {
                Text(
                    text = stringResource(R.string.no_custom_categories),
                    style = MaterialTheme.typography.bodySmall
                )
            }

            if (expenseCustomCategories.isNotEmpty()) {
                Text(
                    text = stringResource(R.string.custom_categories_expense),
                    style = MaterialTheme.typography.titleSmall
                )
                expenseCustomCategories.forEach { name ->
                    CustomCategoryRow(
                        name = name,
                        onRename = { onRename(CategoryType.EXPENSE, name) },
                        onDelete = { onDelete(CategoryType.EXPENSE, name) }
                    )
                }
            }

            if (incomeCustomCategories.isNotEmpty()) {
                Spacer(modifier = Modifier.height(4.dp))
                Text(
                    text = stringResource(R.string.custom_categories_income),
                    style = MaterialTheme.typography.titleSmall
                )
                incomeCustomCategories.forEach { name ->
                    CustomCategoryRow(
                        name = name,
                        onRename = { onRename(CategoryType.INCOME, name) },
                        onDelete = { onDelete(CategoryType.INCOME, name) }
                    )
                }
            }
        }
    }
}

@Composable
private fun CustomCategoryRow(
    name: String,
    onRename: () -> Unit,
    onDelete: () -> Unit
) {
    Row(
        modifier = Modifier.fillMaxWidth(),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Text(
            text = name,
            style = MaterialTheme.typography.bodyMedium,
            modifier = Modifier.weight(1f)
        )
        IconButton(onClick = onRename) {
            Icon(
                imageVector = Icons.Default.Edit,
                contentDescription = stringResource(R.string.rename_category)
            )
        }
        IconButton(onClick = onDelete) {
            Icon(
                imageVector = Icons.Default.Delete,
                contentDescription = stringResource(R.string.delete_category)
            )
        }
    }
}

private fun showDatePicker(
    context: Context,
    initialEpochMs: Long,
    onDateSelected: (Long) -> Unit
) {
    val calendar = Calendar.getInstance().apply { timeInMillis = initialEpochMs }
    DatePickerDialog(
        context,
        { _, year, month, dayOfMonth ->
            val selectedCalendar = Calendar.getInstance().apply {
                set(Calendar.YEAR, year)
                set(Calendar.MONTH, month)
                set(Calendar.DAY_OF_MONTH, dayOfMonth)
                set(Calendar.HOUR_OF_DAY, 12)
                set(Calendar.MINUTE, 0)
                set(Calendar.SECOND, 0)
                set(Calendar.MILLISECOND, 0)
            }
            onDateSelected(selectedCalendar.timeInMillis)
        },
        calendar.get(Calendar.YEAR),
        calendar.get(Calendar.MONTH),
        calendar.get(Calendar.DAY_OF_MONTH)
    ).show()
}

@Composable
private fun CategoryGroupCard(
    group: TransactionGroup,
    entryText: String,
    isExpanded: Boolean,
    onToggle: () -> Unit,
    dateFormat: SimpleDateFormat,
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit
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
                        text = entryText,
                        style = MaterialTheme.typography.bodySmall
                    )
                }
                Text(
                    text = if (isExpanded) stringResource(R.string.collapse) else stringResource(R.string.expand),
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
                            onDelete = onDelete,
                            onEdit = onEdit
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
    onDelete: (Transaction) -> Unit,
    onEdit: (Transaction) -> Unit
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
                        text = stringResource(R.string.amount_euro, transaction.amount),
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
            Row {
                IconButton(onClick = { onEdit(transaction) }) {
                    Icon(
                        imageVector = Icons.Default.Edit,
                        contentDescription = stringResource(R.string.edit_transaction_cd)
                    )
                }
                IconButton(onClick = { onDelete(transaction) }) {
                    Icon(
                        imageVector = Icons.Default.Delete,
                        contentDescription = stringResource(R.string.delete_transaction_cd)
                    )
                }
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
            text = stringResource(R.string.legal_notice_1),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
        Text(
            text = stringResource(R.string.legal_notice_2),
            style = MaterialTheme.typography.bodySmall,
            textAlign = TextAlign.Center
        )
    }
}
