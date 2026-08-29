package com.example.startapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.startapp.data.backup.ImportMode
import com.example.startapp.data.model.AppBackup
import com.example.startapp.domain.calendarDayKey
import com.example.startapp.domain.createNormalizedDateRange
import com.example.startapp.domain.defaultDateRange
import com.example.startapp.domain.model.CategoryCatalog
import com.example.startapp.domain.model.CategoryType
import com.example.startapp.domain.model.DateRangeFilter
import com.example.startapp.domain.model.DailySnapshot
import com.example.startapp.domain.model.ExpenseCategory
import com.example.startapp.domain.model.Transaction
import com.example.startapp.utils.AppLog
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.flow.map

private const val DEFAULT_DAYS_TO_DISPLAY = 30
private const val DEFAULT_MAX_HISTORY_DAYS = 900
private const val MILLIS_PER_DAY = 24L * 60L * 60L * 1000L

// Extension property to delegate DataStore creation to the context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

private val collapsedLegacyCategories = mapOf(
    "Home" to ExpenseCategory.SHOPPING.label,
    "Work" to ExpenseCategory.DIGITAL.label,
    "Subscriptions" to ExpenseCategory.DIGITAL.label,
    "Bills" to ExpenseCategory.DIGITAL.label
)

internal fun normalizeTransactionCategory(amount: Double, description: String, category: String?): String {
    if (amount >= 0) {
        return category
            ?.takeIf { it.isNotBlank() && it != ExpenseCategory.INCOME.label }
            ?: ExpenseCategory.inferIncomeCategory(description)
    }

    val collapsed = category?.let { collapsedLegacyCategories[it] }
    if (collapsed != null) {
        return ExpenseCategory.inferFromDescription(description).takeIf { it != ExpenseCategory.OTHER.label } ?: collapsed
    }

    val shouldReclassifyOther = category == ExpenseCategory.OTHER.label && amount < 0
    if (!category.isNullOrBlank() && !shouldReclassifyOther) {
        return category
    }
    return ExpenseCategory.inferFromDescription(description)
}

private fun List<Transaction>.normalizedTransactions(): List<Transaction> {
    return map { transaction ->
        transaction.copy(
            category = normalizeTransactionCategory(
                amount = transaction.amount,
                description = transaction.description,
                category = transaction.category
            )
        )
    }
}

private inline fun <reified T> Gson.safeParseList(json: String?, logLabel: String): List<T> {
    return try {
        fromJson<List<T>>(json ?: "[]", object : TypeToken<List<T>>() {}.type) ?: emptyList()
    } catch (t: Throwable) {
        AppLog.w("CounterDataRepository", "Corrupted $logLabel JSON, falling back to empty list", t)
        emptyList()
    }
}

private fun Gson.fromJsonStringList(json: String?): List<String> = safeParseList(json, "string-list")

class CounterDataRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    internal val dataStoreForTest: DataStore<Preferences> get() = dataStore

    private val totalAmountKey = doublePreferencesKey("total_amount")
    private val dailyIncreaseKey = doublePreferencesKey("daily_increase")
    private val transactionsKey = stringPreferencesKey("transactions")
    private val dailySnapshotsKey = stringPreferencesKey("daily_snapshots")
    private val daysToDisplayKey = intPreferencesKey("days_to_display")
    private val filterStartDateKey = longPreferencesKey("filter_start_date")
    private val filterEndDateKey = longPreferencesKey("filter_end_date")
    private val maxHistoryDaysKey = intPreferencesKey("max_history_days")
    private val expenseCustomCategoriesKey = stringPreferencesKey("expense_custom_categories")
    private val incomeCustomCategoriesKey = stringPreferencesKey("income_custom_categories")

    private val gson = Gson()

    val totalAmount: Flow<Double> = dataStore.data
        .map { preferences ->
            preferences[totalAmountKey] ?: 0.0
        }

    val dailyIncrease: Flow<Double> = dataStore.data
        .map { preferences ->
            preferences[dailyIncreaseKey] ?: 0.0
        }

    val transactions: Flow<List<Transaction>> = dataStore.data
        .map { preferences ->
            gson.safeParseList<Transaction>(preferences[transactionsKey], "transactions")
                .normalizedTransactions()
        }
        .flowOn(Dispatchers.Default)

    val dailySnapshots: Flow<List<DailySnapshot>> = dataStore.data
        .map { preferences ->
            gson.safeParseList<DailySnapshot>(preferences[dailySnapshotsKey], "daily_snapshots")
        }
        .flowOn(Dispatchers.Default)

    val daysToDisplay: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[daysToDisplayKey] ?: DEFAULT_DAYS_TO_DISPLAY
        }

    val dateRangeFilter: Flow<DateRangeFilter> = dataStore.data
        .map { preferences ->
            val default = defaultDateRange()
            val start = preferences[filterStartDateKey] ?: default.startEpochMs
            val end = preferences[filterEndDateKey] ?: default.endEpochMs
            createNormalizedDateRange(start, end)
        }

    val maxHistoryDays: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[maxHistoryDaysKey] ?: DEFAULT_MAX_HISTORY_DAYS
        }

    val expenseCustomCategories: Flow<List<String>> = dataStore.data
        .map { preferences ->
            gson.fromJsonStringList(preferences[expenseCustomCategoriesKey])
        }
        .flowOn(Dispatchers.Default)

    val incomeCustomCategories: Flow<List<String>> = dataStore.data
        .map { preferences ->
            gson.fromJsonStringList(preferences[incomeCustomCategoriesKey])
        }
        .flowOn(Dispatchers.Default)

    suspend fun updateTotalAmount(newAmount: Double) {
        dataStore.edit {
            it[totalAmountKey] = newAmount
        }
    }

    suspend fun updateDailyIncrease(newAmount: Double) {
        dataStore.edit {
            it[dailyIncreaseKey] = newAmount
        }
    }

    suspend fun addTransaction(transaction: Transaction) {
        recordTransaction(transaction, totalDelta = 0.0)
    }

    /**
     * Atomically appends a transaction (with retention policy) and applies
     * [totalDelta] to the running total inside a single DataStore edit.
     */
    suspend fun recordTransaction(transaction: Transaction, totalDelta: Double) {
        dataStore.edit { preferences ->
            val currentTotal = preferences[totalAmountKey] ?: 0.0
            preferences[totalAmountKey] = currentTotal + totalDelta

            val current = gson.safeParseList<Transaction>(preferences[transactionsKey], "transactions")
                .toMutableList()
            val maxDays = preferences[maxHistoryDaysKey] ?: DEFAULT_MAX_HISTORY_DAYS

            current.add(transaction)

            val retentionLimit = System.currentTimeMillis() - (maxDays.toLong() * MILLIS_PER_DAY)
            preferences[transactionsKey] = gson.toJson(current.filter { it.date >= retentionLimit })
        }
    }

    suspend fun addDailySnapshot(snapshot: DailySnapshot) {
        dataStore.edit { preferences ->
            val current = gson.safeParseList<DailySnapshot>(preferences[dailySnapshotsKey], "daily_snapshots")
                .toMutableList()
            val maxDays = preferences[maxHistoryDaysKey] ?: DEFAULT_MAX_HISTORY_DAYS

            current.add(snapshot)

            val retentionLimit = System.currentTimeMillis() - (maxDays.toLong() * MILLIS_PER_DAY)
            preferences[dailySnapshotsKey] = gson.toJson(current.filter { it.date >= retentionLimit })
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        dataStore.edit { preferences ->
            val current = gson.safeParseList<Transaction>(preferences[transactionsKey], "transactions")
                .toMutableList()

            val transactionToRemove = current.find { it.id == transactionId }
            if (transactionToRemove != null) {
                current.remove(transactionToRemove)
                preferences[transactionsKey] = gson.toJson(current)

                val currentTotal = preferences[totalAmountKey] ?: 0.0
                preferences[totalAmountKey] = currentTotal - transactionToRemove.amount
            }
        }
    }

    suspend fun updateTransaction(
        id: String,
        newAmount: Double,
        newDescription: String,
        newCategory: String
    ) {
        dataStore.edit { preferences ->
            val current = gson.safeParseList<Transaction>(preferences[transactionsKey], "transactions")
                .toMutableList()

            val index = current.indexOfFirst { it.id == id }
            if (index < 0) {
                return@edit
            }

            val old = current[index]
            val updated = old.copy(
                amount = newAmount,
                description = newDescription,
                category = newCategory
            )
            current[index] = updated
            preferences[transactionsKey] = gson.toJson(current)

            val currentTotal = preferences[totalAmountKey] ?: 0.0
            val delta = newAmount - old.amount
            preferences[totalAmountKey] = currentTotal + delta
        }
    }

    suspend fun clearTransactions() {
        dataStore.edit { preferences ->
            preferences[transactionsKey] = "[]"
            preferences[dailySnapshotsKey] = "[]"
        }
    }

    suspend fun normalizeStoredTransactions() {
        dataStore.edit { preferences ->
            val current = gson.safeParseList<Transaction>(preferences[transactionsKey], "transactions")
            val normalized = current.normalizedTransactions()
            if (normalized != current) {
                preferences[transactionsKey] = gson.toJson(normalized)
            }
        }
    }

    suspend fun updateDaysToDisplay(days: Int) {
        dataStore.edit {
            it[daysToDisplayKey] = days
        }
    }

    suspend fun updateDateRangeFilter(startEpochMs: Long, endEpochMs: Long) {
        val normalized = createNormalizedDateRange(startEpochMs, endEpochMs)
        dataStore.edit {
            it[filterStartDateKey] = normalized.startEpochMs
            it[filterEndDateKey] = normalized.endEpochMs
        }
    }

    suspend fun resetDateRangeFilter(now: Long = System.currentTimeMillis()) {
        val default = defaultDateRange(now)
        dataStore.edit {
            it[filterStartDateKey] = default.startEpochMs
            it[filterEndDateKey] = default.endEpochMs
        }
    }

    suspend fun updateMaxHistoryDays(days: Int) {
        dataStore.edit {
            it[maxHistoryDaysKey] = days
        }
    }

    suspend fun resetDaysToDisplay() {
        dataStore.edit {
            it[daysToDisplayKey] = DEFAULT_DAYS_TO_DISPLAY
        }
    }

    suspend fun addCustomCategory(type: CategoryType, rawName: String): String? {
        val key = customCategoriesKey(type)

        var created: String? = null
        dataStore.edit { preferences ->
            val current = gson.fromJsonStringList(preferences[key])
            if (!CategoryCatalog.canAddCustomCategory(type, rawName, current)) {
                return@edit
            }

            created = CategoryCatalog.normalizeCustomCategoryName(rawName)
            preferences[key] = gson.toJson((current + created!!).distinct().sorted())
        }
        return created
    }

    suspend fun renameCustomCategory(type: CategoryType, oldName: String, newName: String): String? {
        val key = customCategoriesKey(type)
        val normalizedNew = CategoryCatalog.normalizeCustomCategoryName(newName)

        var renamed: String? = null
        dataStore.edit { preferences ->
            val current = gson.fromJsonStringList(preferences[key])
            val oldNormalized = CategoryCatalog.normalizeCustomCategoryName(oldName)
            val withoutOld = current.filter { CategoryCatalog.normalizeCustomCategoryName(it) != oldNormalized }
            if (withoutOld.size == current.size) {
                return@edit
            }
            if (!CategoryCatalog.canAddCustomCategory(type, normalizedNew, withoutOld)) {
                return@edit
            }

            renamed = normalizedNew
            preferences[key] = gson.toJson((withoutOld + normalizedNew).distinct().sorted())
        }
        return renamed
    }

    suspend fun deleteCustomCategory(type: CategoryType, name: String) {
        val key = customCategoriesKey(type)
        val target = CategoryCatalog.normalizeCustomCategoryName(name)

        dataStore.edit { preferences ->
            val current = gson.fromJsonStringList(preferences[key])
            val updated = current.filter { CategoryCatalog.normalizeCustomCategoryName(it) != target }
            if (updated.size != current.size) {
                preferences[key] = gson.toJson(updated)
            }
        }
    }

    private fun customCategoriesKey(type: CategoryType) = when (type) {
        CategoryType.EXPENSE -> expenseCustomCategoriesKey
        CategoryType.INCOME -> incomeCustomCategoriesKey
    }

    suspend fun exportBackup(): AppBackup {
        val preferences = dataStore.data.first()

        val transactions = gson.safeParseList<Transaction>(preferences[transactionsKey], "transactions")
        val dailySnapshots = gson.safeParseList<DailySnapshot>(preferences[dailySnapshotsKey], "daily_snapshots")
        val expenseCustom = gson.fromJsonStringList(preferences[expenseCustomCategoriesKey])
        val incomeCustom = gson.fromJsonStringList(preferences[incomeCustomCategoriesKey])

        return AppBackup(
            schemaVersion = com.example.startapp.data.backup.BackupValidator.SUPPORTED_SCHEMA_VERSION,
            createdAtEpochMs = System.currentTimeMillis(),
            totalAmount = preferences[totalAmountKey] ?: 0.0,
            dailyIncrease = preferences[dailyIncreaseKey] ?: 0.0,
            daysToDisplay = preferences[daysToDisplayKey] ?: DEFAULT_DAYS_TO_DISPLAY,
            maxHistoryDays = preferences[maxHistoryDaysKey] ?: DEFAULT_MAX_HISTORY_DAYS,
            transactions = transactions,
            dailySnapshots = dailySnapshots,
            expenseCustomCategories = expenseCustom,
            incomeCustomCategories = incomeCustom
        )
    }

    suspend fun importBackup(backup: AppBackup, mode: ImportMode = ImportMode.REPLACE) {
        when (mode) {
            ImportMode.REPLACE -> importBackupReplace(backup)
            ImportMode.MERGE -> importBackupMerge(backup)
        }
        normalizeStoredTransactions()
    }

    private suspend fun importBackupReplace(backup: AppBackup) {
        val now = System.currentTimeMillis()
        val retentionLimit = now - (backup.maxHistoryDays.toLong() * MILLIS_PER_DAY)
        val filteredTransactions = backup.transactions.filter { it.date >= retentionLimit }
        val filteredSnapshots = backup.dailySnapshots.filter { it.date >= retentionLimit }

        dataStore.edit { preferences ->
            preferences[totalAmountKey] = backup.totalAmount
            preferences[dailyIncreaseKey] = backup.dailyIncrease
            preferences[daysToDisplayKey] = backup.daysToDisplay
            preferences[maxHistoryDaysKey] = backup.maxHistoryDays
            preferences[transactionsKey] = gson.toJson(filteredTransactions)
            preferences[dailySnapshotsKey] = gson.toJson(filteredSnapshots)
            preferences[expenseCustomCategoriesKey] = gson.toJson(backup.expenseCustomCategories.distinct().sorted())
            preferences[incomeCustomCategoriesKey] = gson.toJson(backup.incomeCustomCategories.distinct().sorted())
        }
    }

    private suspend fun importBackupMerge(backup: AppBackup) {
        dataStore.edit { preferences ->
            val currentTransactions = gson.safeParseList<Transaction>(preferences[transactionsKey], "transactions")
            val currentSnapshots = gson.safeParseList<DailySnapshot>(preferences[dailySnapshotsKey], "daily_snapshots")
            val currentExpenseCustom = gson.fromJsonStringList(preferences[expenseCustomCategoriesKey])
            val currentIncomeCustom = gson.fromJsonStringList(preferences[incomeCustomCategoriesKey])
            val maxDays = preferences[maxHistoryDaysKey] ?: DEFAULT_MAX_HISTORY_DAYS

            // Union of transactions by id: current entries win on conflicts.
            val transactionsById = currentTransactions.associateBy { it.id }.toMutableMap()
            backup.transactions.forEach { transaction ->
                transactionsById.putIfAbsent(transaction.id, transaction)
            }

            // Union of snapshots by calendar day: the most recent entry wins.
            val snapshotsByDay = currentSnapshots
                .groupBy { calendarDayKey(it.date) }
                .mapValues { (_, daySnapshots) -> daySnapshots.maxByOrNull(DailySnapshot::date)!! }
                .toMutableMap()
            backup.dailySnapshots.forEach { snapshot ->
                val day = calendarDayKey(snapshot.date)
                val existing = snapshotsByDay[day]
                if (existing == null || snapshot.date >= existing.date) {
                    snapshotsByDay[day] = snapshot
                }
            }

            val mergedTransactions = transactionsById.values.toList()
            val mergedSnapshots = snapshotsByDay.values.sortedBy(DailySnapshot::date)

            // Settings (total, daily increase, retention) stay on current values.
            val retentionLimit = System.currentTimeMillis() - (maxDays.toLong() * MILLIS_PER_DAY)

            preferences[transactionsKey] = gson.toJson(mergedTransactions.filter { it.date >= retentionLimit })
            preferences[dailySnapshotsKey] = gson.toJson(mergedSnapshots.filter { it.date >= retentionLimit })
            preferences[expenseCustomCategoriesKey] =
                gson.toJson((currentExpenseCustom + backup.expenseCustomCategories).distinct().sorted())
            preferences[incomeCustomCategoriesKey] =
                gson.toJson((currentIncomeCustom + backup.incomeCustomCategories).distinct().sorted())
        }
    }
}
