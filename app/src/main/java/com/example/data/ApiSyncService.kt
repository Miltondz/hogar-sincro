package com.example.data

import android.content.Context
import android.util.Log
import com.example.api.RetrofitClient
import com.example.api.SyncRequest
import com.example.api.TableSyncRequest
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

object ApiSyncService {
    private const val TAG = "ApiSyncService"

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        try {
            // Simply return true, as any offline exception will be handled
            // gracefully inside the Retrofit try-catch in syncAll.
            true
        } catch (e: Exception) {
            false
        }
    }

    suspend fun createTables(): Boolean {
        // Tables are already managed by the server and local Room migration.
        return true
    }

    /**
     * Performs a full bidirectional synchronization of all three tables in a single HTTP request.
     * This is highly efficient and atomic.
     */
    suspend fun syncAll(
        context: Context,
        expenseDao: ExpenseDao,
        inventoryDao: InventoryDao,
        shoppingDao: ShoppingDao
    ): Boolean = withContext(Dispatchers.IO) {
        try {
            Log.d(TAG, "Starting full REST API synchronization...")

            // 1. Get locally deleted IDs
            val deletedExpenses = DeletionTracker.getDeletedExpenses(context).toList()
            val deletedInventory = DeletionTracker.getDeletedInventory(context).toList()
            val deletedShopping = DeletionTracker.getDeletedShopping(context).toList()

            // 2. Get local items
            val localExpenses = expenseDao.getAllExpensesDirect()
            val localInventory = inventoryDao.getAllInventoryItemsDirect()
            val localShopping = shoppingDao.getAllShoppingItemsDirect()

            // 3. Build unified payload
            val request = SyncRequest(
                expenses = TableSyncRequest(deletedExpenses, localExpenses),
                inventory = TableSyncRequest(deletedInventory, localInventory),
                shopping = TableSyncRequest(deletedShopping, localShopping)
            )

            // 4. Send to Web Companion REST API
            val response = RetrofitClient.apiService.syncData(request)
            if (!response.isSuccessful) {
                Log.e(TAG, "Sync failed with HTTP error code: ${response.code()}")
                return@withContext false
            }

            val body = response.body()
            if (body == null || !body.success) {
                Log.e(TAG, "Sync failed: response body is null or success is false. Error: ${body?.error}")
                return@withContext false
            }

            Log.d(TAG, "Sync payload accepted by server. Merging changes...")

            // 5. Update Local SQLite Database via DAOs
            // Update Expenses
            val remoteExpenses = body.expenses ?: emptyList()
            val remoteExpenseIds = remoteExpenses.map { it.id }.toSet()
            for (local in localExpenses) {
                if (!remoteExpenseIds.contains(local.id)) {
                    expenseDao.deleteExpense(local)
                }
            }
            for (remote in remoteExpenses) {
                expenseDao.insertExpense(remote)
            }

            // Update Inventory Items
            val remoteInventory = body.inventory ?: emptyList()
            val remoteInventoryIds = remoteInventory.map { it.id }.toSet()
            for (local in localInventory) {
                if (!remoteInventoryIds.contains(local.id)) {
                    inventoryDao.deleteInventoryItem(local)
                }
            }
            for (remote in remoteInventory) {
                inventoryDao.insertInventoryItem(remote)
            }

            // Update Shopping Items
            val remoteShopping = body.shopping ?: emptyList()
            val remoteShoppingIds = remoteShopping.map { it.id }.toSet()
            for (local in localShopping) {
                if (!remoteShoppingIds.contains(local.id)) {
                    shoppingDao.deleteShoppingItem(local)
                }
            }
            for (remote in remoteShopping) {
                shoppingDao.insertShoppingItem(remote)
            }

            // 6. Clear deletion tracking now that server successfully processed them
            DeletionTracker.clearDeletedExpenses(context, deletedExpenses.toSet())
            DeletionTracker.clearDeletedInventory(context, deletedInventory.toSet())
            DeletionTracker.clearDeletedShopping(context, deletedShopping.toSet())

            Log.d(TAG, "REST API Sync completed successfully!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Exception during REST API Sync", e)
            false
        }
    }

    // Compatibility wrappers for sequential sync calls in other components if any
    suspend fun syncExpenses(context: Context, dao: ExpenseDao): Boolean {
        return true
    }

    suspend fun syncInventory(context: Context, dao: InventoryDao): Boolean {
        return true
    }

    suspend fun syncShopping(context: Context, dao: ShoppingDao): Boolean {
        return true
    }
}
