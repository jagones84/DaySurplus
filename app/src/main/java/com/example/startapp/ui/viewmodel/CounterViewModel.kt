package com.example.startapp.ui.viewmodel

import android.app.Application
import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import com.example.startapp.data.CounterDataRepository
import com.example.startapp.data.backup.BackupCodec
import com.example.startapp.data.backup.BackupValidator
import com.example.startapp.data.backup.ImportMode
import com.example.startapp.domain.model.DailySnapshot
import com.example.startapp.domain.model.Transaction
import com.example.startapp.domain.defaultDateRange
import com.example.startapp.domain.filterTransactionsByDateRange
import com.example.startapp.domain.presetDateRange
import com.example.startapp.domain.transactionsToCsv
import com.example.startapp.domain.model.CategoryType
import com.example.startapp.domain.model.DateRangeFilter
import com.example.startapp.domain.model.ExpenseCategory
import com.example.startapp.domain.model.GroupedTransactionState
import com.example.startapp.domain.model.buildGroupedTransactionState
import com.example.startapp.utils.AppLog
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.combine
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch

class CounterViewModel(private val repository: CounterDataRepository) : ViewModel() {

    private val _totalAmount = MutableStateFlow(0.0)
    val totalAmount = _totalAmount.asStateFlow()

    private val _dailyIncrease = MutableStateFlow(0.0)
    val dailyIncrease = _dailyIncrease.asStateFlow()

    private val _transactions = MutableStateFlow<List<Transaction>>(emptyList())
    val transactions = _transactions.asStateFlow()

    private val _groupedTransactions = MutableStateFlow(GroupedTransactionState(emptyList(), emptyList()))
    val groupedTransactions = _groupedTransactions.asStateFlow()

    private val _dailySnapshots = MutableStateFlow<List<DailySnapshot>>(emptyList())
    val dailySnapshots = _dailySnapshots.asStateFlow()

    private val _daysToDisplay = MutableStateFlow(30) // Default value, will be updated from repository
    val daysToDisplay = _daysToDisplay.asStateFlow()

    private val _dateRangeFilter = MutableStateFlow(defaultDateRange())
    val dateRangeFilter = _dateRangeFilter.asStateFlow()

    private val _expenseCustomCategories = MutableStateFlow<List<String>>(emptyList())
    val expenseCustomCategories = _expenseCustomCategories.asStateFlow()

    private val _incomeCustomCategories = MutableStateFlow<List<String>>(emptyList())
    val incomeCustomCategories = _incomeCustomCategories.asStateFlow()

    private val _maxHistoryDays = MutableStateFlow(900)
    val maxHistoryDays = _maxHistoryDays.asStateFlow()

    init {
        viewModelScope.launch {
            repository.totalAmount.collect { _totalAmount.value = it }
        }

        viewModelScope.launch {
            repository.dailyIncrease.collect { _dailyIncrease.value = it }
        }

        viewModelScope.launch {
            repository.normalizeStoredTransactions()
            repository.transactions.collect { _transactions.value = it }
        }

        viewModelScope.launch {
            repository.dailySnapshots.collect {
                _dailySnapshots.value = it
            }
        }

        viewModelScope.launch {
            repository.daysToDisplay.collect { _daysToDisplay.value = it }
        }

        viewModelScope.launch {
            repository.dateRangeFilter.collect { _dateRangeFilter.value = it }
        }

        viewModelScope.launch {
            combine(repository.transactions, repository.dateRangeFilter) { transactions, range ->
                buildGroupedTransactionState(filterTransactionsByDateRange(transactions, range))
            }.collect { _groupedTransactions.value = it }
        }

        viewModelScope.launch {
            repository.expenseCustomCategories.collect { _expenseCustomCategories.value = it }
        }

        viewModelScope.launch {
            repository.incomeCustomCategories.collect { _incomeCustomCategories.value = it }
        }

        viewModelScope.launch {
            repository.maxHistoryDays.collect { _maxHistoryDays.value = it }
        }
    }

    fun updateDaysToDisplay(days: Int) {
        viewModelScope.launch {
            repository.updateDaysToDisplay(days)
        }
    }

