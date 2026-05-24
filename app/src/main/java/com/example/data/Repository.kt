package com.example.data

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.flow
import java.util.Calendar

class Repository(private val database: AppDatabase) {
    val expenseDao = database.expenseDao()
    val inventoryDao = database.inventoryDao()
    val shoppingDao = database.shoppingDao()
    val syncSettingsDao = database.syncSettingsDao()

    // Flow exports
    val allExpenses: Flow<List<Expense>> = expenseDao.getAllExpenses()
    val allInventoryItems: Flow<List<InventoryItem>> = inventoryDao.getAllInventoryItems()
    val lowStockItems: Flow<List<InventoryItem>> = inventoryDao.getLowStockItems()
    val allShoppingItems: Flow<List<ShoppingItem>> = shoppingDao.getAllShoppingItems()
    val syncSettings: Flow<SyncSettings?> = syncSettingsDao.getSyncSettings()

    // Sum sums
    fun getCurrentMonthStartTimestamp(): Long {
        val calendar = Calendar.getInstance()
        calendar.set(Calendar.DAY_OF_MONTH, 1)
        calendar.set(Calendar.HOUR_OF_DAY, 0)
        calendar.set(Calendar.MINUTE, 0)
        calendar.set(Calendar.SECOND, 0)
        calendar.set(Calendar.MILLISECOND, 0)
        return calendar.timeInMillis
    }

    val variableExpensesSum: Flow<Double?> = expenseDao.getVariableExpensesSum(getCurrentMonthStartTimestamp())
    val recurringExpensesSum: Flow<Double?> = expenseDao.getRecurringExpensesSum()

    // Mutators
    suspend fun insertExpense(expense: Expense) = expenseDao.insertExpense(expense)
    suspend fun deleteExpense(expense: Expense) = expenseDao.deleteExpense(expense)

    suspend fun insertInventoryItem(item: InventoryItem) = inventoryDao.insertInventoryItem(item)
    suspend fun deleteInventoryItem(item: InventoryItem) = inventoryDao.deleteInventoryItem(item)
    suspend fun updateInventoryItem(item: InventoryItem) = inventoryDao.updateInventoryItem(item)

    suspend fun insertShoppingItem(item: ShoppingItem) = shoppingDao.insertShoppingItem(item)
    suspend fun updateShoppingItem(item: ShoppingItem) = shoppingDao.updateShoppingItem(item)
    suspend fun deleteShoppingItem(item: ShoppingItem) = shoppingDao.deleteShoppingItem(item)
    suspend fun deleteBoughtShoppingItems() = shoppingDao.deleteBoughtItems()
    suspend fun clearAllShoppingItems() = shoppingDao.clearAll()

    suspend fun saveSyncSettings(settings: SyncSettings) = syncSettingsDao.saveSyncSettings(settings)

    suspend fun clearAllLocalData() {
        expenseDao.clearAllExpenses()
        inventoryDao.clearAllInventoryItems()
        shoppingDao.clearAll()
    }

    // Pre-populate Database with realistic household data if empty
    suspend fun prepopulateIfEmpty() {
        // Force complete clean state for Milton & Alejandra
        val settings = syncSettings.first()
        if (settings == null || !settings.members.contains("Alejandra") || settings.members.contains("Pilar")) {
            // Delete all local data
            clearAllLocalData()
            
            // Save clean settings
            val cleanSettings = SyncSettings(
                members = "Milton,Alejandra",
                activeUser = "Milton",
                householdCode = "HOGAR-5892"
            )
            syncSettingsDao.saveSyncSettings(cleanSettings)
        }
    }

}
