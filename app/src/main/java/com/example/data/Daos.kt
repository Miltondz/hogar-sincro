package com.example.data

import androidx.room.*
import kotlinx.coroutines.flow.Flow

@Dao
interface ExpenseDao {
    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    fun getAllExpenses(): Flow<List<Expense>>

    @Query("SELECT * FROM expenses ORDER BY timestamp DESC")
    suspend fun getAllExpensesDirect(): List<Expense>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertExpense(expense: Expense)

    @Update
    suspend fun updateExpense(expense: Expense)

    @Delete
    suspend fun deleteExpense(expense: Expense)

    @Query("SELECT SUM(amount) FROM expenses WHERE isRecurring = 0 AND timestamp >= :startAndEndTimestamp")
    fun getVariableExpensesSum(startAndEndTimestamp: Long): Flow<Double?>

    @Query("SELECT SUM(amount) FROM expenses WHERE isRecurring = 1")
    fun getRecurringExpensesSum(): Flow<Double?>

    @Query("DELETE FROM expenses")
    suspend fun clearAllExpenses()
}

@Dao
interface InventoryDao {
    @Query("SELECT * FROM inventory_items WHERE isArchived = 0 ORDER BY name ASC")
    fun getAllInventoryItems(): Flow<List<InventoryItem>>

    @Query("SELECT * FROM inventory_items WHERE isArchived = 0 ORDER BY name ASC")
    suspend fun getAllInventoryItemsDirect(): List<InventoryItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertInventoryItem(item: InventoryItem)

    @Update
    suspend fun updateInventoryItem(item: InventoryItem)

    @Delete
    suspend fun deleteInventoryItem(item: InventoryItem)

    @Query("SELECT * FROM inventory_items WHERE isArchived = 0 AND currentStock <= minStockAlert")
    fun getLowStockItems(): Flow<List<InventoryItem>>

    @Query("DELETE FROM inventory_items")
    suspend fun clearAllInventoryItems()
}

@Dao
interface ShoppingDao {
    @Query("SELECT * FROM shopping_items ORDER BY isBought ASC, productName ASC")
    fun getAllShoppingItems(): Flow<List<ShoppingItem>>

    @Query("SELECT * FROM shopping_items ORDER BY isBought ASC, productName ASC")
    suspend fun getAllShoppingItemsDirect(): List<ShoppingItem>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun insertShoppingItem(item: ShoppingItem)

    @Update
    suspend fun updateShoppingItem(item: ShoppingItem)

    @Delete
    suspend fun deleteShoppingItem(item: ShoppingItem)

    @Query("DELETE FROM shopping_items WHERE isBought = 1")
    suspend fun deleteBoughtItems()

    @Query("DELETE FROM shopping_items")
    suspend fun clearAll()
}

@Dao
interface SyncSettingsDao {
    @Query("SELECT * FROM sync_settings WHERE id = 1")
    fun getSyncSettings(): Flow<SyncSettings?>

    @Insert(onConflict = OnConflictStrategy.REPLACE)
    suspend fun saveSyncSettings(settings: SyncSettings)
}
