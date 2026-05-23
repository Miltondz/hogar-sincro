package com.example.data

import android.content.Context
import android.util.Log
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.sql.Connection
import java.sql.DriverManager
import java.sql.Statement

object NeonDatabaseHelper {
    private const val TAG = "NeonDatabaseHelper"
    
    // Explicit connection parameters specified by user
    private const val CONNECTION_URL = "jdbc:postgresql://ep-blue-water-aco8ck54-pooler.sa-east-1.aws.neon.tech/neondb?sslmode=require&user=neondb_owner&password=npg_Nu0Yhmp1GrBP"

    init {
        // Load PostgreSQL JDBC Driver
        try {
            Class.forName("org.postgresql.Driver")
            Log.d(TAG, "PostgreSQL JDBC Driver loaded successfully")
        } catch (e: Exception) {
            Log.e(TAG, "Failed to load PostgreSQL JDBC Driver", e)
        }
    }

    private suspend fun getConnection(): Connection? = withContext(Dispatchers.IO) {
        try {
            DriverManager.getConnection(CONNECTION_URL)
        } catch (e: Exception) {
            Log.e(TAG, "Error connecting to PostgreSQL", e)
            null
        }
    }

    suspend fun testConnection(): Boolean = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection()
            if (conn != null) {
                Log.d(TAG, "Successfully connected to Neon PostgreSQL!")
                true
            } else {
                false
            }
        } catch (e: Exception) {
            false
        } finally {
            conn?.close()
        }
    }

    suspend fun createTables(): Boolean = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        var stmt: Statement? = null
        try {
            conn = getConnection() ?: return@withContext false
            stmt = conn.createStatement()
            
            // Create expenses table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS expenses (
                    id SERIAL PRIMARY KEY,
                    title TEXT NOT NULL,
                    amount DOUBLE PRECISION NOT NULL,
                    category TEXT NOT NULL,
                    timestamp BIGINT NOT NULL,
                    is_recurring BOOLEAN NOT NULL DEFAULT FALSE,
                    recurring_due_date TEXT,
                    paid_by TEXT NOT NULL DEFAULT 'Milton'
                )
            """.trimIndent())

            // Create inventory_items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS inventory_items (
                    id SERIAL PRIMARY KEY,
                    name TEXT NOT NULL,
                    current_stock DOUBLE PRECISION NOT NULL,
                    min_stock_alert DOUBLE PRECISION NOT NULL,
                    unit TEXT NOT NULL,
                    depletion_rate_per_day DOUBLE PRECISION NOT NULL,
                    best_store TEXT,
                    best_price DOUBLE PRECISION,
                    second_best_store TEXT,
                    second_best_price DOUBLE PRECISION,
                    last_updated BIGINT NOT NULL
                )
            """.trimIndent())

            // Create shopping_items table
            stmt.execute("""
                CREATE TABLE IF NOT EXISTS shopping_items (
                    id SERIAL PRIMARY KEY,
                    product_name TEXT NOT NULL,
                    quantity_to_buy DOUBLE PRECISION NOT NULL,
                    unit TEXT NOT NULL DEFAULT 'u',
                    estimated_price DOUBLE PRECISION NOT NULL DEFAULT 0.0,
                    is_bought BOOLEAN NOT NULL DEFAULT FALSE,
                    target_store TEXT
                )
            """.trimIndent())

            Log.d(TAG, "Neon PostgreSQL Tables checked/created successfully.")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to create Neon PostgreSQL tables", e)
            false
        } finally {
            stmt?.close()
            conn?.close()
        }
    }

    // Bidirectional sync for expenses
    suspend fun syncExpenses(context: Context, dao: ExpenseDao): Boolean = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection() ?: return@withContext false
            conn.autoCommit = false
            
            // Delete locally-tracked deleted items from remote
            val deletedIds = DeletionTracker.getDeletedExpenses(context)
            if (deletedIds.isNotEmpty()) {
                try {
                    val delStmt = conn.prepareStatement("DELETE FROM expenses WHERE id = ?")
                    for (id in deletedIds) {
                        delStmt.setInt(1, id)
                        delStmt.addBatch()
                    }
                    delStmt.executeBatch()
                    delStmt.close()
                    DeletionTracker.clearDeletedExpenses(context, deletedIds)
                    conn.commit()
                } catch (e: Exception) {
                    Log.e(TAG, "Error performing remote expense deletions", e)
                }
            }
            
            // 1. Fetch remote expenses
            val remoteExpenses = mutableListOf<Expense>()
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT id, title, amount, category, timestamp, is_recurring, recurring_due_date, paid_by FROM expenses ORDER BY id ASC")
            while (rs.next()) {
                remoteExpenses.add(
                    Expense(
                        id = rs.getInt("id"),
                        title = rs.getString("title"),
                        amount = rs.getDouble("amount"),
                        category = rs.getString("category"),
                        timestamp = rs.getLong("timestamp"),
                        isRecurring = rs.getBoolean("is_recurring"),
                        recurringDueDate = rs.getString("recurring_due_date"),
                        paidBy = rs.getString("paid_by")
                    )
                )
            }
            rs.close()
            stmt.close()

            // 2. Fetch local expenses which aren't in remote (or simply merge based on ID & content)
            val localExpenses = dao.getAllExpensesDirect()
            
            // If local is empty, populate from remote
            if (localExpenses.isEmpty() && remoteExpenses.isNotEmpty()) {
                for (remote in remoteExpenses) {
                    dao.insertExpense(remote)
                }
            } else {
                // Determine remote max ID or list of IDs
                val remoteIds = remoteExpenses.map { it.id }.toSet()
                
                // Upload local expenses that are not remote
                val pstmt = conn.prepareStatement("""
                    INSERT INTO expenses (id, title, amount, category, timestamp, is_recurring, recurring_due_date, paid_by)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                    title = EXCLUDED.title,
                    amount = EXCLUDED.amount,
                    category = EXCLUDED.category,
                    timestamp = EXCLUDED.timestamp,
                    is_recurring = EXCLUDED.is_recurring,
                    recurring_due_date = EXCLUDED.recurring_due_date,
                    paid_by = EXCLUDED.paid_by
                """.trimIndent())
                
                for (local in localExpenses) {
                    pstmt.setInt(1, local.id)
                    pstmt.setString(2, local.title)
                    pstmt.setDouble(3, local.amount)
                    pstmt.setString(4, local.category)
                    pstmt.setLong(5, local.timestamp)
                    pstmt.setBoolean(6, local.isRecurring)
                    pstmt.setString(7, local.recurringDueDate ?: "")
                    pstmt.setString(8, local.paidBy)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
                pstmt.close()
                
                // Commit changes to remote
                conn.commit()

                // Insert into local what is remote but not local
                val localIds = localExpenses.map { it.id }.toSet()
                for (remote in remoteExpenses) {
                    if (!localIds.contains(remote.id)) {
                        dao.insertExpense(remote)
                    }
                }
            }

            Log.d(TAG, "Synced Expenses with Neon PostgreSQL!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing expenses", e)
            conn?.rollback()
            false
        } finally {
            conn?.close()
        }
    }

    // Bidirectional sync for inventory items
    suspend fun syncInventory(context: Context, dao: InventoryDao): Boolean = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection() ?: return@withContext false
            conn.autoCommit = false
            
            // Delete locally-tracked deleted items from remote
            val deletedIds = DeletionTracker.getDeletedInventory(context)
            if (deletedIds.isNotEmpty()) {
                try {
                    val delStmt = conn.prepareStatement("DELETE FROM inventory_items WHERE id = ?")
                    for (id in deletedIds) {
                        delStmt.setInt(1, id)
                        delStmt.addBatch()
                    }
                    delStmt.executeBatch()
                    delStmt.close()
                    DeletionTracker.clearDeletedInventory(context, deletedIds)
                    conn.commit()
                } catch (e: Exception) {
                    Log.e(TAG, "Error performing remote inventory deletions", e)
                }
            }
            
            // 1. Fetch remote items
            val remoteItems = mutableListOf<InventoryItem>()
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("""
                SELECT id, name, current_stock, min_stock_alert, unit, depletion_rate_per_day, 
                       best_store, best_price, second_best_store, second_best_price, last_updated 
                FROM inventory_items ORDER BY id ASC
            """.trimIndent())
            while (rs.next()) {
                remoteItems.add(
                    InventoryItem(
                        id = rs.getInt("id"),
                        name = rs.getString("name"),
                        currentStock = rs.getDouble("current_stock"),
                        minStockAlert = rs.getDouble("min_stock_alert"),
                        unit = rs.getString("unit"),
                        depletionRatePerDay = rs.getDouble("depletion_rate_per_day"),
                        bestStore = rs.getString("best_store"),
                        bestPrice = rs.getDouble("best_price"),
                        secondBestStore = rs.getString("second_best_store"),
                        secondBestPrice = rs.getDouble("second_best_price"),
                        lastUpdated = rs.getLong("last_updated")
                    )
                )
            }
            rs.close()
            stmt.close()

            // 2. Fetch local items
            val localItems = dao.getAllInventoryItemsDirect()
            
            if (localItems.isEmpty() && remoteItems.isNotEmpty()) {
                for (remote in remoteItems) {
                    dao.insertInventoryItem(remote)
                }
            } else {
                val pstmt = conn.prepareStatement("""
                    INSERT INTO inventory_items (id, name, current_stock, min_stock_alert, unit, depletion_rate_per_day,
                        best_store, best_price, second_best_store, second_best_price, last_updated)
                    VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                    name = EXCLUDED.name,
                    current_stock = EXCLUDED.current_stock,
                    min_stock_alert = EXCLUDED.min_stock_alert,
                    unit = EXCLUDED.unit,
                    depletion_rate_per_day = EXCLUDED.depletion_rate_per_day,
                    best_store = EXCLUDED.best_store,
                    best_price = EXCLUDED.best_price,
                    second_best_store = EXCLUDED.second_best_store,
                    second_best_price = EXCLUDED.second_best_price,
                    last_updated = EXCLUDED.last_updated
                """.trimIndent())

                for (local in localItems) {
                    pstmt.setInt(1, local.id)
                    pstmt.setString(2, local.name)
                    pstmt.setDouble(3, local.currentStock)
                    pstmt.setDouble(4, local.minStockAlert)
                    pstmt.setString(5, local.unit)
                    pstmt.setDouble(6, local.depletionRatePerDay)
                    pstmt.setString(7, local.bestStore ?: "")
                    pstmt.setDouble(8, local.bestPrice ?: 0.0)
                    pstmt.setString(9, local.secondBestStore ?: "")
                    pstmt.setDouble(10, local.secondBestPrice ?: 0.0)
                    pstmt.setLong(11, local.lastUpdated)
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
                pstmt.close()
                conn.commit()

                // Insert into local what is remote but not local or newer
                val localMap = localItems.associateBy { it.id }
                for (remote in remoteItems) {
                    val local = localMap[remote.id]
                    if (local == null) {
                        dao.insertInventoryItem(remote)
                    } else if (remote.lastUpdated > local.lastUpdated) {
                        dao.updateInventoryItem(remote)
                    }
                }
            }
            Log.d(TAG, "Synced Inventory with Neon PostgreSQL!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing inventory", e)
            conn?.rollback()
            false
        } finally {
            conn?.close()
        }
    }

    // Bidirectional sync for shopping items
    suspend fun syncShopping(context: Context, dao: ShoppingDao): Boolean = withContext(Dispatchers.IO) {
        var conn: Connection? = null
        try {
            conn = getConnection() ?: return@withContext false
            conn.autoCommit = false
            
            // Delete locally-tracked deleted items from remote
            val deletedIds = DeletionTracker.getDeletedShopping(context)
            if (deletedIds.isNotEmpty()) {
                try {
                    val delStmt = conn.prepareStatement("DELETE FROM shopping_items WHERE id = ?")
                    for (id in deletedIds) {
                        delStmt.setInt(1, id)
                        delStmt.addBatch()
                    }
                    delStmt.executeBatch()
                    delStmt.close()
                    DeletionTracker.clearDeletedShopping(context, deletedIds)
                    conn.commit()
                } catch (e: Exception) {
                    Log.e(TAG, "Error performing remote shopping deletions", e)
                }
            }
            
            // 1. Fetch remote
            val remoteItems = mutableListOf<ShoppingItem>()
            val stmt = conn.createStatement()
            val rs = stmt.executeQuery("SELECT id, product_name, quantity_to_buy, unit, estimated_price, is_bought, target_store FROM shopping_items ORDER BY id ASC")
            while (rs.next()) {
                remoteItems.add(
                    ShoppingItem(
                        id = rs.getInt("id"),
                        productName = rs.getString("product_name"),
                        quantityToBuy = rs.getDouble("quantity_to_buy"),
                        unit = rs.getString("unit"),
                        estimatedPrice = rs.getDouble("estimated_price"),
                        isBought = rs.getBoolean("is_bought"),
                        targetStore = rs.getString("target_store")
                    )
                )
            }
            rs.close()
            stmt.close()

            // 2. Local
            val localItems = dao.getAllShoppingItemsDirect()
            
            if (localItems.isEmpty() && remoteItems.isNotEmpty()) {
                for (remote in remoteItems) {
                    dao.insertShoppingItem(remote)
                }
            } else {
                val pstmt = conn.prepareStatement("""
                    INSERT INTO shopping_items (id, product_name, quantity_to_buy, unit, estimated_price, is_bought, target_store)
                    VALUES (?, ?, ?, ?, ?, ?, ?)
                    ON CONFLICT (id) DO UPDATE SET
                    product_name = EXCLUDED.product_name,
                    quantity_to_buy = EXCLUDED.quantity_to_buy,
                    unit = EXCLUDED.unit,
                    estimated_price = EXCLUDED.estimated_price,
                    is_bought = EXCLUDED.is_bought,
                    target_store = EXCLUDED.target_store
                """.trimIndent())

                for (local in localItems) {
                    pstmt.setInt(1, local.id)
                    pstmt.setString(2, local.productName)
                    pstmt.setDouble(3, local.quantityToBuy)
                    pstmt.setString(4, local.unit)
                    pstmt.setDouble(5, local.estimatedPrice)
                    pstmt.setBoolean(6, local.isBought)
                    pstmt.setString(7, local.targetStore ?: "")
                    pstmt.addBatch()
                }
                pstmt.executeBatch()
                pstmt.close()
                conn.commit()

                // Insert/update local
                val localMap = localItems.associateBy { it.id }
                for (remote in remoteItems) {
                    val local = localMap[remote.id]
                    if (local == null) {
                        dao.insertShoppingItem(remote)
                    } else if (remote.isBought != local.isBought || remote.quantityToBuy != local.quantityToBuy || remote.productName != local.productName) {
                        dao.updateShoppingItem(remote)
                    }
                }
            }
            Log.d(TAG, "Synced Shopping with Neon PostgreSQL!")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Error syncing shopping", e)
            conn?.rollback()
            false
        } finally {
            conn?.close()
        }
    }
}