    fun updateDateRangeFilter(startEpochMs: Long, endEpochMs: Long) {
        viewModelScope.launch {
            repository.updateDateRangeFilter(startEpochMs, endEpochMs)
        }
    }

    fun applyAnalysisWindow(days: Int, now: Long = System.currentTimeMillis()) {
        viewModelScope.launch {
            val range = presetDateRange(days = days, now = now)
            repository.updateDaysToDisplay(days)
            repository.updateDateRangeFilter(
                startEpochMs = range.startEpochMs,
                endEpochMs = range.endEpochMs
            )
        }
    }

    fun resetDateRangeFilter() {
        viewModelScope.launch {
            repository.resetDaysToDisplay()
            repository.resetDateRangeFilter()
        }
    }

    fun addAmount(amount: Double, description: String, category: String = ExpenseCategory.INCOME.label) {
        viewModelScope.launch {
            repository.recordTransaction(
                transaction = Transaction(
                    amount = amount,
                    date = System.currentTimeMillis(),
                    description = description,
                    category = category
                ),
                totalDelta = amount
            )
        }
    }

    fun subtractAmount(amount: Double, description: String, category: String) {
        viewModelScope.launch {
            repository.recordTransaction(
                transaction = Transaction(
                    amount = -amount,
                    date = System.currentTimeMillis(),
                    description = description,
                    category = category
                ),
                totalDelta = -amount
            )
        }
    }

    fun deleteTransaction(transaction: Transaction) {
        viewModelScope.launch {
            repository.deleteTransaction(transaction.id)
        }
    }

    fun updateTransaction(id: String, newAmount: Double, newDescription: String, newCategory: String) {
        viewModelScope.launch {
            repository.updateTransaction(
                id = id,
                newAmount = newAmount,
                newDescription = newDescription,
                newCategory = newCategory
            )
        }
    }

    fun addCustomCategory(type: CategoryType, name: String, onCreated: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.addCustomCategory(type, name)?.let(onCreated)
        }
    }

    fun renameCustomCategory(type: CategoryType, oldName: String, newName: String, onRenamed: (String) -> Unit = {}) {
        viewModelScope.launch {
            repository.renameCustomCategory(type, oldName, newName)?.let(onRenamed)
        }
    }

    fun deleteCustomCategory(type: CategoryType, name: String) {
        viewModelScope.launch {
            repository.deleteCustomCategory(type, name)
        }
    }

    suspend fun exportBackupJson(): String {
        return BackupCodec.encode(repository.exportBackup())
    }

    suspend fun exportCsv(): String {
        return transactionsToCsv(repository.transactions.first())
    }

    suspend fun importBackupJson(json: String, mode: ImportMode): String? {
        val decoded = try {
            BackupCodec.decode(json)
        } catch (t: Throwable) {
            AppLog.w("CounterViewModel", "Backup decode failed", t)
            return "Invalid JSON: ${t.message ?: "unknown error"}"
        }

        if (!BackupValidator.validate(decoded)) {
            AppLog.w("CounterViewModel", "Backup rejected by validator")
            return "Unsupported schema version or invalid values"
        }

        repository.importBackup(decoded, mode)
        AppLog.i("CounterViewModel", "Backup imported in $mode mode")
        return null
    }

    fun updateDailyIncrease(amount: Double) {
        viewModelScope.launch {
            repository.updateDailyIncrease(amount)
        }
    }

    fun updateMaxHistoryDays(days: Int) {
        viewModelScope.launch {
            repository.updateMaxHistoryDays(days)
        }
    }

    fun reset() {
        viewModelScope.launch {
            repository.updateTotalAmount(0.0)
            repository.updateDailyIncrease(0.0)
            repository.clearTransactions()
            repository.resetDaysToDisplay()
            repository.resetDateRangeFilter()
        }
    }
}

class CounterViewModelFactory(private val application: Application) : ViewModelProvider.Factory {
    override fun <T : ViewModel> create(modelClass: Class<T>): T {
        if (modelClass.isAssignableFrom(CounterViewModel::class.java)) {
            @Suppress("UNCHECKED_CAST")
            return CounterViewModel(CounterDataRepository(application)) as T
        }
        throw IllegalArgumentException("Unknown ViewModel class")
    }
}
