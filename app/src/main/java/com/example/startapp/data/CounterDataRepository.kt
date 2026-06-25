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
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

// Extension property to delegate DataStore creation to the context
private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class CounterDataRepository(private val context: Context) {

    // Define keys for each piece of data we want to store
    private val totalAmountKey = doublePreferencesKey("total_amount")
    private val dailyIncreaseKey = doublePreferencesKey("daily_increase")
    private val transactionsKey = stringPreferencesKey("transactions")
    private val dailySnapshotsKey = stringPreferencesKey("daily_snapshots")
    private val daysToDisplayKey = intPreferencesKey("days_to_display")
    private val maxHistoryDaysKey = intPreferencesKey("max_history_days")

    private val gson = Gson()

    // Flow to emit the total amount whenever it changes
    val totalAmount: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[totalAmountKey] ?: 0.0
        }

    // Flow to emit the daily increase amount whenever it changes
    val dailyIncrease: Flow<Double> = context.dataStore.data
        .map { preferences ->
            preferences[dailyIncreaseKey] ?: 0.0
        }

    // Flow to emit the list of transactions
    val transactions: Flow<List<Transaction>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[transactionsKey] ?: "[]"
            val type = object : TypeToken<List<Transaction>>() {}.type
            gson.fromJson(json, type)
        }

    // Flow to emit the list of daily snapshots
    val dailySnapshots: Flow<List<DailySnapshot>> = context.dataStore.data
        .map { preferences ->
            val json = preferences[dailySnapshotsKey] ?: "[]"
            val type = object : TypeToken<List<DailySnapshot>>() {}.type
            gson.fromJson(json, type)
        }

    // Flow to emit the number of days to display (for the chart view)
    val daysToDisplay: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[daysToDisplayKey] ?: 30 // Default to 30 days
        }
    
    // Flow to emit the max history days to keep
    val maxHistoryDays: Flow<Int> = context.dataStore.data
        .map { preferences ->
            preferences[maxHistoryDaysKey] ?: 900 // Default to 900 days
        }

    // Suspended function to update the total amount
    suspend fun updateTotalAmount(newAmount: Double) {
        context.dataStore.edit {
            it[totalAmountKey] = newAmount
        }
    }

    // Suspended function to update the daily increase amount
    suspend fun updateDailyIncrease(newAmount: Double) {
        context.dataStore.edit {
            it[dailyIncreaseKey] = newAmount
        }
    }

    suspend fun addTransaction(transaction: Transaction) {
        context.dataStore.edit { preferences ->
            val json = preferences[transactionsKey] ?: "[]"
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            val currentList: MutableList<Transaction> = gson.fromJson(json, type)
            val maxDays = preferences[maxHistoryDaysKey] ?: 900

            currentList.add(transaction)

            // Retention policy: Keep data up to maxDays
            val retentionLimit = System.currentTimeMillis() - (maxDays.toLong() * 24 * 60 * 60 * 1000)
            val filteredList = currentList.filter { it.date >= retentionLimit }

            preferences[transactionsKey] = gson.toJson(filteredList)
        }
    }

    suspend fun addDailySnapshot(snapshot: DailySnapshot) {
        context.dataStore.edit { preferences ->
            val json = preferences[dailySnapshotsKey] ?: "[]"
            val type = object : TypeToken<MutableList<DailySnapshot>>() {}.type
            val currentList: MutableList<DailySnapshot> = gson.fromJson(json, type)
            val maxDays = preferences[maxHistoryDaysKey] ?: 900

            currentList.add(snapshot)
            
            // Retention policy for snapshots
            val retentionLimit = System.currentTimeMillis() - (maxDays.toLong() * 24 * 60 * 60 * 1000)
            val filteredList = currentList.filter { it.date >= retentionLimit }
            
            preferences[dailySnapshotsKey] = gson.toJson(filteredList)
        }
    }

    suspend fun deleteTransaction(transactionId: String) {
        context.dataStore.edit { preferences ->
            val json = preferences[transactionsKey] ?: "[]"
            val type = object : TypeToken<MutableList<Transaction>>() {}.type
            val currentList: MutableList<Transaction> = gson.fromJson(json, type)

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

    suspend fun clearTransactions() {
        context.dataStore.edit { preferences ->
            preferences[transactionsKey] = "[]"
            preferences[dailySnapshotsKey] = "[]"
        }
    }

    suspend fun updateDaysToDisplay(days: Int) {
        context.dataStore.edit {
            it[daysToDisplayKey] = days
        }
    }

    suspend fun updateMaxHistoryDays(days: Int) {
        context.dataStore.edit {
            it[maxHistoryDaysKey] = days
        }
    }

    suspend fun resetDaysToDisplay() {
        context.dataStore.edit {
            it[daysToDisplayKey] = 30 // Reset to default
        }
    }
}
