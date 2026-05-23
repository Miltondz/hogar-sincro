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

    // Pre-populate Database with realistic household data if empty
    suspend fun prepopulateIfEmpty() {
        val existingExpenses = allExpenses.first()
        if (existingExpenses.isEmpty()) {
            val now = System.currentTimeMillis()
            val oneDayMs = 24 * 60 * 60 * 1000L

            // Seed monthly recurring bills & variables
            expenseDao.insertExpense(Expense(title = "Alquiler Mensual", amount = 850.0, category = "Alquiler", isRecurring = true, recurringDueDate = "Día 01 de cada mes", paidBy = "Milton"))
            expenseDao.insertExpense(Expense(title = "Servicio de Luz", amount = 72.50, category = "Servicio", isRecurring = true, recurringDueDate = "Día 10 de cada mes", paidBy = "Pilar"))
            expenseDao.insertExpense(Expense(title = "Servicio de Agua", amount = 31.80, category = "Servicio", isRecurring = true, recurringDueDate = "Día 12 de cada mes", paidBy = "Milton"))
            expenseDao.insertExpense(Expense(title = "Suscripción Internet Fibra", amount = 45.00, category = "Servicio", isRecurring = true, recurringDueDate = "Día 18 de cada mes", paidBy = "Pilar"))
            
            // Seed variable expenses from past few days to make graphs interesting
            expenseDao.insertExpense(Expense(title = "Artículos de Aseo (Walmart)", amount = 42.15, category = "Alimentos", timestamp = now - oneDayMs, paidBy = "Pilar"))
            expenseDao.insertExpense(Expense(title = "Frutas y Verduras (Mercado)", amount = 28.30, category = "Alimentos", timestamp = now - 2 * oneDayMs, paidBy = "Milton"))
            expenseDao.insertExpense(Expense(title = "Cena de Fin de Semana", amount = 65.00, category = "Diverso", timestamp = now - 3 * oneDayMs, paidBy = "Milton"))
            expenseDao.insertExpense(Expense(title = "Gasolina Automóvil", amount = 55.00, category = "Diverso", timestamp = now - 4 * oneDayMs, paidBy = "Pilar"))
        }

        val existingInventory = allInventoryItems.first()
        if (existingInventory.isEmpty()) {
            // Seed Inventory items with low stock warning or depletion dates
            inventoryDao.insertInventoryItem(InventoryItem(
                name = "Leche Entera",
                currentStock = 2.0,
                minStockAlert = 3.0,
                unit = "litros",
                depletionRatePerDay = 0.5, // 4 days remaining
                bestStore = "Walmart",
                bestPrice = 1.15,
                secondBestStore = "Carrefour",
                secondBestPrice = 1.25
            ))
            inventoryDao.insertInventoryItem(InventoryItem(
                name = "Arroz Grano Largo",
                currentStock = 0.8,
                minStockAlert = 1.0,
                unit = "kg",
                depletionRatePerDay = 0.2, // 4 days remaining (low stock alert!)
                bestStore = "Mercadona",
                bestPrice = 0.95,
                secondBestStore = "Walmart",
                secondBestPrice = 1.05
            ))
            inventoryDao.insertInventoryItem(InventoryItem(
                name = "Café Molido",
                currentStock = 0.15,
                minStockAlert = 0.20,
                unit = "kg",
                depletionRatePerDay = 0.02, // 7 days remaining
                bestStore = "Carrefour",
                bestPrice = 3.40,
                secondBestStore = "Walmart",
                secondBestPrice = 3.65
            ))
            inventoryDao.insertInventoryItem(InventoryItem(
                name = "Detergente Ropa",
                currentStock = 1.0,
                minStockAlert = 1.0,
                unit = "lavados",
                depletionRatePerDay = 0.05, // 20 days remaining (safe)
                bestStore = "Costco",
                bestPrice = 12.00,
                secondBestStore = "Mercadona",
                secondBestPrice = 13.50
            ))
            inventoryDao.insertInventoryItem(InventoryItem(
                name = "Huevos Especiales",
                currentStock = 6.0,
                minStockAlert = 12.0,
                unit = "unidades",
                depletionRatePerDay = 2.0, // 3 days remaining (low stock warning!)
                bestStore = "Mercado Central",
                bestPrice = 2.20,
                secondBestStore = "Walmart",
                secondBestPrice = 2.50
            ))
            inventoryDao.insertInventoryItem(InventoryItem(
                name = "Papel Higiénico",
                currentStock = 18.0,
                minStockAlert = 6.0,
                unit = "rollos",
                depletionRatePerDay = 0.7, // 25 days remaining
                bestStore = "Walmart",
                bestPrice = 5.80,
                secondBestStore = "Costco",
                secondBestPrice = 6.20
            ))
        }

        val existingShoppingItems = allShoppingItems.first()
        if (existingShoppingItems.isEmpty()) {
            shoppingDao.insertShoppingItem(ShoppingItem(productName = "Leche Entera", quantityToBuy = 4.0, unit = "litros", estimatedPrice = 1.15, isBought = false, targetStore = "Walmart"))
            shoppingDao.insertShoppingItem(ShoppingItem(productName = "Huevos Especiales", quantityToBuy = 24.0, unit = "unidades", estimatedPrice = 2.20, isBought = false, targetStore = "Mercado Central"))
            shoppingDao.insertShoppingItem(ShoppingItem(productName = "Arroz Grano Largo", quantityToBuy = 3.0, unit = "kg", estimatedPrice = 0.95, isBought = false, targetStore = "Mercadona"))
            shoppingDao.insertShoppingItem(ShoppingItem(productName = "Papel de Aluminio", quantityToBuy = 1.0, unit = "u", estimatedPrice = 1.80, isBought = true, targetStore = "Walmart"))
        }

        val settings = syncSettings.first()
        if (settings == null) {
            syncSettingsDao.saveSyncSettings(SyncSettings())
        }
    }
}
