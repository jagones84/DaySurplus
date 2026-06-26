package com.example.startapp.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.doublePreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import com.example.startapp.data.model.DailySnapshot
import com.example.startapp.data.model.Transaction
import com.example.startapp.domain.model.CategoryCatalog
import com.example.startapp.domain.model.CategoryType
import com.example.startapp.domain.model.ExpenseCategory
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

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

private fun Gson.fromJsonStringList(json: String?): List<String> {
    val type = object : TypeToken<List<String>>() {}.type
    return fromJson<List<String>>(json ?: "[]", type) ?: emptyList()
}

class CounterDataRepository(private val dataStore: DataStore<Preferences>) {

    constructor(context: Context) : this(context.dataStore)

    // Define keys for each piece of data we want to store
    private val totalAmountKey = doublePreferencesKey("total_amount")
    private val dailyIncreaseKey = doublePreferencesKey("daily_increase")
    private val transactionsKey = stringPreferencesKey("transactions")
    private val dailySnapshotsKey = stringPreferencesKey("daily_snapshots")
    private val daysToDisplayKey = intPreferencesKey("days_to_display")
    private val maxHistoryDaysKey = intPreferencesKey("max_history_days")
    private val expenseCustomCategoriesKey = stringPreferencesKey("expense_custom_categories")
    private val incomeCustomCategoriesKey = stringPreferencesKey("income_custom_categories")

    private val gson = Gson()

    // Flow to emit the total amount whenever it changes
    val totalAmount: Flow<Double> = dataStore.data
        .map { preferences ->
            preferences[totalAmountKey] ?: 0.0
        }

    // Flow to emit the daily increase amount whenever it changes
    val dailyIncrease: Flow<Double> = dataStore.data
        .map { preferences ->
            preferences[dailyIncreaseKey] ?: 0.0
        }

    // Flow to emit the list of transactions
    val transactions: Flow<List<Transaction>> = dataStore.data
        .map { preferences ->
            val json = preferences[transactionsKey] ?: "[]"
            val type = object : TypeToken<List<Transaction>>() {}.type
            val current: List<Transaction> = gson.fromJson(json, type) ?: emptyList()
            current.normalizedTransactions()
        }

    // Flow to emit the list of daily snapshots
    val dailySnapshots: Flow<List<DailySnapshot>> = dataStore.data
        .map { preferences ->
            val json = preferences[dailySnapshotsKey] ?: "[]"
            val type = object : TypeToken<List<DailySnapshot>>() {}.type
            gson.fromJson(json, type)
        }

    // Flow to emit the number of days to display (for the chart view)
    val daysToDisplay: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[daysToDisplayKey] ?: 30 // Default to 30 days
        }
    
    // Flow to emit the max history days to keep
    val maxHistoryDays: Flow<Int> = dataStore.data
        .map { preferences ->
            preferences[maxHistoryDaysKey] ?: 900 // Default to 900 days
        }

    val expenseCustomCategories: Flow<List<String>> = dataStore.data
        .map { preferences ->
            gson.fromJsonStringList(preferences[expenseCustomCategoriesKey])
        }

    val incomeCustomCategories: Flow<List<String>> = dataStore.data
        .map { preferences ->
            gson.fromJsonStringList(preferences[incomeCustomCategoriesKey])
        }

    // Suspended function to update the total amount
    suspend fun updateTotalAmount(newAmount: Double) {
        dataStore.edit {
            it[totalAmountKey] = newAmount
        }
    }

    // Suspended function to update the daily increase amount
    suspend fun updateDailyIncrease(newAmount: Double) {
        dataStore.edit {
            it[dailyIncreaseKey] = newAmount
        }
    }

    suspend fun addTransaction(transaction: Transaction) {
        dataStore.edit { preferences ->
            val json = preferences[transactionsKey] ?: "[]"
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            val currentList: MutableList<Transaction> = gson.fromJson(json, type) ?: mutableListOf()
            val maxDays = preferences[maxHistoryDaysKey] ?: 900

            currentList.add(transaction)

            // Retention policy: Keep data up to maxDays
            val retentionLimit = System.currentTimeMillis() - (maxDays.toLong() * 24 * 60 * 60 * 1000)
            val filteredList = currentList.filter { it.date >= retentionLimit }

            preferences[transactionsKey] = gson.toJson(filteredList)
        }
    }

    suspend fun addDailySnapshot(snapshot: DailySnapshot) {
        dataStore.edit { preferences ->
            val json = preferences[dailySnapshotsKey] ?: "[]"
            val type = object : TypeToken<MutableList<DailySnapshot>>() {}.type
            val currentList: MutableList<DailySnapshot> = gson.fromJson(json, type) ?: mutableListOf()
            val maxDays = preferences[maxHistoryDaysKey] ?: 900

            currentList.add(snapshot)
            
            // Retention policy for snapshots
            val retentionLimit = System.currentTimeMillis() - (maxDays.toLong() * 24 * 60 * 60 * 1000)
            val filteredList = currentList.filter { it.date >= retentionLimit }
            
            preferences[dailySnapshotsKey] = gson.toJson(filteredList)
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        dataStore.edit { preferences ->
            val json = preferences[transactionsKey] ?: "[]"
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            val currentList: MutableList<Transaction> = gson.fromJson(json, type) ?: mutableListOf()

            val transactionToRemove = currentList.find { it.id == transactionId }
            if (transactionToRemove != null) {
                currentList.remove(transactionToRemove)
                preferences[transactionsKey] = gson.toJson(currentList)

                // Re-update total amount by reversing the transaction
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
            val json = preferences[transactionsKey] ?: "[]"
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            val currentList: MutableList<Transaction> = gson.fromJson(json, type) ?: mutableListOf()

            val index = currentList.indexOfFirst { it.id == id }
            if (index < 0) {
                return@edit
            }

            val old = currentList[index]
            val updated = old.copy(
                amount = newAmount,
                description = newDescription,
                category = newCategory
            )
            currentList[index] = updated
            preferences[transactionsKey] = gson.toJson(currentList)

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
            val json = preferences[transactionsKey] ?: "[]"
            val type = object : TypeToken<List<Transaction>>() {}.type
            val current: List<Transaction> = gson.fromJson(json, type) ?: emptyList()
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

    suspend fun updateMaxHistoryDays(days: Int) {
        dataStore.edit {
            it[maxHistoryDaysKey] = days
        }
    }

    suspend fun resetDaysToDisplay() {
        dataStore.edit {
            it[daysToDisplayKey] = 30 // Reset to default
        }
    }

    suspend fun addCustomCategory(type: CategoryType, rawName: String): String? {
        val key = when (type) {
            CategoryType.EXPENSE -> expenseCustomCategoriesKey
            CategoryType.INCOME -> incomeCustomCategoriesKey
        }

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
}
